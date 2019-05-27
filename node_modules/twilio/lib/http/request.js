'use strict';

var _ = require('lodash');

var Request = function(opts) {
  opts = opts || {};

  this.method = opts.method || this.ANY;
  this.url = opts.url || this.ANY;
  this.auth = opts.auth || this.ANY;
  this.params = opts.params || this.ANY;
  this.data = opts.data || this.ANY;
  this.headers = opts.headers || this.ANY;
};

Request.prototype.ANY = '*';

Request.prototype.attributeEqual = function(lhs, rhs) {
  if (lhs === this.ANY || rhs === this.ANY) {
    return true;
  }

  lhs = lhs || undefined;
  rhs = rhs || undefined;

  return _.isEqual(lhs, rhs);
};

Request.prototype.isEqual = function(other) {
  return (this.attributeEqual(this.method, other.method) &&
      this.attributeEqual(this.url, other.url) &&
      this.attributeEqual(this.auth, other.auth) &&
      this.attributeEqual(this.params, other.params) &&
      this.attributeEqual(this.data, other.data) &&
      this.attributeEqual(this.headers, other.headers));
};

Request.prototype.toString = function() {
  var auth = '';
  if (this.auth && this.auth !== this.ANY) {
    auth = this.auth + ' ';
  }

  var params = '';
  if (this.params && this.params !== this.ANY) {
    params = '?' + _.join(_.chain(_.keys(this.params))
        .map(function(key) { return key + '=' + this.params[key]; }.bind(this))
        .value(), '&');
  }

  var data = '';
  if (this.data && this.data !== this.ANY) {
    if (this.method === 'GET') {
      data = '\n -G';
    }

    data = data + '\n' + _.join(
      _.map(this.data, function(value, key) {
        return ' -d ' + key + '='  + value;
      }), '\n');
  }

  var headers = '';
  if (this.headers && this.headers !== this.ANY) {
    headers = '\n' + _.join(
      _.map(this.headers, function(value, key) {
        return ' -H ' + key + '='  + value;
      }), '\n');
  }

  return auth + this.method + ' ' + this.url + params + data + headers;
};

module.exports = Request;
