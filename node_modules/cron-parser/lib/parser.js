'use strict';

var CronExpression = require('./expression');

function CronParser() {}

/**
 * Parse crontab entry
 *
 * @private
 * @param {String} entry Crontab file entry/line
 */
CronParser._parseEntry = function _parseEntry (entry) {
  var atoms = entry.split(' ');

  if (atoms.length === 6) {
    return {
      interval: CronExpression.parse(entry)
    };
  } else if (atoms.length > 6) {
    return {
      interval: CronExpression.parse(entry),
      command: atoms.slice(6, atoms.length)
    };
  } else {
    throw new Error('Invalid entry: ' + entry);
  }
};

/**
 * Wrapper for CronExpression.parser method
 *
 * @public
 * @param {String} expression Input expression
 * @param {Object} [options] Parsing options
 * @return {Object}
 */
CronParser.parseExpression = function parseExpression (expression, options, callback) {
  return CronExpression.parse(expression, options, callback);
};

/**
 * Parse content string
 *
 * @public
 * @param {String} data Crontab content
 * @return {Object}
 */
CronParser.parseString = function parseString (data) {
  var self = this;
  var blocks = data.split('\n');

  var response = {
    variables: {},
    expressions: [],
    errors: {}
  };

  for (var i = 0, c = blocks.length; i < c; i++) {
    var block = blocks[i];
    var matches = null;
    var entry = block.replace(/^\s+|\s+$/g, ''); // Remove surrounding spaces

    if (entry.length > 0) {
      if (entry.match(/^#/)) { // Comment
        continue;
      } else if ((matches = entry.match(/^(.*)=(.*)$/))) { // Variable
        response.variables[matches[1]] = matches[2];
      } else { // Expression?
        var result = null;

        try {
          result = self._parseEntry('0 ' + entry);
          response.expressions.push(result.interval);
        } catch (err) {
          response.errors[entry] = err;
        }
      }
    }
  }

  return response;
};

/**
 * Parse crontab file
 *
 * @public
 * @param {String} filePath Path to file
 * @param {Function} callback
 */
CronParser.parseFile = function parseFile (filePath, callback) {
  require('fs').readFile(filePath, function(err, data) {
    if (err) {
      callback(err);
      return;
    }

    return callback(null, CronParser.parseString(data.toString()));
  });
};

module.exports = CronParser;
