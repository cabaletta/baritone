
'use strict';

var main = require('../package.json').main;
var schedule = require('../' + main);

module.exports = {
  "step defaults to 1": function(test) {
    var range = new schedule.Range(2, 6);

    test.equals(1, range.step);

    test.done();
  },
  "when step is 1": {
    "setUp": function(done) {
      this.range = new schedule.Range(2, 6, 1);

      done();
    },
    "includes start value": function(test) {
      test.ok(this.range.contains(2));

      test.done();
    },
    "includes end value": function(test) {
      test.ok(this.range.contains(6));

      test.done();
    },
    "includes value between start and end": function(test) {
      test.ok(this.range.contains(3));

      test.done();
    },
    "excludes values outside of start and end": function(test) {
      test.ok(!this.range.contains(1));
      test.ok(!this.range.contains(7));

      test.done();
    }
  },
  "when step > 1": {
    "setUp": function(done) {
      this.range = new schedule.Range(2, 6, 2);

      done();
    },
    "includes start value": function(test) {
      test.ok(this.range.contains(2));

      test.done();
    },
    "excludes end value": function(test) {
      test.ok(!this.range.contains(6));

      test.done();
    },
    "includes value between start and end that is evenly divisible by step": function(test) {
      test.ok(this.range.contains(4));

      test.done();
    },
    "excludes value between start and end that is not evenly divisible by step": function(test) {
      test.ok(!this.range.contains(5));

      test.done();
    },
    "excludes values outside of start and end": function(test) {
      test.ok(!this.range.contains(1));
      test.ok(!this.range.contains(7));

      test.done();
    }
  }

};
