
'use strict';

module.exports = function(schedule) {
  return {
    jobInGenerator: function(test) {
      test.expect(1);

      var job = new schedule.Job(function*() {
        test.ok(true);
      });

      job.runOnDate(new Date(Date.now() + 3000));

      setTimeout(function() {
        test.done();
      }, 3250);
    },
    jobContextInGenerator: function(test) {
      test.expect(1);

      var job = new schedule.Job('name of job', function*() {
        test.ok(this.name === 'name of job');
      });

      job.runOnDate(new Date(Date.now() + 3000));

      setTimeout(function() {
        test.done();
      }, 3250);
    }
  }
}
