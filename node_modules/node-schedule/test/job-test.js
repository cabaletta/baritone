
'use strict';

var sinon = require('sinon');
var main = require('../package.json').main;
var schedule = require('../' + main);

var es6;
try {
  eval('(function* () {})()');
  es6 = require('./es6/job-test')(schedule);
} catch (e) {}

var clock;

module.exports = {
  setUp: function(cb) {
    clock = sinon.useFakeTimers();
    cb();
  },
  "Job constructor": {
    "Accepts Job name and function to run": function(test) {
      var job = new schedule.Job('the job', function() {});

      test.equal(job.name, 'the job');
      test.done();
    },
    "Job name is optional and will be auto-generated": function(test) {
      var job = new schedule.Job();

      test.ok(job.name);
      test.done();
    },
    "Uses unique names across auto-generated Job names": function(test) {
      var job1 = new schedule.Job();
      var job2 = new schedule.Job();

      test.notEqual(job1.name, job2.name);
      test.done();
    }
  },
  "#schedule(Date)": {
    "Runs job once at some date": function(test) {
      test.expect(1);

      var job = new schedule.Job(function() {
        test.ok(true);
      });

      job.schedule(new Date(Date.now() + 3000));

      setTimeout(function() {
        test.done();
      }, 3250);

      clock.tick(3250);
    },
    "Cancel next job before it runs": function(test) {
      test.expect(1);

      var job = new schedule.Job(function() {
        test.ok(true);
      });

      job.schedule(new Date(Date.now() + 1500));
      job.schedule(new Date(Date.now() + 3000));
      job.cancelNext();
      setTimeout(function() {
        test.done();
      }, 3250);

      clock.tick(3250);
    },
    "Run job on specified date": function(test) {
      test.expect(1);

      var job = new schedule.Job(function() {
        test.ok(true);
      });

      job.runOnDate(new Date(Date.now() + 3000));

      setTimeout(function() {
        test.done();
      }, 3250);

      clock.tick(3250);
    },
    "Run job in generator": function(test) {
      if (!es6) {
        test.expect(0);
        test.done();
        return;
      }

      es6.jobInGenerator(test);

      clock.tick(3250);
    },
    "Context is passed into generator correctly": function(test) {
      if (!es6) {
        test.expect(0);
        test.done();
        return;
      }

      es6.jobContextInGenerator(test);

      clock.tick(3250);
    },
    "Won't run job if scheduled in the past": function(test) {
      test.expect(0);

      var job = new schedule.Job(function() {
        test.ok(false);
      });

      job.schedule(new Date(Date.now() - 3000));

      setTimeout(function() {
        test.done();
      }, 1000);

      clock.tick(1000);
    },
    "Jobs still run after scheduling a Job in the past": function(test) {
      test.expect(1);

      var pastJob = new schedule.Job(function() {
      // Should not run, blow up if it does
        test.ok(false);
      });

      pastJob.schedule(new Date(Date.now() - 3000));

      var job = new schedule.Job(function() {
        test.ok(true);
      });

      job.schedule(new Date(Date.now() + 3000));

      setTimeout(function() {
        test.done();
      }, 3250);

      clock.tick(3250);
    },
    "Job emits 'scheduled' event with 'run at' Date": function(test) {
      test.expect(1);

      var date = new Date(Date.now() + 3000);
      var job = new schedule.Job(function() {
        test.done();
      });

      job.on('scheduled', function(runAtDate) {
        test.equal(runAtDate.getTime(), date.getTime());
      });

      job.schedule(date);
      clock.tick(3250);
    }
  },
  "#schedule(Date, fn)": {
    "Runs job once at some date, calls callback when done": function(test) {
      test.expect(1);

      var job = new schedule.Job(function() {}, function() {
        test.ok(true);
      });

      job.schedule(new Date(Date.now() + 3000));

      setTimeout(function() {
        test.done();
      }, 3250);

      clock.tick(3250);
    }
  },
  "#schedule(RecurrenceRule)": {
    "Runs job at interval based on recur rule, repeating indefinitely": function(test) {
      test.expect(3);

      var job = new schedule.Job(function() {
        test.ok(true);
      });

      var rule = new schedule.RecurrenceRule();
      rule.second = null; // fire every second

      job.schedule(rule);

      setTimeout(function() {
        job.cancel();
        test.done();
      }, 3250);

      clock.tick(3250);
    },
    "Job emits 'scheduled' event for every next invocation": function(test) {
      // Job will run 3 times but be scheduled 4 times, 4th run never happens
      // due to cancel.
      test.expect(4);

      var job = new schedule.Job(function() {});

      job.on('scheduled', function(runOnDate) {
        test.ok(true);
      });

      var rule = new schedule.RecurrenceRule();
      rule.second = null; // fire every second

      job.schedule(rule);

      setTimeout(function() {
        job.cancel();
        test.done();
      }, 3250);

      clock.tick(3250);
    },
    "Doesn't invoke job if recur rule schedules it in the past": function(test) {
      test.expect(0);

      var job = new schedule.Job(function() {
        test.ok(false);
      });

      var rule = new schedule.RecurrenceRule();
      rule.year = 2000;

      job.schedule(rule);

      setTimeout(function() {
        job.cancel();
        test.done();
      }, 1000);

      clock.tick(1000);
    }
  },
  "#schedule({...})": {
    "Runs job at interval based on object, repeating indefinitely": function(test) {
      test.expect(3);

      var job = new schedule.Job(function() {
        test.ok(true);
      });

      job.schedule({
        second: null // fire every second
      });

      setTimeout(function() {
        job.cancel();
        test.done();
      }, 3250);

      clock.tick(3250);
    },
    "Job emits 'scheduled' event for every next invocation": function(test) {
      // Job will run 3 times but be scheduled 4 times, 4th run never happens
      // due to cancel.
      test.expect(4);

      var job = new schedule.Job(function() {});

      job.on('scheduled', function(runOnDate) {
        test.ok(true);
      });

      job.schedule({
        second: null // Fire every second
      });

      setTimeout(function() {
        job.cancel();
        test.done();
      }, 3250);

      clock.tick(3250);
    },
    "Doesn't invoke job if object schedules it in the past": function(test) {
      test.expect(0);

      var job = new schedule.Job(function() {
        test.ok(false);
      });

      job.schedule({
        year: 2000
      });

      setTimeout(function() {
        job.cancel();
        test.done();
      }, 1000);

      clock.tick(1000);
    }
  },
  "#schedule('jobName', {...})": {
    "Runs job with a custom name input": function(test) {
      test.expect(3);

      var job = new schedule.Job('jobName', function() {
        test.equal(job.name, 'jobName');
      });

      job.schedule({
        second: null // fire every second
      });

      setTimeout(function() {
        job.cancel();
        test.done();
      }, 3250);

      clock.tick(3250);
    }
  },
  "#schedule({...}, {...})": {
    "Runs job and run callback when job is done if callback is provided": function(test) {
      test.expect(3);

      var job = new schedule.Job(function() {}, function() {
        test.ok(true);
      });

      job.schedule({
        second: null // fire every second
      });

      setTimeout(function() {
        job.cancel();
        test.done();
      }, 3250);

      clock.tick(3250);
    },
    "Runs job with a custom name input and run callback when job is done": function(test) {
      test.expect(3);

      var job = new schedule.Job('MyJob', function() {}, function() {
        test.equal(job.name, 'MyJob');
      });

      job.schedule({
        second: null // fire every second
      });

      setTimeout(function() {
        job.cancel();
        test.done();
      }, 3250);

      clock.tick(3250);
    }
  },
  "#cancel": {
    "Prevents all future invocations": function(test) {
      test.expect(1);

      var job = new schedule.Job(function() {
        test.ok(true);
      });

      job.schedule({
        second: null // fire every second
      });

      setTimeout(function() {
        job.cancel();
      }, 1250);

      setTimeout(function() {
        test.done();
      }, 2250);

      clock.tick(2250);
    },
    "Job emits 'canceled' event": function(test) {
      test.expect(1);

      var job = new schedule.Job(function() {});

      job.on('canceled', function() {
        test.ok(true);
      });

      job.schedule({
        second: null // fire every second
      });

      setTimeout(function() {
        job.cancel();
      }, 1250);

      setTimeout(function() {
        test.done();
      }, 2250);

      clock.tick(2250);
    },
    "Job is added to scheduledJobs when created and removed when cancelled": function(test) {
      test.expect(4);

      var job1 = new schedule.Job('cancelJob', function() {});
      job1.schedule({
        second: null // fire every second
      });

      var job2 = schedule.scheduleJob('second',
                                      { second: null },
                                      function() {},
                                      function() {});

      test.strictEqual(schedule.scheduledJobs.cancelJob, job1);
      test.strictEqual(schedule.scheduledJobs.second, job2);
      setTimeout(function() {
        job1.cancel();
        job2.cancel();
        test.strictEqual(schedule.scheduledJobs.cancelJob, undefined);
        test.strictEqual(schedule.scheduledJobs.second, undefined);
        test.done();
      }, 1250);

      clock.tick(1250);
    }
  },
  "#reschedule": {
    "When rescheduled counter will be reset to zero": function(test) {

      var job = new schedule.scheduleJob({
        second: null
      }, function() {});

      setTimeout(function() {
        test.equal(job.triggeredJobs(), 3);
        schedule.rescheduleJob(job, {
          minute: null
        });
      }, 3250);

      setTimeout(function() {
        job.cancel();
        test.equal(job.triggeredJobs(), 0);
        test.done();
      }, 5000);

      clock.tick(5000);
    }
  },
  "When invoked": {
    "Job emits 'run' event": function(test) {
      test.expect(1);

      var job = new schedule.Job(function() {});

      job.on('run', function() {
        test.ok(true);
      });

      job.schedule(new Date(Date.now() + 3000));

      setTimeout(function() {
        test.done();
      }, 3250);

      clock.tick(3250);
    },
    "Job counter increase properly": function(test) {
      var job = new schedule.Job(function() {});

      job.schedule({
        second: null // fire every second
      });

      setTimeout(function() {
        job.cancel();
        test.equal(job.triggeredJobs(), 2);
        test.done();
      }, 2250);

      clock.tick(2250);
    },
    "Job gets invoked with the fire date": function (test) {
      test.expect(2);
      var prevFireDate;
      var job = new schedule.Job(function (fireDate) {
        if (!prevFireDate) {
          test.ok(fireDate instanceof Date);
        } else {
          test.equal(fireDate.getTime() - prevFireDate.getTime(), 1000);
        }
        prevFireDate = fireDate;
      });

      job.schedule({
        second: null // fire every second
      });

      setTimeout(function () {
        job.cancel();
        test.done();
      }, 2250);

      clock.tick(2250);
    }
  },
  tearDown: function(cb) {
    clock.restore();
    cb();
  }
};
