
'use strict';

var sinon = require('sinon');
var main = require('../package.json').main;
var schedule = require('../' + main);

var clock;

module.exports = {
  setUp: function(cb) {
    clock = sinon.useFakeTimers();
    cb();
  },
  ".scheduleJob": {
    "Returns Job instance": function(test) {
      var job = schedule.scheduleJob(new Date(Date.now() + 1000), function() {});

      test.ok(job instanceof schedule.Job);

      job.cancel();
      test.done();
    }
  },
  ".scheduleJob(Date, fn)": {
    "Runs job once at some date": function(test) {
      test.expect(1);

      schedule.scheduleJob(new Date(Date.now() + 3000), function() {
        test.ok(true);
      });

      setTimeout(function() {
        test.done();
      }, 3250);

      clock.tick(3250);
    },
    "Job doesn't emit initial 'scheduled' event": function(test) {
      var job = schedule.scheduleJob(new Date(Date.now() + 1000), function() {});

      job.on('scheduled', function() {
        test.ok(false);
      });

      setTimeout(function() {
        test.done();
      }, 1250);

      clock.tick(1250);
    },
    "Won't run job if scheduled in the past": function(test) {
      test.expect(1);
      var job = schedule.scheduleJob(new Date(Date.now() - 3000), function() {
        test.ok(false);
      });

      test.equal(job, null);

      setTimeout(function() {
        test.done();
      }, 1000);

      clock.tick(1000);
    }
  },
  ".scheduleJob(RecurrenceRule, fn)": {
    "Runs job at interval based on recur rule, repeating indefinitely": function(test) {
      test.expect(3);

      var rule = new schedule.RecurrenceRule();
      rule.second = null; // fire every second

      var job = schedule.scheduleJob(rule, function() {
        test.ok(true);
      });

      setTimeout(function() {
        job.cancel();
        test.done();
      }, 3250);

      clock.tick(3250);
    },
    "Job doesn't emit initial 'scheduled' event": function(test) {
      /*
        * If this was Job#schedule it'd fire 4 times.
        */
      test.expect(3);

      var rule = new schedule.RecurrenceRule();
      rule.second = null; // fire every second

      var job = new schedule.scheduleJob(rule, function() {});

      job.on('scheduled', function(runOnDate) {
        test.ok(true);
      });

      setTimeout(function() {
        job.cancel();
        test.done();
      }, 3250);

      clock.tick(3250);
    },
    "Doesn't invoke job if recur rule schedules it in the past": function(test) {
      test.expect(1);
      var rule = new schedule.RecurrenceRule();
      rule.year = 1960;

      var job = schedule.scheduleJob(rule, function() {
        test.ok(false);
      });
      
      test.equal(job, null);

      setTimeout(function() {
        test.done();
      }, 1000);

      clock.tick(1000);
    }
  },
  ".scheduleJob({...}, fn)": {
    "Runs job at interval based on object, repeating indefinitely": function(test) {
      test.expect(3);

      var job = new schedule.scheduleJob({
        second: null // Fire every second
      }, function() {
        test.ok(true);
      });

      setTimeout(function() {
        job.cancel();
        test.done();
      }, 3250);

      clock.tick(3250);
    },
    "Job doesn't emit initial 'scheduled' event": function(test) {
      /*
        * With Job#schedule this would be 3:
        *  scheduled at time 0
        *  scheduled at time 1000
        *  scheduled at time 2000
        */
      test.expect(2);

      var job = schedule.scheduleJob({
        second: null // fire every second
      }, function() {});

      job.on('scheduled', function() {
        test.ok(true);
      });

      setTimeout(function() {
        job.cancel();
        test.done();
      }, 2250);

      clock.tick(2250);
    },
    "Doesn't invoke job if object schedules it in the past": function(test) {
      test.expect(1);
    
      var job = schedule.scheduleJob({
        year: 1960
      }, function() {
        test.ok(false);
      });
      
      test.equal(job, null);

      setTimeout(function() {
        test.done();
      }, 1000);

      clock.tick(1000);
    }
  },
  ".scheduleJob({...}, {...}, fn)": {
    "Callback called for each job if callback is provided": function(test) {
      test.expect(3);

      var job = new schedule.scheduleJob({
        second: null // Fire every second
      }, function() {}, function() {
        test.ok(true);
      });

      setTimeout(function() {
        job.cancel();
        test.done();
      }, 3250);

      clock.tick(3250);
    }
  },
  ".rescheduleJob(job, {...})": {
    "Reschedule jobs from object based to object based": function(test) {
      test.expect(3);

      var job = new schedule.scheduleJob({
        second: null
      }, function() {
        test.ok(true);
      });

      setTimeout(function() {
        schedule.rescheduleJob(job, {
          minute: null
        });
      }, 3250);

      setTimeout(function() {
        job.cancel();
        test.done();
      }, 5000);

      clock.tick(5000);
    },
    "Reschedule jobs from every minutes to every second": function(test) {
      test.expect(3);

      var timeout = 60 * 1000;

      var job = new schedule.scheduleJob({
        minute: null
      }, function() {
        test.ok(true);
      });

      setTimeout(function() {
        schedule.rescheduleJob(job, {
          second: null
        });
      }, timeout);

      setTimeout(function() {
        job.cancel();
        test.done();
      }, timeout + 2250);

      clock.tick(timeout + 2250);
    }
  },
  ".rescheduleJob(job, Date)": {
    "Reschedule jobs from Date to Date": function(test) {
      test.expect(1);

      var job = new schedule.scheduleJob(new Date(Date.now() + 3000), function() {
        test.ok(true);
      });

      setTimeout(function() {
        schedule.rescheduleJob(job, new Date(Date.now() + 5000));
      }, 1000);

      setTimeout(function() {
        test.done();
      }, 6150);

      clock.tick(6150);
    },
    "Reschedule jobs that has been executed": function(test) {
      test.expect(2);

      var job = new schedule.scheduleJob(new Date(Date.now() + 1000), function() {
        test.ok(true);
      });

      setTimeout(function() {
        schedule.rescheduleJob(job, new Date(Date.now() + 2000));
      }, 2000);

      setTimeout(function() {
        test.done();
      }, 5150);

      clock.tick(5150);
    }
  },
  ".rescheduleJob(job, RecurrenceRule)": {
    "Reschedule jobs from RecurrenceRule to RecurrenceRule": function(test) {
      test.expect(3);

      var timeout = 60 * 1000;

      var rule = new schedule.RecurrenceRule();
      rule.second = null; // fire every second

      var job = schedule.scheduleJob(rule, function() {
        test.ok(true);
      });

      var newRule = new schedule.RecurrenceRule();
      newRule.minute = null;

      setTimeout(function() {
        schedule.rescheduleJob(job, newRule);
      }, 2250);

      setTimeout(function() {
        job.cancel();
        test.done();
      }, timeout + 2250);

      clock.tick(timeout + 2250);
    },
    "Reschedule jobs from RecurrenceRule to Date": function(test) {
      test.expect(3);

      var rule = new schedule.RecurrenceRule();
      rule.second = null; // fire every second

      var job = schedule.scheduleJob(rule, function() {
        test.ok(true);
      });

      setTimeout(function() {
        schedule.rescheduleJob(job, new Date(Date.now() + 2000));
      }, 2150);

      setTimeout(function() {
        test.done();
      }, 4250);

      clock.tick(4250);
    },
    "Reschedule jobs from RecurrenceRule to {...}": function(test) {
      test.expect(3);

      var timeout = 60 * 1000;

      var rule = new schedule.RecurrenceRule();
      rule.second = null; // fire every second

      var job = schedule.scheduleJob(rule, function() {
        test.ok(true);
      });

      setTimeout(function() {
        schedule.rescheduleJob(job, {
          minute: null
        });
      }, 2150);

      setTimeout(function() {
        job.cancel();
        test.done();
      }, timeout + 2150);

      clock.tick(timeout + 2150);
    },
    "Reschedule jobs that is not available": function(test) {
      test.expect(4);

      var rule = new schedule.RecurrenceRule();
      rule.second = null; // fire every second

      var job = schedule.scheduleJob(rule, function() {
        test.ok(true);
      });

      setTimeout(function() {
        schedule.rescheduleJob(null, new Date(Date.now() + 2000));
      }, 2150);

      setTimeout(function() {
        job.cancel();
        test.done();
      }, 4250);

      clock.tick(4250);
    }
  },
  '.rescheduleJob("job name", {...})': {
    "Reschedule jobs from object based to object based": function(test) {
      test.expect(3);

      var job = new schedule.scheduleJob({
        second: null
      }, function() {
        test.ok(true);
      });

      setTimeout(function() {
        schedule.rescheduleJob(job.name, {
          minute: null
        });
      }, 3250);

      setTimeout(function() {
        job.cancel();
        test.done();
      }, 5000);

      clock.tick(5000);
    },
    "Reschedule jobs from every minutes to every second": function(test) {
      test.expect(3);

      var timeout = 60 * 1000;

      var job = new schedule.scheduleJob({
        minute: null
      }, function() {
        test.ok(true);
      });

      setTimeout(function() {
        schedule.rescheduleJob(job.name, {
          second: null
        });
      }, timeout);

      setTimeout(function() {
        job.cancel();
        test.done();
      }, timeout + 2250);

      clock.tick(timeout + 2250);
    }
  },
  '.rescheduleJob("job name", Date)': {
    "Reschedule jobs from Date to Date": function(test) {
      test.expect(1);

      var job = new schedule.scheduleJob(new Date(Date.now() + 3000), function() {
        test.ok(true);
      });

      setTimeout(function() {
        schedule.rescheduleJob(job.name, new Date(Date.now() + 5000));
      }, 1000);

      setTimeout(function() {
        test.done();
      }, 6150);

      clock.tick(6150);
    },
    "Reschedule jobs that has been executed": function(test) {
      test.expect(2);

      var job = new schedule.scheduleJob(new Date(Date.now() + 1000), function() {
        test.ok(true);
      });

      setTimeout(function() {
        schedule.rescheduleJob(job.name, new Date(Date.now() + 2000));
      }, 2000);

      setTimeout(function() {
        test.done();
      }, 5150);

      clock.tick(5150);
    }
  },
  '.rescheduleJob("job name", RecurrenceRule)': {
    "Reschedule jobs from RecurrenceRule to RecurrenceRule": function(test) {
      test.expect(3);

      var timeout = 60 * 1000;

      var rule = new schedule.RecurrenceRule();
      rule.second = null; // fire every second

      var job = schedule.scheduleJob(rule, function() {
        test.ok(true);
      });

      var newRule = new schedule.RecurrenceRule();
      newRule.minute = null;

      setTimeout(function() {
        schedule.rescheduleJob(job.name, newRule);
      }, 2250);

      setTimeout(function() {
        job.cancel();
        test.done();
      }, timeout + 2250);

      clock.tick(timeout + 2250);
    },
    "Reschedule jobs from RecurrenceRule to Date": function(test) {
      test.expect(3);

      var rule = new schedule.RecurrenceRule();
      rule.second = null; // fire every second

      var job = schedule.scheduleJob(rule, function() {
        test.ok(true);
      });

      setTimeout(function() {
        schedule.rescheduleJob(job.name, new Date(Date.now() + 2000));
      }, 2150);

      setTimeout(function() {
        test.done();
      }, 4250);

      clock.tick(4250);
    },
    "Reschedule jobs from RecurrenceRule to {...}": function(test) {
      test.expect(3);

      var timeout = 60 * 1000;

      var rule = new schedule.RecurrenceRule();
      rule.second = null; // fire every second

      var job = schedule.scheduleJob(rule, function() {
        test.ok(true);
      });

      setTimeout(function() {
        schedule.rescheduleJob(job.name, {
          minute: null
        });
      }, 2150);

      setTimeout(function() {
        job.cancel();
        test.done();
      }, timeout + 2150);

      clock.tick(timeout + 2150);
    },
    "Reschedule jobs that is not available": function(test) {
      test.expect(4);

      var rule = new schedule.RecurrenceRule();
      rule.second = null; // fire every second

      var job = schedule.scheduleJob(rule, function() {
        test.ok(true);
      });

      setTimeout(function() {
        schedule.rescheduleJob("Blah", new Date(Date.now() + 2000));
      }, 2150);

      setTimeout(function() {
        job.cancel();
        test.done();
      }, 4250);

      clock.tick(4250);
    }
  },
  ".cancelJob(Job)": {
    "Prevents all future invocations of Job passed in": function(test) {
      test.expect(2);

      var job = schedule.scheduleJob({
        second: null
      }, function() {
        test.ok(true);
      });

      setTimeout(function() {
        schedule.cancelJob(job);
      }, 2250);

      setTimeout(function() {
        test.done();
      }, 3250);

      clock.tick(3250);
    },
    "Can cancel Jobs scheduled with Job#schedule": function(test) {
      test.expect(2);

      var job = new schedule.Job(function() {
        test.ok(true);
      });

      job.schedule({
        second: null
      });

      setTimeout(function() {
        schedule.cancelJob(job);
      }, 2250);

      setTimeout(function() {
        test.done();
      }, 3250);

      clock.tick(3250);
    },
    "Job emits 'canceled' event": function(test) {
      test.expect(1);

      var job = schedule.scheduleJob({
        second: null
      }, function() {});

      job.on('canceled', function() {
        test.ok(true);
      });

      setTimeout(function() {
        schedule.cancelJob(job);
        test.done();
      }, 1250);

      clock.tick(1250);
    }
  },
  '.cancelJob("job name")': {
    "Prevents all future invocations of Job identified by name": function(test) {
      test.expect(2);

      var job = schedule.scheduleJob({
        second: null
      }, function() {
        test.ok(true);
      });

      setTimeout(function() {
        schedule.cancelJob(job.name);
      }, 2250);

      setTimeout(function() {
        test.done();
      }, 3250);

      clock.tick(3250);
    },
    /*
    "Can cancel Jobs scheduled with Job#schedule": function(test) {
      test.expect(2);

      var job = new schedule.Job(function() {
      test.ok(true);
      });

      job.schedule({
      second: null
      });

      setTimeout(function() {
      schedule.cancelJob(job.name);
      }, 2250);

      setTimeout(function() {
      test.done();
      }, 3250);
    },*/
    "Job emits 'canceled' event": function(test) {
      test.expect(1);

      var job = schedule.scheduleJob({
        second: null
      }, function() {});

      job.on('canceled', function() {
        test.ok(true);
      });

      setTimeout(function() {
        schedule.cancelJob(job.name);
        test.done();
      }, 1250);

      clock.tick(1250);
    },
    "Does nothing if no job found by that name": function(test) {
      test.expect(3);

      var job = schedule.scheduleJob({
        second: null
      }, function() {
        test.ok(true);
      });

      setTimeout(function() {
        // This cancel should not affect anything
        schedule.cancelJob('blah');
      }, 2250);

      setTimeout(function() {
        job.cancel(); // prevent tests from hanging
        test.done();
      }, 3250);

      clock.tick(3250);
    }
  },
  '.pendingInvocations()': {
    "Retrieves pendingInvocations of the job": function(test) {
      var job = schedule.scheduleJob(new Date(Date.now() + 1000), function() {});

      test.ok(job instanceof schedule.Job);
      test.ok(job.pendingInvocations()[0].job);

      job.cancel();
      test.done();
    }
  },
  tearDown: function(cb) {
    clock.restore();
    cb();
  }
};
