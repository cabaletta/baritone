/**
 * Module dependencies.
 */
var util = require('util')
  , OAuthStrategy = require('passport-oauth').OAuthStrategy
  , InternalOAuthError = require('passport-oauth').InternalOAuthError;


/**
 * `Strategy` constructor.
 *
 * The Google authentication strategy authenticates requests by delegating to
 * Google using the OAuth protocol.
 *
 * Applications must supply a `verify` callback which accepts a `token`,
 * `tokenSecret` and service-specific `profile`, and then calls the `done`
 * callback supplying a `user`, which should be set to `false` if the
 * credentials are not valid.  If an exception occured, `err` should be set.
 *
 * Options:
 *   - `consumerKey`     identifies client to Google
 *   - `consumerSecret`  secret used to establish ownership of the consumer key
 *   - `callbackURL`     URL to which Google will redirect the user after obtaining authorization
 *
 * Examples:
 *
 *     passport.use(new GoogleStrategy({
 *         consumerKey: '123-456-789',
 *         consumerSecret: 'shhh-its-a-secret'
 *         callbackURL: 'https://www.example.net/auth/google/callback'
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
function Strategy(options, verify) {
  options = options || {};
  options.requestTokenURL = options.requestTokenURL || 'https://www.google.com/accounts/OAuthGetRequestToken';
  options.accessTokenURL = options.accessTokenURL || 'https://www.google.com/accounts/OAuthGetAccessToken';
  options.userAuthorizationURL = options.userAuthorizationURL || 'https://www.google.com/accounts/OAuthAuthorizeToken';
  options.sessionKey = options.sessionKey || 'oauth:google';

  OAuthStrategy.call(this, options, verify);
  this.name = 'google';
}

/**
 * Inherit from `OAuthStrategy`.
 */
util.inherits(Strategy, OAuthStrategy);

/**
 * Retrieve user profile from Google.
 *
 * This function constructs a normalized profile, with the following properties:
 *
 *   - `id`
 *   - `displayName`
 *
 * @param {String} token
 * @param {String} tokenSecret
 * @param {Object} params
 * @param {Function} done
 * @api protected
 */
Strategy.prototype.userProfile = function(token, tokenSecret, params, done) {
  this._oauth.get('https://www.google.com/m8/feeds/contacts/default/full?max-results=1&alt=json', token, tokenSecret, function (err, body, res) {
    if (err) { return done(new InternalOAuthError('failed to fetch user profile', err)); }
    
    try {
      var json = JSON.parse(body);
      
      var profile = { provider: 'google' };
      profile.id = json.feed.id['$t']
      profile.displayName = json.feed.author[0].name['$t'];
      profile.emails = [{ value: json.feed.author[0].email['$t'] }];
      
      profile._raw = body;
      profile._json = json;
      
      done(null, profile);
    } catch(e) {
      done(e);
    }
  });
}

/**
 * Return extra Google-specific parameters to be included in the request token
 * request.
 *
 * @param {Object} options
 * @return {Object}
 * @api protected
 */
Strategy.prototype.requestTokenParams = function(options) {
  var params = options || {};
  
  var scope = options.scope;
  if (scope) {
    if (Array.isArray(scope)) { scope = scope.join(' '); }
    params['scope'] = scope;
  }
  return params;
}


/**
 * Expose `Strategy`.
 */
module.exports = Strategy;
