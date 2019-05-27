'use strict';

var _ = require('lodash');
var http = require('request');
var Q = require('q');
var Response = require('../http/response');
var Request = require('../http/request');

var RequestClient = function() {};

/**
 * Make http request
 * @param {object} opts - The options argument
 * @param {string} opts.method - The http method
 * @param {string} opts.uri - The request uri
 * @param {string} [opts.username] - The username used for auth
 * @param {string} [opts.password] - The password used for auth
 * @param {object} [opts.headers] - The request headers
 * @param {object} [opts.params] - The request params
 * @param {object} [opts.data] - The request data
 * @param {int} [opts.timeout=30000] - The request timeout in milliseconds
 * @param {boolean} [opts.allowRedirects] - Should the client follow redirects
 * @param {boolean} [opts.forever] - Set to true to use the forever-agent
 */
RequestClient.prototype.request = function(opts) {
  opts = opts || {};
  if (!opts.method) {
    throw new Error('http method is required');
  }

  if (!opts.uri) {
    throw new Error('uri is required');
  }

  var deferred = Q.defer();
  var headers = opts.headers || {};
  if (opts.username && opts.password) {
    var b64Auth = new Buffer(opts.username + ':' + opts.password).toString('base64');
    headers.Authorization = 'Basic ' + b64Auth;
  }

  var options = {
    timeout: opts.timeout || 30000,
    followRedirect: opts.allowRedirects || false,
    url: opts.uri,
    method: opts.method,
    headers: opts.headers,
    forever: opts.forever === false ? false : true,
  };

  if (!_.isNull(opts.data)) {
    options.formData = opts.data;
  }

  if (!_.isNull(opts.params)) {
    options.qs = opts.params;
    options.useQuerystring = true;
  }

  var optionsRequest = {
    method: options.method,
    url: options.url,
    auth: b64Auth || null,
    params: options.qs,
    data: options.formData,
    headers: options.headers,
  };

  var that = this;
  this.lastResponse = undefined;
  this.lastRequest = new Request(optionsRequest);

  http(options, function(error, response) {
    if (error) {
      that.lastResponse = undefined;
      deferred.reject(error);
    } else {
      that.lastResponse = new Response(response.statusCode, response.body);
      deferred.resolve({
        statusCode: response.statusCode,
        body: response.body,
      });
    }
  });

  return deferred.promise;
};

module.exports = RequestClient;
