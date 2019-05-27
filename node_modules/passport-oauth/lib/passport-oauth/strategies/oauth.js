/**
 * Module dependencies.
 */
var passport = require('passport')
  , url = require('url')
  , util = require('util')
  , utils = require('./utils')
  , OAuth = require('oauth').OAuth
  , InternalOAuthError = require('../errors/internaloautherror');


/**
 * `OAuthStrategy` constructor.
 *
 * The OAuth authentication strategy authenticates requests using the OAuth
 * protocol.
 *
 * OAuth provides a facility for delegated authentication, whereby users can
 * authenticate using a third-party service such as Twitter.  Delegating in this
 * manner involves a sequence of events, including redirecting the user to the
 * third-party service for authorization.  Once authorization has been obtained,
 * the user is redirected back to the application and a token can be used to
 * obtain credentials.
 *
 * Applications must supply a `verify` callback which accepts a `token`,
 * `tokenSecret` and service-specific `profile`, and then calls the `done`
 * callback supplying a `user`, which should be set to `false` if the
 * credentials are not valid.  If an exception occured, `err` should be set.
 *
 * Options:
 *   - `requestTokenURL`       URL used to obtain an unauthorized request token
 *   - `accessTokenURL`        URL used to exchange a user-authorized request token for an access token
 *   - `userAuthorizationURL`  URL used to obtain user authorization
 *   - `consumerKey`           identifies client to service provider
 *   - `consumerSecret`        secret used to establish ownership of the consumer key
 *   - `callbackURL`           URL to which the service provider will redirect the user after obtaining authorization
 *   - `passReqToCallback`     when `true`, `req` is the first argument to the verify callback (default: `false`)
 *
 * Examples:
 *
 *     passport.use(new OAuthStrategy({
 *         requestTokenURL: 'https://www.example.com/oauth/request_token',
 *         accessTokenURL: 'https://www.example.com/oauth/access_token',
 *         userAuthorizationURL: 'https://www.example.com/oauth/authorize',
 *         consumerKey: '123-456-789',
 *         consumerSecret: 'shhh-its-a-secret'
 *         callbackURL: 'https://www.example.net/auth/example/callback'
 *       },
 *       function(token, tokenSecret, profile, done) {
 *         User.findOrCreate(..., function (err, user) {
 *           done(err, user);
 *         });
 *       }
 *     ));
 *
 * @param {Object} options
 * @param {Function} verify
 * @api public
 */
function OAuthStrategy(options, verify) {
  options = options || {}
  passport.Strategy.call(this);
  this.name = 'oauth';
  this._verify = verify;
  
  if (!options.requestTokenURL) throw new Error('OAuthStrategy requires a requestTokenURL option');
  if (!options.accessTokenURL) throw new Error('OAuthStrategy requires a accessTokenURL option');
  if (!options.userAuthorizationURL) throw new Error('OAuthStrategy requires a userAuthorizationURL option');
  if (!options.consumerKey) throw new Error('OAuthStrategy requires a consumerKey option');
  if (options.consumerSecret === undefined) throw new Error('OAuthStrategy requires a consumerSecret option');
  if (!verify) throw new Error('OAuth authentication strategy requires a verify function');
  
  // NOTE: The _oauth property is considered "protected".  Subclasses are
  //       allowed to use it when making protected resource requests to retrieve
  //       the user profile.
  this._oauth = new OAuth(options.requestTokenURL, options.accessTokenURL,
                          options.consumerKey,  options.consumerSecret,
                          "1.0", null, options.signatureMethod || "HMAC-SHA1",
                          null, options.customHeaders);
  
  this._userAuthorizationURL = options.userAuthorizationURL;
  this._callbackURL = options.callbackURL;
  this._passReqToCallback = options.passReqToCallback;
  this._skipUserProfile = (options.skipUserProfile === undefined) ? false : options.skipUserProfile;
  this._key = options.sessionKey || 'oauth';
}

/**
 * Inherit from `passport.Strategy`.
 */
util.inherits(OAuthStrategy, passport.Strategy);


/**
 * Authenticate request by delegating to a service provider using OAuth.
 *
 * @param {Object} req
 * @api protected
 */
OAuthStrategy.prototype.authenticate = function(req, options) {
  options = options || {};
  if (!req.session) { return this.error(new Error('OAuth authentication requires session support')); }
  
  var self = this;
  
  if (req.query && req.query['oauth_token']) {
    // The request being authenticated contains an oauth_token parameter in the
    // query portion of the URL.  This indicates that the service provider has
    // redirected the user back to the application, after authenticating the
    // user and obtaining their authorization.
    //
    // The value of the oauth_token parameter is the request token.  Together
    // with knowledge of the token secret (stored in the session), the request
    // token can be exchanged for an access token and token secret.
    //
    // This access token and token secret, along with the optional ability to
    // fetch profile information from the service provider, is sufficient to
    // establish the identity of the user.
    
    // Bail if the session does not contain the request token and corresponding
    // secret.  If this happens, it is most likely caused by initiating OAuth
    // from a different host than that of the callback endpoint (for example:
    // initiating from 127.0.0.1 but handling callbacks at localhost).
    if (!req.session[self._key]) { return self.error(new Error('failed to find request token in session')); }
    
    var oauthToken = req.query['oauth_token'];
    var oauthVerifier = req.query['oauth_verifier'] || null;
    var oauthTokenSecret = req.session[self._key]["oauth_token_secret"];
    
    // NOTE: The oauth_verifier parameter will be supplied in the query portion
    //       of the redirect URL, if the server supports OAuth 1.0a.
    
    this._oauth.getOAuthAccessToken(oauthToken, oauthTokenSecret, oauthVerifier, function(err, token, tokenSecret, params) {
      if (err) { return self.error(new InternalOAuthError('failed to obtain access token', err)); }
      
      // The request token has been exchanged for an access token.  Since the
      // request token is a single-use token, that data can be removed from the
      // session.
      delete req.session[self._key]['oauth_token'];
      delete req.session[self._key]['oauth_token_secret'];
      if (Object.keys(req.session[self._key]).length == 0) {
        delete req.session[self._key];
      }
      
      self._loadUserProfile(token, tokenSecret, params, function(err, profile) {
        if (err) { return self.error(err); };
        
        function verified(err, user, info) {
          if (err) { return self.error(err); }
          if (!user) { return self.fail(info); }
          self.success(user, info);
        }
        
        if (self._passReqToCallback) {
          var arity = self._verify.length;
          if (arity == 6) {
            self._verify(req, token, tokenSecret, params, profile, verified);
          } else { // arity == 5
            self._verify(req, token, tokenSecret, profile, verified);
          }
        } else {
          var arity = self._verify.length;
          if (arity == 5) {
            self._verify(token, tokenSecret, params, profile, verified);
          } else { // arity == 4
            self._verify(token, tokenSecret, profile, verified);
          }
        }
      });
    });
  } else {
    // In order to authenticate via OAuth, the application must obtain a request
    // token from the service provider and redirect the user to the service
    // provider to obtain their authorization.  After authorization has been
    // approved the user will be redirected back the application, at which point
    // the application can exchange the request token for an access token.
    //
    // In order to successfully exchange the request token, its corresponding
    // token secret needs to be known.  The token secret will be temporarily
    // stored in the session, so that it can be retrieved upon the user being
    // redirected back to the application.
    
    var params = this.requestTokenParams(options);
    var callbackURL = options.callbackURL || this._callbackURL;
    if (callbackURL) {
      var parsed = url.parse(callbackURL);
      if (!parsed.protocol) {
        // The callback URL is relative, resolve a fully qualified URL from the
        // URL of the originating request.
        callbackURL = url.resolve(utils.originalURL(req), callbackURL);
      }
    }
    params['oauth_callback'] = callbackURL;
    
    this._oauth.getOAuthRequestToken(params, function(err, token, tokenSecret, params) {
      if (err) { return self.error(new InternalOAuthError('failed to obtain request token', err)); }
      
      // NOTE: params will contain an oauth_callback_confirmed property set to
      //       true, if the server supports OAuth 1.0a.
      //       { oauth_callback_confirmed: 'true' }

      if (!req.session[self._key]) { req.session[self._key] = {}; }
      req.session[self._key]['oauth_token'] = token;
      req.session[self._key]['oauth_token_secret'] = tokenSecret;

      var parsed = url.parse(self._userAuthorizationURL, true);
      parsed.query['oauth_token'] = token;
      utils.merge(parsed.query, self.userAuthorizationParams(options))
      delete parsed.search;
      var location = url.format(parsed);
      self.redirect(location);
    });
  }
}

/**
 * Retrieve user profile from service provider.
 *
 * OAuth-based authentication strategies can overrride this function in order to
 * load the user's profile from the service provider.  This assists applications
 * (and users of those applications) in the initial registration process by
 * automatically submitting required information.
 *
 * @param {String} accessToken
 * @param {Function} done
 * @api protected
 */
OAuthStrategy.prototype.userProfile = function(token, tokenSecret, params, done) {
  return done(null, {});
}

/**
 * Return extra parameters to be included in the request token request.
 *
 * Some OAuth providers require additional parameters to be included when
 * issuing a request token.  Since these parameters are not standardized by the
 * OAuth specification, OAuth-based authentication strategies can overrride this
 * function in order to populate these parameters as required by the provider.
 *
 * @param {Object} options
 * @return {Object}
 * @api protected
 */
OAuthStrategy.prototype.requestTokenParams = function(options) {
  return {};
}

/**
 * Return extra parameters to be included in the user authorization request.
 *
 * Some OAuth providers allow additional, non-standard parameters to be included
 * when requesting authorization.  Since these parameters are not standardized
 * by the OAuth specification, OAuth-based authentication strategies can
 * overrride this function in order to populate these parameters as required by
 * the provider.
 *
 * @param {Object} options
 * @return {Object}
 * @api protected
 */
OAuthStrategy.prototype.userAuthorizationParams = function(options) {
  return {};
}

/**
 * Load user profile, contingent upon options.
 *
 * @param {String} accessToken
 * @param {Function} done
 * @api private
 */
OAuthStrategy.prototype._loadUserProfile = function(token, tokenSecret, params, done) {
  var self = this;
  
  function loadIt() {
    return self.userProfile(token, tokenSecret, params, done);
  }
  function skipIt() {
    return done(null);
  }
  
  if (typeof this._skipUserProfile == 'function' && this._skipUserProfile.length > 1) {
    // async
    this._skipUserProfile(token, tokenSecret, function(err, skip) {
      if (err) { return done(err); }
      if (!skip) { return loadIt(); }
      return skipIt();
    });
  } else {
    var skip = (typeof this._skipUserProfile == 'function') ? this._skipUserProfile() : this._skipUserProfile;
    if (!skip) { return loadIt(); }
    return skipIt();
  }
}


/**
 * Expose `OAuthStrategy`.
 */ 
module.exports = OAuthStrategy;
