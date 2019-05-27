'use strict';

var _ = require('lodash');
var moment = require('moment');

/**
 * @namespace deserialize
 */

function parseDate(s, format) {
  var m = moment.utc(s, format);
  if (m.isValid()) {
    return m.toDate();
  }

  return s;
}

function parseNumber(n, parser) {
  var parsed = parser(n);
  if (isNaN(parsed)) {
    return n;
  }

  return parsed;
}

/**
 * @function iso8601Date
 * @memberOf deserialize
 * @description parse a string into a Date object
 *
 * @param  {string} s date string in YYYY-MM-DD format
 * @return {Date} Date object
 */
function iso8601Date(s) {
  return parseDate(s, 'YYYY-MM-DD');
}

/**
 * @function iso8601DateTime
 * @memberOf deserialize
 * @description parse a string into a Date object
 *
 * @param  {String} s date string in YYYY-MM-DD[T]HH:mm:ss[Z] format
 * @return {Date} Date object
 */
function iso8601DateTime(s) {
  return parseDate(s, 'YYYY-MM-DD[T]HH:mm:ss[Z]');
}

/**
 * @function rfc2822DateTime
 * @memberOf deserialize
 * @description parse a string into a Date object
 *
 * @param  {String} s date string in ddd, DD MMM YYYY HH:mm:ss [+0000] format
 * @return {Date} Date object
 */
function rfc2822DateTime(s) {
  return parseDate(s, 'ddd, DD MMM YYYY HH:mm:ss [+0000]');
}

/**
 * @function decimal
 * @memberOf deserialize
 * @description parse a string into a decimal
 *
 * @param  {string} d decimal value as string
 * @return {number} number object
 */
function decimal(d) {
  return parseNumber(d, parseFloat);
}

/**
 * @function integer
 * @memberOf deserialize
 * @description parse a string into a integer
 *
 * @param  {string} i integer value as string
 * @return {number} number object
 */
function integer(i) {
  return parseNumber(i, _.parseInt);
}

module.exports = {
  iso8601Date: iso8601Date,
  iso8601DateTime: iso8601DateTime,
  rfc2822DateTime: rfc2822DateTime,
  decimal: decimal,
  integer: integer,
};
