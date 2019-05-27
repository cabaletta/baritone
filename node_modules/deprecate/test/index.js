var assert = require('assert');

var deprecate = require(__dirname + '/../');

var output = {
  _text: [],
  _clear: function() {
    this._text = [];
  },
  write: function(message) {
    this._text.push(message);
  }
}

describe('deprecate', function() {
  beforeEach(function() {
    output._clear();
    deprecate.stream = output;
  });

  it('does nothing if silence is turned on', function() {
    deprecate.silence = true;
    deprecate('this method is deprecated and will be removed');
    assert.equal(output._text.length, 0);
  });

  it('prints to output if silence is turned off', function() {
    deprecate.silence = false;
    deprecate('line1', 'line2', 'line3');
    var text = output._text.join(' ');
    assert(text.indexOf('WARNING') > 0, 'should have contained the string "warning"');
    assert(text.indexOf('line1') > 0, 'should have contained the string "line1"');
    assert(text.indexOf('line2') > 0, 'should have contained the string "line2"');
    assert(text.indexOf('line2') > text.indexOf('line1'), 'line 2 should come after line 1');
    assert(text.indexOf(deprecate.color) > 0, 'should have color');
  });


  it('does not print color if color turned off', function() {
    deprecate.color = false;
    deprecate('test');
    var text = output._text.join(' ');
    assert.equal(text.indexOf(deprecate.color), -1, 'should not have color string');
    assert.equal(text.indexOf('\x1b[0m'), -1, 'should not have reset color char ');
  });

  it('only prints once for each function deprecated', function() {
    var someDeprecatedMethod = function() {
      deprecate('first');
    }
    var someOtherDeprecatedMethod = function() {
      deprecate('second');
    }
    assert.equal(output._text.length, 0);
    someDeprecatedMethod();
    var length = output._text.length;
    assert(length > 0, "should have printed deprecation warning");
    someDeprecatedMethod();
    assert.equal(length, output._text.length, "should not have warned again");
    someOtherDeprecatedMethod();
    assert(output._text.length > length);
  });

});
