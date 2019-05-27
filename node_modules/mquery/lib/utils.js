'use strict';

/*!
 * Module dependencies.
 */

var Buffer = require('safe-buffer').Buffer;
var RegExpClone = require('regexp-clone');

/**
 * Clones objects
 *
 * @param {Object} obj the object to clone
 * @param {Object} options
 * @return {Object} the cloned object
 * @api private
 */

var clone = exports.clone = function clone(obj, options) {
  if (obj === undefined || obj === null)
    return obj;

  if (Array.isArray(obj))
    return exports.cloneArray(obj, options);

  if (obj.constructor) {
    if (/ObjectI[dD]$/.test(obj.constructor.name)) {
      return 'function' == typeof obj.clone
        ? obj.clone()
        : new obj.constructor(obj.id);
    }

    if (obj.constructor.name === 'ReadPreference') {
      return new obj.constructor(obj.mode, clone(obj.tags, options));
    }

    if ('Binary' == obj._bsontype && obj.buffer && obj.value) {
      return 'function' == typeof obj.clone
        ? obj.clone()
        : new obj.constructor(obj.value(true), obj.sub_type);
    }

    if ('Date' === obj.constructor.name || 'Function' === obj.constructor.name)
      return new obj.constructor(+obj);

    if ('RegExp' === obj.constructor.name)
      return RegExpClone(obj);

    if ('Buffer' === obj.constructor.name)
      return exports.cloneBuffer(obj);
  }

  if (isObject(obj))
    return exports.cloneObject(obj, options);

  if (obj.valueOf)
    return obj.valueOf();
};

/*!
 * ignore
 */

exports.cloneObject = function cloneObject(obj, options) {
  var minimize = options && options.minimize;
  var ret = {};
  var hasKeys;
  var val;
  var k;

  for (k in obj) {
    val = clone(obj[k], options);

    if (!minimize || ('undefined' !== typeof val)) {
      hasKeys || (hasKeys = true);
      ret[k] = val;
    }
  }

  return minimize
    ? hasKeys && ret
    : ret;
};

exports.cloneArray = function cloneArray(arr, options) {
  var ret = [];
  for (var i = 0, l = arr.length; i < l; i++)
    ret.push(clone(arr[i], options));
  return ret;
};

/**
 * process.nextTick helper.
 *
 * Wraps the given `callback` in a try/catch. If an error is
 * caught it will be thrown on nextTick.
 *
 * node-mongodb-native had a habit of state corruption when
 * an error was immediately thrown from within a collection
 * method (find, update, etc) callback.
 *
 * @param {Function} [callback]
 * @api private
 */

exports.tick = function tick(callback) {
  if ('function' !== typeof callback) return;
  return function() {
    // callbacks should always be fired on the next
    // turn of the event loop. A side benefit is
    // errors thrown from executing the callback
    // will not cause drivers state to be corrupted
    // which has historically been a problem.
    var args = arguments;
    soon(function() {
      callback.apply(this, args);
    });
  };
};

/**
 * Merges `from` into `to` without overwriting existing properties.
 *
 * @param {Object} to
 * @param {Object} from
 * @api private
 */

exports.merge = function merge(to, from) {
  var keys = Object.keys(from),
      i = keys.length,
      key;

  while (i--) {
    key = keys[i];
    if ('undefined' === typeof to[key]) {
      to[key] = from[key];
    } else {
      if (exports.isObject(from[key])) {
        merge(to[key], from[key]);
      } else {
        to[key] = from[key];
      }
    }
  }
};

/**
 * Same as merge but clones the assigned values.
 *
 * @param {Object} to
 * @param {Object} from
 * @api private
 */

exports.mergeClone = function mergeClone(to, from) {
  var keys = Object.keys(from),
      i = keys.length,
      key;

  while (i--) {
    key = keys[i];
    if ('undefined' === typeof to[key]) {
      to[key] = clone(from[key]);
    } else {
      if (exports.isObject(from[key])) {
        mergeClone(to[key], from[key]);
      } else {
        to[key] = clone(from[key]);
      }
    }
  }
};

/**
 * Read pref helper (mongo 2.2 drivers support this)
 *
 * Allows using aliases instead of full preference names:
 *
 *     p   primary
 *     pp  primaryPreferred
 *     s   secondary
 *     sp  secondaryPreferred
 *     n   nearest
 *
 * @param {String} pref
 */

exports.readPref = function readPref(pref) {
  switch (pref) {
    case 'p':
      pref = 'primary';
      break;
    case 'pp':
      pref = 'primaryPreferred';
      break;
    case 's':
      pref = 'secondary';
      break;
    case 'sp':
      pref = 'secondaryPreferred';
      break;
    case 'n':
      pref = 'nearest';
      break;
  }

  return pref;
};


/**
 * Read Concern helper (mongo 3.2 drivers support this)
 *
 * Allows using string to specify read concern level:
 *
 *     local          3.2+
 *     available      3.6+
 *     majority       3.2+
 *     linearizable   3.4+
 *     snapshot       4.0+
 *
 * @param {String|Object} concern
 */

exports.readConcern = function readConcern(concern) {
  if ('string' === typeof concern) {
    switch (concern) {
      case 'l':
        concern = 'local';
        break;
      case 'a':
        concern = 'available';
        break;
      case 'm':
        concern = 'majority';
        break;
      case 'lz':
        concern = 'linearizable';
        break;
      case 's':
        concern = 'snapshot';
        break;
    }
    concern = { level: concern };
  }
  return concern;
};

/**
 * Object.prototype.toString.call helper
 */

var _toString = Object.prototype.toString;
exports.toString = function(arg) {
  return _toString.call(arg);
};

/**
 * Determines if `arg` is an object.
 *
 * @param {Object|Array|String|Function|RegExp|any} arg
 * @return {Boolean}
 */

var isObject = exports.isObject = function(arg) {
  return '[object Object]' == exports.toString(arg);
};

/**
 * Determines if `arg` is an array.
 *
 * @param {Object}
 * @return {Boolean}
 * @see nodejs utils
 */

exports.isArray = function(arg) {
  return Array.isArray(arg) ||
    'object' == typeof arg && '[object Array]' == exports.toString(arg);
};

/**
 * Object.keys helper
 */

exports.keys = Object.keys || function(obj) {
  var keys = [];
  for (var k in obj) if (obj.hasOwnProperty(k)) {
    keys.push(k);
  }
  return keys;
};

/**
 * Basic Object.create polyfill.
 * Only one argument is supported.
 *
 * Based on https://developer.mozilla.org/en-US/docs/JavaScript/Reference/Global_Objects/Object/create
 */

exports.create = 'function' == typeof Object.create
  ? Object.create
  : create;

function create(proto) {
  if (arguments.length > 1) {
    throw new Error('Adding properties is not supported');
  }

  function F() {}
  F.prototype = proto;
  return new F;
}

/**
 * inheritance
 */

exports.inherits = function(ctor, superCtor) {
  ctor.prototype = exports.create(superCtor.prototype);
  ctor.prototype.constructor = ctor;
};

/**
 * nextTick helper
 * compat with node 0.10 which behaves differently than previous versions
 */

var soon = exports.soon = 'function' == typeof setImmediate
  ? setImmediate
  : process.nextTick;

/**
 * Clones the contents of a buffer.
 *
 * @param {Buffer} buff
 * @return {Buffer}
 */

exports.cloneBuffer = function(buff) {
  var dupe = Buffer.alloc(buff.length);
  buff.copy(dupe, 0, 0, buff.length);
  return dupe;
};

/**
 * Check if this object is an arguments object
 *
 * @param {Any} v
 * @return {Boolean}
 */

exports.isArgumentsObject = function(v) {
  return Object.prototype.toString.call(v) === '[object Arguments]';
};
