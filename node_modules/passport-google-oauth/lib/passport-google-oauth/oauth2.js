/**
 * Module dependencies.
 */
var util = require('util')
  , OAuth2Strategy = require('passport-oauth').OAuth2Strategy
  , InternalOAuthError = require('passport-oauth').InternalOAuthError;


/**
 * `Strategy` constructor.
 *
 * The Google authentication strategy authenticates requests by delegating to
 * Google using the OAuth 2.0 protocol.
 *
 * Applications must supply a `verify` callback which accepts an `accessToken`,
 * `refreshToken` and service-specific `profile`, and then calls the `done`
 * callback supplying a `user`, which should be set to `false` if the
 * credentials are not valid.  If an exception occured, `err` should be set.
 *
 * Options:
 *   - `clientID`      your Google application's client id
 *   - `clientSecret`  your Google application's client secret
 *   - `callbackURL`   URL to which Google will redirect the user after granting authorization
 *
 * Examples:
 *
 *     passport.use(new GoogleStrategy({
 *         clientID: '123-456-789',
 *         clientSecret: 'shhh-its-a-secret'
 *         callbackURL: 'https://www.example.net/auth/google/callback'
 *       },
 *       function(accessToken, refreshToken, profile, done) {
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
function Strategy(options, verify) {
  options = options || {};
  options.authorizationURL = options.authorizationURL || 'https://accounts.google.com/o/oauth2/auth';
  options.tokenURL = options.tokenURL || 'https://accounts.google.com/o/oauth2/token';
  
  OAuth2Strategy.call(this, options, verify);
  this.name = 'google';
}

/**
 * Inherit from `OAuth2Strategy`.
 */
util.inherits(Strategy, OAuth2Strategy);


/**
 * Retrieve user profile from Google.
 *
 * This function constructs a normalized profile, with the following properties:
 *
 *   - `provider`         always set to `google`
 *   - `id`
 *   - `username`
 *   - `displayName`
 *
 * @param {String} accessToken
 * @param {Function} done
 * @api protected
 */
Strategy.prototype.userProfile = function(accessToken, done) {
  this._oauth2.get('https://www.googleapis.com/oauth2/v1/userinfo', accessToken, function (err, body, res) {
    if (err) { return done(new InternalOAuthError('failed to fetch user profile', err)); }
    
    try {
      var json = JSON.parse(body);
      
      var profile = { provider: 'google' };
      profile.id = json.id;
      profile.displayName = json.name;
      profile.name = { familyName: json.family_name,
                       givenName: json.given_name };
      profile.emails = [{ value: json.email }];
      
      profile._raw = body;
      profile._json = json;
      
      done(null, profile);
    } catch(e) {
      done(e);
    }
  });
}

/**
 * Return extra Google-specific parameters to be included in the authorization
 * request.
 *
 * @param {Object} options
 * @return {Object}
 * @api protected
 */
Strategy.prototype.authorizationParams = function(options) {
  var params = {};
  if (options.accessType) {
    params['access_type'] = options.accessType;
  }
  if (options.approvalPrompt) {
    params['approval_prompt'] = options.approvalPrompt;
  }
  if (options.prompt) {
    // This parameter is undocumented in Google's official documentation.
    // However, it was detailed by Breno de Medeiros (who works at Google) in
    // this Stack Overflow answer:
    //  http://stackoverflow.com/questions/14384354/force-google-account-chooser/14393492#14393492
    params['prompt'] = options.prompt;
  }
  if (options.loginHint) {
    // This parameter is derived from OpenID Connect, and supported by Google's
    // OAuth 2.0 endpoint.
    //   https://github.com/jaredhanson/passport-google-oauth/pull/8
    //   https://bitbucket.org/openid/connect/commits/970a95b83add
    params['login_hint'] = options.loginHint;
  }
  if (options.userID) {
    // Undocumented, but supported by Google's OAuth 2.0 endpoint.  Appears to
    // be equivalent to `login_hint`.
    params['user_id'] = options.userID;
  }
  if (options.hostedDomain || options.hd) {
    // This parameter is derived from Google's OAuth 1.0 endpoint, and (although
    // undocumented) is supported by Google's OAuth 2.0 endpoint was well.
    //   https://developers.google.com/accounts/docs/OAuth_ref
    params['hd'] = options.hostedDomain || options.hd;
  }
  return params;
}


/**
 * Expose `Strategy`.
 */
module.exports = Strategy;
