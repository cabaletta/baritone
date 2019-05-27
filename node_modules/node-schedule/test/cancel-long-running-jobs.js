'use strict';

var schedule = require('../lib/schedule');

module.exports = {
  'Cancel Long Running Job': {
    'should work even when recurring jobs are to be run on the past': function (test) {
      var ok = true;
      var job = schedule.scheduleJob('*/1 * * * * *', function () {
        test.ok(ok);
        var time = Date.now();
        while (ok && (Date.now() - time < 2000)) {
        }
      });

      test.ok(job);
      setTimeout(function () {
        job.cancel();
        test.done();
        ok = false;
      }, 2100);
    }
  }
};
