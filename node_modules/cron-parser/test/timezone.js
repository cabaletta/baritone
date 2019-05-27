var test = require('tap').test;
var CronExpression = require('../lib/expression');

test('It works on DST start', function(t) {
  try {
    var options = {
      currentDate: '2016-03-27 02:00:01',
      tz: 'Europe/Athens'
    };

    var interval, date;

    interval = CronExpression.parse('0 * * * *', options);
    t.ok(interval, 'Interval parsed');

    date = interval.next();
    t.equal(date.getMinutes(), 0, '0 Minutes');
    t.equal(date.getHours(), 4, 'Due to DST start in Athens, 3 is skipped');
    t.equal(date.getDate(), 27, 'on the 27th');

    date = interval.next();
    t.equal(date.getMinutes(), 0, '0 Minutes');
    t.equal(date.getHours(), 5, '5 AM');
    t.equal(date.getDate(), 27, 'on the 27th');

    interval = CronExpression.parse('30 2 * * *', options);
    t.ok(interval, 'Interval parsed');

    date = interval.next();
    t.equal(date.getMinutes(), 30, '30 Minutes');
    t.equal(date.getHours(), 2, '2 AM');
    t.equal(date.getDate(), 27, 'on the 27th');

    date = interval.next();
    t.equal(date.getMinutes(), 30, '30 Minutes');
    t.equal(date.getHours(), 2, '2 AM');
    t.equal(date.getDate(), 28, 'on the 28th');

    interval = CronExpression.parse('0 3 * * *', options);
    t.ok(interval, 'Interval parsed');

    date = interval.next();
    t.equal(date.getMinutes(), 0, '0 Minutes');
    t.equal(date.getHours(), 4, 'Due to DST start in Athens, 3 is skipped');
    t.equal(date.getDate(), 27, 'on the 27th');

    date = interval.next();
    t.equal(date.getMinutes(), 0, '0 Minutes');
    t.equal(date.getHours(), 3, '3 on the 28th');
    t.equal(date.getDate(), 28, 'on the 28th');

    interval = CronExpression.parse('*/20 3 * * *', options);
    t.ok(interval, 'Interval parsed');

    date = interval.next();
    t.equal(date.getMinutes(), 0, '0 Minutes');
    t.equal(date.getHours(), 4, 'Due to DST start in Athens, 3 is skipped');
    t.equal(date.getDate(), 27, 'on the 27th');

    date = interval.next();
    t.equal(date.getMinutes(), 20, '20 Minutes');
    t.equal(date.getHours(), 4, 'Due to DST start in Athens, 3 is skipped');
    t.equal(date.getDate(), 27, 'on the 27th');

    date = interval.next();
    t.equal(date.getMinutes(), 40, '20 Minutes');
    t.equal(date.getHours(), 4, 'Due to DST start in Athens, 3 is skipped');
    t.equal(date.getDate(), 27, 'on the 27th');

    date = interval.next();
    t.equal(date.getMinutes(), 0, '0 Minutes');
    t.equal(date.getHours(), 3, '3 AM');
    t.equal(date.getDate(), 28, 'on the 28th');

    options.currentDate = '2016-03-27 00:00:01';

    interval = CronExpression.parse('0 * 27 * *', options);
    t.ok(interval, 'Interval parsed');

    date = interval.next();
    t.equal(date.getMinutes(), 0, '0 Minutes');
    t.equal(date.getHours(), 1, '1 AM');
    t.equal(date.getDate(), 27, 'on the 27th');

    date = interval.next();
    t.equal(date.getMinutes(), 0, '0 Minutes');
    t.equal(date.getHours(), 2, '2 AM');
    t.equal(date.getDate(), 27, 'on the 27th');

    date = interval.next();
    t.equal(date.getMinutes(), 0, '0 Minutes');
    t.equal(date.getHours(), 4, '4 AM');
    t.equal(date.getDate(), 27, 'on the 27th');

    date = interval.next();
    t.equal(date.getMinutes(), 0, '0 Minutes');
    t.equal(date.getHours(), 5, '5 AM');
    t.equal(date.getDate(), 27, 'on the 27th');

    options.currentDate = '2016-03-27 00:00:01';
    options.endDate = '2016-03-27 03:00:01';

    interval = CronExpression.parse('0 * * * *', options);
    t.ok(interval, 'Interval parsed');

    date = interval.next();
    t.equal(date.getMinutes(), 0, '0 Minutes');
    t.equal(date.getHours(), 1, '1 AM');
    t.equal(date.getDate(), 27, 'on the 27th');

    date = interval.next();
    t.equal(date.getMinutes(), 0, '0 Minutes');
    t.equal(date.getHours(), 2, '2 AM');
    t.equal(date.getDate(), 27, 'on the 27th');

    date = interval.next();
    t.equal(date.getMinutes(), 0, '0 Minutes');
    t.equal(date.getHours(), 4, '4 AM');
    t.equal(date.getDate(), 27, 'on the 27th');

    // Out of the timespan range
    t.throws(function() {
        date = interval.next();
    });
  } catch (err) {
    t.ifError(err, 'Interval parse error');
  }

  t.end();
});

test('It works on DST end', function(t) {
  try {
    var options = {
      currentDate: '2016-10-30 02:00:01',
      tz: 'Europe/Athens'
    };

    var interval, date;

    interval = CronExpression.parse('0 * * * *', options);
    t.ok(interval, 'Interval parsed');

    date = interval.next();
    t.equal(date.getHours(), 3, '3 AM');
    t.equal(date.getDate(), 30, '30th');

    date = interval.next();
    t.equal(date.getHours(), 3, 'Due to DST end in Athens (4-->3)');
    t.equal(date.getDate(), 30, '30th');

    date = interval.next();
    t.equal(date.getHours(), 4, '4 AM');
    t.equal(date.getDate(), 30, '30th');

    interval = CronExpression.parse('0 3 * * *', options);
    t.ok(interval, 'Interval parsed');

    date = interval.next();
    t.equal(date.getHours(), 3, '3 AM');
    t.equal(date.getDate(), 30, '30th');

    date = interval.next();
    t.equal(date.getHours(), 3, '3 AM');
    t.equal(date.getDate(), 31, '31st');

    interval = CronExpression.parse('*/20 3 * * *', options);
    t.ok(interval, 'Interval parsed');

    date = interval.next();
    t.equal(date.getMinutes(), 0, '0');
    t.equal(date.getHours(), 3, '3 AM');
    t.equal(date.getDate(), 30, '30th');

    date = interval.next();
    t.equal(date.getMinutes(), 20, '20');
    t.equal(date.getHours(), 3, '3 AM');
    t.equal(date.getDate(), 30, '30th');

    date = interval.next();
    t.equal(date.getMinutes(), 40, '40');
    t.equal(date.getHours(), 3, '3 AM');
    t.equal(date.getDate(), 30, '30th');

    date = interval.next();
    t.equal(date.getMinutes(), 0, '0');
    t.equal(date.getHours(), 3, '3 AM');
    t.equal(date.getDate(), 31, '31st');

    options.currentDate = '2016-10-30 00:00:01';

    interval = CronExpression.parse('0 * 30 * *', options);
    t.ok(interval, 'Interval parsed');

    date = interval.next();
    t.equal(date.getHours(), 1, '1 AM');
    t.equal(date.getDate(), 30, '30th');

    date = interval.next();
    t.equal(date.getHours(), 2, '2 AM');
    t.equal(date.getDate(), 30, '30th');

    date = interval.next();
    t.equal(date.getHours(), 3, '3 AM');
    t.equal(date.getDate(), 30, '30th');

    date = interval.next();
    t.equal(date.getHours(), 3, '3 AM');
    t.equal(date.getDate(), 30, '30th');

    date = interval.next();
    t.equal(date.getHours(), 4, '4 AM');
    t.equal(date.getDate(), 30, '30th');

    options.currentDate = '2016-10-30 00:00:01';
    options.endDate = '2016-10-30 03:00:01';

    interval = CronExpression.parse('0 * * * *', options);
    t.ok(interval, 'Interval parsed');

    date = interval.next();
    t.equal(date.getHours(), 1, '1 AM');
    t.equal(date.getDate(), 30, '30th');

    date = interval.next();
    t.equal(date.getHours(), 2, '2 AM');
    t.equal(date.getDate(), 30, '30th');

    date = interval.next();
    t.equal(date.getHours(), 3, '3 AM');
    t.equal(date.getDate(), 30, '30th');

    // Out of the timespan range
    t.throws(function() {
        date = interval.next();
    });

    options.endDate = '2016-10-30 04:00:01';

    interval = CronExpression.parse('0 * * * *', options);
    t.ok(interval, 'Interval parsed');

    date = interval.next();
    t.equal(date.getHours(), 1, '1 AM');
    t.equal(date.getDate(), 30, '30th');

    date = interval.next();
    t.equal(date.getHours(), 2, '2 AM');
    t.equal(date.getDate(), 30, '30th');

    date = interval.next();
    t.equal(date.getHours(), 3, '3 AM');
    t.equal(date.getDate(), 30, '30th');

    date = interval.next();
    t.equal(date.getHours(), 3, '3 AM');
    t.equal(date.getDate(), 30, '30th');

    date = interval.next();
    t.equal(date.getHours(), 4, '4 AM');
    t.equal(date.getDate(), 30, '30th');

    // Out of the timespan range
    t.throws(function() {
        date = interval.next();
    });

    options = {
        currentDate : new Date('Sun Oct 29 2016 01:00:00 GMT+0200')
    }

    interval = CronExpression.parse('0 12 * * *', options);
    t.ok(interval, 'Interval parsed');

    date = interval.next();
    t.equal(date.getHours(), 12, '12');
    t.equal(date.getDate(), 29, '29th');
    date = interval.next();
    t.equal(date.getHours(), 12, '12');
    t.equal(date.getDate(), 30, '30th');
    date = interval.next();
    t.equal(date.getHours(), 12, '12');
    t.equal(date.getDate(), 31, '31st');

    options = {
        currentDate : new Date('Sun Oct 29 2016 02:59:00 GMT+0200')
    }

    interval = CronExpression.parse('0 12 * * *', options);
    t.ok(interval, 'Interval parsed');

    date = interval.next();
    t.equal(date.getHours(), 12, '12');
    t.equal(date.getDate(), 29, '29th');
    date = interval.next();
    t.equal(date.getHours(), 12, '12');
    t.equal(date.getDate(), 30, '30th');
    date = interval.next();
    t.equal(date.getHours(), 12, '12');
    t.equal(date.getDate(), 31, '31st');

    options = {
        currentDate : new Date('Sun Oct 29 2016 02:59:59 GMT+0200')
    }

    interval = CronExpression.parse('0 12 * * *', options);
    t.ok(interval, 'Interval parsed');

    date = interval.next();
    t.equal(date.getHours(), 12, '12');
    t.equal(date.getDate(), 29, '29th');
    date = interval.next();
    t.equal(date.getHours(), 12, '12');
    t.equal(date.getDate(), 30, '30th');
    date = interval.next();
    t.equal(date.getHours(), 12, '12');
    t.equal(date.getDate(), 31, '31st');

    options = {
        currentDate : new Date('Sun Oct 30 2016 01:00:00 GMT+0200')
    }

    interval = CronExpression.parse('0 12 * * *', options);
    t.ok(interval, 'Interval parsed');

    date = interval.next();
    t.equal(date.getHours(), 12, '12');
    t.equal(date.getDate(), 30, '30th');
    date = interval.next();
    t.equal(date.getHours(), 12, '12');
    t.equal(date.getDate(), 31, '31st');

    options = {
        currentDate : new Date('Sun Oct 30 2016 01:59:00 GMT+0200')
    }

    interval = CronExpression.parse('0 12 * * *', options);
    t.ok(interval, 'Interval parsed');

    date = interval.next();
    t.equal(date.getHours(), 12, '12');
    t.equal(date.getDate(), 30, '30th');
    date = interval.next();
    t.equal(date.getHours(), 12, '12');
    t.equal(date.getDate(), 31, '31st');

    options = {
        currentDate : new Date('Sun Oct 30 2016 01:59:59 GMT+0200')
    }

    interval = CronExpression.parse('0 12 * * *', options);
    t.ok(interval, 'Interval parsed');

    date = interval.next();
    t.equal(date.getHours(), 12, '12');
    t.equal(date.getDate(), 30, '30th');
    date = interval.next();
    t.equal(date.getHours(), 12, '12');
    t.equal(date.getDate(), 31, '31st');

    options = {
        currentDate : new Date('Sun Oct 30 2016 02:59:00 GMT+0200')
    }

    interval = CronExpression.parse('0 12 * * *', options);
    t.ok(interval, 'Interval parsed');

    date = interval.next();
    t.equal(date.getHours(), 12, '12');
    t.equal(date.getDate(), 30, '30th');
    date = interval.next();
    t.equal(date.getHours(), 12, '12');
    t.equal(date.getDate(), 31, '31st');
   } catch (err) {
    t.ifError(err, 'Interval parse error');
  }

  t.end();
});
