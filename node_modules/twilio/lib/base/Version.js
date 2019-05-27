'use strict';
var _ = require('lodash');
var RestException = require('./RestException');

/**
 * @constructor
 *
 * @description Base version object
 *
 * @param {Domain} domain twilio domain
 * @param {Version} version api version
 */
function Version(domain, version) {
  this._domain = domain;
  this._version = version;
}

/**
 * Generate absolute url from a uri
 *
 * @param  {string} uri uri to transform
 * @return {string} transformed url
 */
Version.prototype.absoluteUrl = function(uri) {
  return this._domain.absoluteUrl(this.relativeUrl(uri));
};

/**
 * Generate relative url from a uri
 *
 * @param  {string} uri uri to transform
 * @return {string} transformed url
 */
Version.prototype.relativeUrl = function(uri) {
  return _.trim(this._version, '/') + '/' + _.trim(uri, '/');
};

/**
 * Make a request against the domain
 *
 * @param  {object} opts request options
 * @return {Promise} promise that resolves to request response
 */
Version.prototype.request = function(opts) {
  return this._domain.request(_.assign({}, opts, {
    uri: this.relativeUrl(opts.uri),
  }));
};

/**
 * Fetch a instance of a record
 * @throws {Error} If response returns non 2xx status code
 *
 * @param  {object} opts request options
 * @return {Promise} promise that resolves to fetched result
 */
Version.prototype.fetch = function(opts) {
  var qResponse = this.request(opts);

  qResponse = qResponse.then(
    function success(response) {
      if (response.statusCode < 200 || response.statusCode >= 300) {
        throw new RestException(response);
      }

      return JSON.parse(response.body);
    }
  );

  return qResponse;
};

/**
 * Update a record
 * @throws {Error} If response returns non 2xx status code
 *
 * @param  {object} opts request options
 * @return {Promise} promise that resolves to updated result
 */
Version.prototype.update = function(opts) {
  var qResponse = this.request(opts);
  qResponse = qResponse.then(
    function success(response) {
      if (response.statusCode < 200 || response.statusCode >= 300) {
        throw new RestException(response);
      }

      return JSON.parse(response.body);
    }
  );

  return qResponse;
};

/**
 * Delete a record
 * @throws {Error} If response returns a 5xx status
 *
 * @param  {object} opts request options
 * @return {Promise} promise that resolves to true if record was deleted
 */
Version.prototype.remove = function(opts) {
  var qResponse = this.request(opts);
  qResponse = qResponse.then(
    function success(response) {
      if (response.statusCode < 200 || response.statusCode >= 300) {
        throw new RestException(response);
      }

      return response.statusCode === 204;
    }
  );

  return qResponse;
};

/**
 * Create a new record
 * @throws {Error} If response returns non 2xx or 201 status code
 *
 * @param  {object} opts request options
 * @return {Promise} promise that resolves to created record
 */
Version.prototype.create = function(opts) {
  var qResponse = this.request(opts);
  qResponse = qResponse.then(
    function success(response) {
      if (response.statusCode < 200 || response.statusCode >= 300) {
        throw new RestException(response);
      }

      return JSON.parse(response.body);
    }
  );

  return qResponse;
};

/**
 * Fetch a page of records
 *
 * @param  {object} opts request options
 * @return {Promise} promise that resolves to page of records
 */
Version.prototype.page = function(opts) {
  return this.request(opts);
};

/**
 * Process limits for list requests
 *
 * @param {object} [opts] ...
 * @param {number} [opts.limit] The maximum number of items to fetch
 * @param {number} [opts.pageSize] The maximum number of items to return
 *                                  with every request
 */
Version.prototype.readLimits = function(opts) {
  var limit = opts.limit;
  var pageLimit;
  var pageSize = opts.pageSize;
  if (!_.isNil(limit) && (!_.isFinite(limit) || limit <= 0)) { 
    throw new TypeError("Parameter limit must be a positive integer");
  }

  if (!_.isNil(pageSize) && (!_.isFinite(pageSize) || pageSize <= 0)) { 
    throw new TypeError("Parameter pageSize must be a positive integer");
  }

  if (limit) {
    if (!pageSize) {
      pageSize = limit;
    }

    pageLimit = parseInt(Math.ceil(limit / parseFloat(pageSize)), 10);
  }

  return {
    limit: limit,
    pageSize: pageSize,
    pageLimit: pageLimit,
  };
};

module.exports = Version;
