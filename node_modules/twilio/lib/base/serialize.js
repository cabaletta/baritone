'use strict';

var _ = require('lodash');
var moment = require('moment');

module.exports = {};

/**
 * @namespace serialize
 */

/**
 * @function iso8601Date
 * @memberOf serialize
 * @description turns a Date object into a string if parameter is a Date
 * otherwise returns the parameter
 *
 * @param  {Date} d date object to format
 * @return {string|object} date formatted in YYYY-MM-DD form
 */
module.exports.iso8601Date = function(d) {
  if (_.isUndefined(d) || _.isString(d) || !(_.isDate(d))) {
    return d;
  } else {
    return moment.utc(d).format('YYYY-MM-DD');
  }
};

/**
 * @function iso8601DateTime
 * @memberOf serialize
 * @description turns a Date object into a string if parameter is a Date
 * otherwise returns the parameter
 *
 * @param  {Date} d date object to format
 * @return {string|object} date formatted in YYYY-MM-DD[T]HH:mm:ss[Z] form
 */
module.exports.iso8601DateTime = function(d) {
  if (_.isUndefined(d) || _.isString(d) || !(d instanceof Date)) {
    return d;
  } else {
    return moment.utc(d).format('YYYY-MM-DD[T]HH:mm:ss[Z]');
  }
};

/**
 * @function prefixedCollapsibleMap
 * @memberOf serialize
 * @description turns a map of params int oa flattened map separated by dots
 * if the parameter is an object, otherwise returns an empty map
 *
 * @param {object} m map to transform
 * @param {string|undefined} prefix to append to each flattened value
 * @return {object} flattened map
 */
module.exports.prefixedCollapsibleMap = function(m, prefix) {
  if (_.isUndefined(m) || !_.isPlainObject(m)) {
    return {};
  }

  function flatten(m, result, previous) {
    result = result || {};
    previous = previous || [];

    _.each(_.keys(m), function(key) {
      if (_.isPlainObject(m[key])) {
        flatten(m[key], result, _.union(previous, [key]));
      } else {
        result[_.join(_.union(previous, [key]), '.')] = m[key];
      }
    });

    return result;
  }

  var flattened = flatten(m);
  var result = flattened;
  if (prefix) {
    result = {};
    _.each(_.keys(flattened), function(key) {
      result[prefix + '.' + key] = flattened[key];
    });
  }

  return result;
};

/**
 * @function object
 * @memberOf serialize
 * @description turns an object into a JSON string if the parameter
 * is an object, otherwise returns the passed in object
 *
 * @param {object|array} o json object or array
 * @returns {string|object} stringified object
 */
module.exports.object = function(o) {
  if (_.isObject(o) || _.isArray(o)) {
    return JSON.stringify(o);
  }

  return o;
};

/**
 * @function bool
 * @memberOf serialize
 * @description coerces a boolean literal into a string
 *
 * @param {boolean|string} input boolean or string to be coerced
 * @returns {string} a string "true" or "false"
 */
module.exports.bool = function(input) {
  if (_.isString(input)) {
    return input;
  }
  if(_.isBoolean(input)) {
    return input.toString();
  }

  return input;
};


/**
 * @function map
 * @memberOf serialize
 * @description maps transform over each element in input if input is an array
 *
 * @param {array} input array to map transform over, if not an array then it is
 * returned as is.
 * @returns {array} new array with transform applied to each element.
 */
module.exports.map = function(input, transform) {
    if (_.isArray(input)) {
        return _.map(input, transform);
    }
    return input;
}
