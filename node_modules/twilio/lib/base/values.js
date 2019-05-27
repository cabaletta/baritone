'use strict';
var _ = require('lodash');

/**
 * @namespace values
 */

/**
 * @function of
 * @memberOf values
 * @description removes all undefined values of an object
 *
 * @param  {object} obj object to filter
 * @return {object} object with no undefined values
 */
function of(obj) {
  return _.omitBy(obj, _.isUndefined);
}

module.exports = {
  of: of,
};
