'use strict';

var crypto = require('crypto');
var _ = require('lodash');
var scmp = require('scmp');
var url = require('url');

/**
 Utility function to validate an incoming request is indeed from Twilio

 @param {string} authToken - The auth token, as seen in the Twilio portal
 @param {string} twilioHeader - The value of the X-Twilio-Signature header from the request
 @param {string} url - The full URL (with query string) you configured to handle this request
 @param {object} params - the parameters sent with this request
 */
function validateRequest(authToken, twilioHeader, url, params) {
  Object.keys(params).sort().forEach(function(key) {
      url = url + key + params[key];
  });

  var signature = crypto
    .createHmac('sha1', authToken)
    .update(Buffer.from(url, 'utf-8'))
    .digest('base64');

  return scmp(twilioHeader, signature);
}

/**
 Utility function to validate an incoming request is indeed from Twilio. This also validates
 the request body against the bodySHA256 post parameter.

 @param {string} authToken - The auth token, as seen in the Twilio portal
 @param {string} twilioHeader - The value of the X-Twilio-Signature header from the request
 @param {string} requestUrl - The full URL (with query string) you configured to handle this request
 @param {string} body - The body of the request
 */
function validateRequestWithBody(authToken, twilioHeader, requestUrl, body) {
  var urlObject = new url.URL(requestUrl);
  return validateRequest(authToken, twilioHeader, requestUrl, {}) && validateBody(body, urlObject.searchParams.get('bodySHA256'));
}

function validateBody(body, expectedValue) {
  var hash = crypto
    .createHash('sha256')
    .update(Buffer.from(body, 'utf-8'))
    .digest('base64');

  return scmp(expectedValue, hash);
}

/**
 Utility function to validate an incoming request is indeed from Twilio (for use with express).
 adapted from https://github.com/crabasa/twiliosig

 @param {object} request - An expressjs request object (http://expressjs.com/api.html#req.params)
 @param {string} authToken - The auth token, as seen in the Twilio portal
 @param {object} opts - options for request validation:
    - url: The full URL (with query string) you used to configure the webhook with Twilio - overrides host/protocol options
    - host: manually specify the host name used by Twilio in a number's webhook config
    - protocol: manually specify the protocol used by Twilio in a number's webhook config
 */
function validateExpressRequest(request, authToken, opts) {
  var options = opts || {};
  var webhookUrl;

  if (options.url) {
    // Let the user specify the full URL
    webhookUrl = options.url;
  } else {
    // Use configured host/protocol, or infer based on request
    var protocol = options.protocol || request.protocol;
    var host = options.host || request.headers.host;

    webhookUrl = url.format({
        protocol: protocol,
        host: host,
        pathname: request.originalUrl
    });
    if (request.originalUrl.search(/\?/) >= 0) {
      webhookUrl = webhookUrl.replace("%3F","?");
    }

  }

  if (webhookUrl.indexOf('bodySHA256') > 0) {
    return validateRequestWithBody(
      authToken,
      request.header('X-Twilio-Signature'),
      webhookUrl,
      request.body || {}
    );
  } else {
    return validateRequest(
      authToken,
      request.header('X-Twilio-Signature'),
      webhookUrl,
      request.body || {}
    );
  }
}

/**
Express middleware to accompany a Twilio webhook. Provides Twilio
request validation, and makes the response a little more friendly for our
TwiML generator.  Request validation requires the express.urlencoded middleware
to have been applied (e.g. app.use(express.urlencoded()); in your app config).

Options:
- validate: {Boolean} whether or not the middleware should validate the request
    came from Twilio.  Default true. If the request does not originate from
    Twilio, we will return a text body and a 403.  If there is no configured
    auth token and validate=true, this is an error condition, so we will return
    a 500.
- host: manually specify the host name used by Twilio in a number's webhook config
- protocol: manually specify the protocol used by Twilio in a number's webhook config

Returns a middleware function.

Examples:
var webhookMiddleware = twilio.webhook();
var webhookMiddleware = twilio.webhook('asdha9dhjasd'); //init with auth token
var webhookMiddleware = twilio.webhook({
    validate:false // don't attempt request validation
});
var webhookMiddleware = twilio.webhook({
    host: 'hook.twilio.com',
    protocol: 'https'
});
 */
function webhook() {
  var opts = {
    validate: true,
  };

  // Process arguments
  var tokenString;
  for (var i = 0, l = arguments.length; i < l; i++) {
    var arg = arguments[i];
    if (typeof arg === 'string') {
      tokenString = arg;
    } else {
      opts = _.extend(opts, arg);
    }
  }

  // set auth token from input or environment variable
  opts.authToken = tokenString ? tokenString : process.env.TWILIO_AUTH_TOKEN;

  // Create middleware function
  return function hook(request, response, next) {
    // Do validation if requested
    if (opts.validate) {
      // Check for a valid auth token
      if (!opts.authToken) {
        console.error('[Twilio]: Error - Twilio auth token is required for webhook request validation.');
        response.type('text/plain')
          .status(500)
          .send('Webhook Error - we attempted to validate this request without first configuring our auth token.');
      } else {
        // Check that the request originated from Twilio
        var valid = validateExpressRequest(request, opts.authToken, {
          url: opts.url,
          host: opts.host,
          protocol: opts.protocol
        });

        if (valid) {
            next();
        } else {
          return response
            .type('text/plain')
            .status(403)
            .send('Twilio Request Validation Failed.');
        }
      }
    } else {
      next();
    }
  };
}

module.exports = {
  validateRequest,
  validateRequestWithBody,
  validateExpressRequest,
  validateBody,
  webhook,
};
