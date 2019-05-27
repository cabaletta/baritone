'use strict';
var _ = require('lodash');

/**
 * Base domain object
 *
 * @constructor
 *
 * @param {Twilio} twilio - A Twilio Client
 * @param {string} baseUrl - Base url for this domain
 */
function Domain(twilio, baseUrl) {
  this.twilio = twilio;
  this.baseUrl = baseUrl;
}

/**
 * Turn a uri into an absolute url
 *
 * @param  {string} uri uri to transform
 * @return {string} absolute url
 */
Domain.prototype.absoluteUrl = function(uri) {
  return _.trim(this.baseUrl, '/') + '/' + _.trim(uri, '/');
};

/**
 * Make request to this domain
 *
 * @param {object} opts request options
 * @return {Promise} request promise
 */
Domain.prototype.request = function(opts) {
  return this.twilio.request(_.assign({}, opts, {
    uri: this.absoluteUrl(opts.uri),
  }));
};

module.exports = Domain;
