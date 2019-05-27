'use strict';
var util = require('util');

function RestException(response) {
  Error.call('[HTTP ' + response.statusCode + '] Failed to execute request');

  var body = JSON.parse(response.body);
  this.status = response.statusCode;
  this.message = body.message;
  this.code = body.code;
  this.moreInfo = body.more_info; /* jshint ignore:line */
  this.detail = body.detail;
}

util.inherits(RestException, Error);

module.exports = RestException;
