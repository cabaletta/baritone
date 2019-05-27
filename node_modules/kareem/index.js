'use strict';

function Kareem() {
  this._pres = new Map();
  this._posts = new Map();
}

Kareem.prototype.execPre = function(name, context, args, callback) {
  if (arguments.length === 3) {
    callback = args;
    args = [];
  }
  var pres = get(this._pres, name, []);
  var numPres = pres.length;
  var numAsyncPres = pres.numAsync || 0;
  var currentPre = 0;
  var asyncPresLeft = numAsyncPres;
  var done = false;
  var $args = args;

  if (!numPres) {
    return process.nextTick(function() {
      callback(null);
    });
  }

  var next = function() {
    if (currentPre >= numPres) {
      return;
    }
    var pre = pres[currentPre];

    if (pre.isAsync) {
      var args = [
        decorateNextFn(_next),
        decorateNextFn(function(error) {
          if (error) {
            if (done) {
              return;
            }
            done = true;
            return callback(error);
          }
          if (--asyncPresLeft === 0 && currentPre >= numPres) {
            return callback(null);
          }
        })
      ];

      callMiddlewareFunction(pre.fn, context, args, args[0]);
    } else if (pre.fn.length > 0) {
      var args = [decorateNextFn(_next)];
      var _args = arguments.length >= 2 ? arguments : [null].concat($args);
      for (var i = 1; i < _args.length; ++i) {
        args.push(_args[i]);
      }

      callMiddlewareFunction(pre.fn, context, args, args[0]);
    } else {
      let error = null;
      let maybePromise = null;
      try {
        maybePromise = pre.fn.call(context);
      } catch (err) {
        error = err;
      }

      if (isPromise(maybePromise)) {
        maybePromise.then(() => _next(), err => _next(err));
      } else {
        if (++currentPre >= numPres) {
          if (asyncPresLeft > 0) {
            // Leave parallel hooks to run
            return;
          } else {
            return process.nextTick(function() {
              callback(error);
            });
          }
        }
        next(error);
      }
    }
  };

  next.apply(null, [null].concat(args));

  function _next(error) {
    if (error) {
      if (done) {
        return;
      }
      done = true;
      return callback(error);
    }

    if (++currentPre >= numPres) {
      if (asyncPresLeft > 0) {
        // Leave parallel hooks to run
        return;
      } else {
        return callback(null);
      }
    }

    next.apply(context, arguments);
  }
};

Kareem.prototype.execPreSync = function(name, context, args) {
  var pres = get(this._pres, name, []);
  var numPres = pres.length;

  for (var i = 0; i < numPres; ++i) {
    pres[i].fn.apply(context, args || []);
  }
};

Kareem.prototype.execPost = function(name, context, args, options, callback) {
  if (arguments.length < 5) {
    callback = options;
    options = null;
  }
  var posts = get(this._posts, name, []);
  var numPosts = posts.length;
  var currentPost = 0;

  var firstError = null;
  if (options && options.error) {
    firstError = options.error;
  }

  if (!numPosts) {
    return process.nextTick(function() {
      callback.apply(null, [firstError].concat(args));
    });
  }

  var next = function() {
    var post = posts[currentPost];
    var numArgs = 0;
    var argLength = args.length;
    var newArgs = [];
    for (var i = 0; i < argLength; ++i) {
      numArgs += args[i] && args[i]._kareemIgnore ? 0 : 1;
      if (!args[i] || !args[i]._kareemIgnore) {
        newArgs.push(args[i]);
      }
    }

    if (firstError) {
      if (post.length === numArgs + 2) {
        var _cb = decorateNextFn(function(error) {
          if (error) {
            firstError = error;
          }
          if (++currentPost >= numPosts) {
            return callback.call(null, firstError);
          }
          next();
        });

        callMiddlewareFunction(post, context,
          [firstError].concat(newArgs).concat([_cb]), _cb);
      } else {
        if (++currentPost >= numPosts) {
          return callback.call(null, firstError);
        }
        next();
      }
    } else {
      const _cb = decorateNextFn(function(error) {
        if (error) {
          firstError = error;
          return next();
        }

        if (++currentPost >= numPosts) {
          return callback.apply(null, [null].concat(args));
        }

        next();
      });

      if (post.length === numArgs + 2) {
        // Skip error handlers if no error
        if (++currentPost >= numPosts) {
          return callback.apply(null, [null].concat(args));
        }
        return next();
      }
      if (post.length === numArgs + 1) {
        callMiddlewareFunction(post, context, newArgs.concat([_cb]), _cb);
      } else {
        let error;
        let maybePromise;
        try {
          maybePromise = post.apply(context, newArgs);
        } catch (err) {
          error = err;
          firstError = err;
        }

        if (isPromise(maybePromise)) {
          return maybePromise.then(() => _cb(), err => _cb(err));
        }

        if (++currentPost >= numPosts) {
          return callback.apply(null, [error].concat(args));
        }

        next(error);
      }
    }
  };

  next();
};

Kareem.prototype.execPostSync = function(name, context, args) {
  const posts = get(this._posts, name, []);
  const numPosts = posts.length;

  for (let i = 0; i < numPosts; ++i) {
    posts[i].apply(context, args || []);
  }
};

Kareem.prototype.createWrapperSync = function(name, fn) {
  var kareem = this;
  return function syncWrapper() {
    kareem.execPreSync(name, this, arguments);

    var toReturn = fn.apply(this, arguments);

    kareem.execPostSync(name, this, [toReturn]);

    return toReturn;
  };
}

function _handleWrapError(instance, error, name, context, args, options, callback) {
  if (options.useErrorHandlers) {
    var _options = { error: error };
    return instance.execPost(name, context, args, _options, function(error) {
      return typeof callback === 'function' && callback(error);
    });
  } else {
    return typeof callback === 'function' ?
      callback(error) :
      undefined;
  }
}

Kareem.prototype.wrap = function(name, fn, context, args, options) {
  const lastArg = (args.length > 0 ? args[args.length - 1] : null);
  const argsWithoutCb = typeof lastArg === 'function' ?
    args.slice(0, args.length - 1) :
    args;
  const _this = this;

  options = options || {};
  const checkForPromise = options.checkForPromise;

  this.execPre(name, context, args, function(error) {
    if (error) {
      const numCallbackParams = options.numCallbackParams || 0;
      const errorArgs = options.contextParameter ? [context] : [];
      for (var i = errorArgs.length; i < numCallbackParams; ++i) {
        errorArgs.push(null);
      }
      return _handleWrapError(_this, error, name, context, errorArgs,
        options, lastArg);
    }

    const end = (typeof lastArg === 'function' ? args.length - 1 : args.length);
    const numParameters = fn.length;
    const ret = fn.apply(context, args.slice(0, end).concat(_cb));

    if (checkForPromise) {
      if (ret != null && typeof ret.then === 'function') {
        // Thenable, use it
        return ret.then(
          res => _cb(null, res),
          err => _cb(err)
        );
      }

      // If `fn()` doesn't have a callback argument and doesn't return a
      // promise, assume it is sync
      if (numParameters < end + 1) {
        return _cb(null, ret);
      }
    }

    function _cb() {
      const args = arguments;
      const argsWithoutError = Array.prototype.slice.call(arguments, 1);
      if (options.nullResultByDefault && argsWithoutError.length === 0) {
        argsWithoutError.push(null);
      }
      if (arguments[0]) {
        // Assume error
        return _handleWrapError(_this, arguments[0], name, context,
          argsWithoutError, options, lastArg);
      } else {
        _this.execPost(name, context, argsWithoutError, function() {
          if (arguments[0]) {
            return typeof lastArg === 'function' ?
              lastArg(arguments[0]) :
              undefined;
          }

          return typeof lastArg === 'function' ?
            lastArg.apply(context, arguments) :
            undefined;
        });
      }
    }
  });
};

Kareem.prototype.hasHooks = function(name) {
  return this._pres.has(name) || this._posts.has(name);
};

Kareem.prototype.createWrapper = function(name, fn, context, options) {
  var _this = this;
  if (!this.hasHooks(name)) {
    // Fast path: if there's no hooks for this function, just return the
    // function wrapped in a nextTick()
    return function() {
      process.nextTick(() => fn.apply(this, arguments));
    };
  }
  return function() {
    var _context = context || this;
    var args = Array.prototype.slice.call(arguments);
    _this.wrap(name, fn, _context, args, options);
  };
};

Kareem.prototype.pre = function(name, isAsync, fn, error, unshift) {
  if (typeof arguments[1] !== 'boolean') {
    error = fn;
    fn = isAsync;
    isAsync = false;
  }

  const pres = get(this._pres, name, []);
  this._pres.set(name, pres);

  if (isAsync) {
    pres.numAsync = pres.numAsync || 0;
    ++pres.numAsync;
  }

  if (unshift) {
    pres.unshift({ fn: fn, isAsync: isAsync });
  } else {
    pres.push({ fn: fn, isAsync: isAsync });
  }

  return this;
};

Kareem.prototype.post = function(name, fn, unshift) {
  const hooks = get(this._posts, name, []);

  if (unshift) {
    hooks.unshift(fn);
  } else {
    hooks.push(fn);
  }
  this._posts.set(name, hooks);
  return this;
};

Kareem.prototype.clone = function() {
  const n = new Kareem();

  for (let key of this._pres.keys()) {
    const clone = this._pres.get(key).slice();
    clone.numAsync = this._pres.get(key).numAsync;
    n._pres.set(key, clone);
  }
  for (let key of this._posts.keys()) {
    n._posts.set(key, this._posts.get(key).slice());
  }

  return n;
};

Kareem.prototype.merge = function(other) {
  var ret = this.clone();

  for (let key of other._pres.keys()) {
    const sourcePres = get(ret._pres, key, []);
    const deduplicated = other._pres.get(key).
      // Deduplicate based on `fn`
      filter(p => sourcePres.map(_p => _p.fn).indexOf(p.fn) === -1);
    const combined = sourcePres.concat(deduplicated);
    combined.numAsync = sourcePres.numAsync || 0;
    combined.numAsync += deduplicated.filter(p => p.isAsync).length;
    ret._pres.set(key, combined);
  }
  for (let key of other._posts.keys()) {
    const sourcePosts = get(ret._posts, key, []);
    const deduplicated = other._posts.get(key).
      filter(p => sourcePosts.indexOf(p) === -1);
    ret._posts.set(key, sourcePosts.concat(deduplicated));
  }

  return ret;
};

function get(map, key, def) {
  if (map.has(key)) {
    return map.get(key);
  }
  return def;
}

function callMiddlewareFunction(fn, context, args, next) {
  let maybePromise;
  try {
    maybePromise = fn.apply(context, args);
  } catch (error) {
    return next(error);
  }

  if (isPromise(maybePromise)) {
    maybePromise.then(() => next(), err => next(err));
  }
}

function isPromise(v) {
  return v != null && typeof v.then === 'function';
}

function decorateNextFn(fn) {
  var called = false;
  var _this = this;
  return function() {
    // Ensure this function can only be called once
    if (called) {
      return;
    }
    called = true;
    // Make sure to clear the stack so try/catch doesn't catch errors
    // in subsequent middleware
    return process.nextTick(() => fn.apply(_this, arguments));
  };
}

module.exports = Kareem;
