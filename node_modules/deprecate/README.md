# deprecate

[![Build Status](https://secure.travis-ci.org/brianc/node-deprecate.png?branch=master)](http://travis-ci.org/brianc/node-deprecate)

Mark a method as deprecated.  Write a message to a stream the first time the deprecated method is called.

## api

`var deprecate = require('deprecate');`

### deprecate([string message1 [, string message2 [,...]]])

Call `deprecate` within a function you are deprecating.  It will spit out all the messages to the console the first time _and only the first time_ the method is called.

```js
var deprecate = require('deprecate');

var someDeprecatedFunction = function() {
  deprecate('someDeprecatedFunction() is deprecated');
};

someDeprecatedFunction();
someDeprecatedFunction();
someDeprecatedFunction();
console.log('end');

//program output:

WARNING!!
someDeprecatedFunction() is deprecated


end
```

### deprecate.color

Set to `false` to not output a color.  Defaults to `'\x1b[31;1m'` which is red.

### deprecate.silence

Do nothing at all when the deprecate method is called.

### deprecate.stream

The to which output is written.  Defaults to `process.stderr`

## license

MIT
