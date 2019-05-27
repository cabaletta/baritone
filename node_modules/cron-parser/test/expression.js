var util = require('util');
var test = require('tap').test;
var CronExpression = require('../lib/expression');
var CronDate = require('../lib/date');

test('empty expression test', function(t) {
  try {
    var interval = CronExpression.parse('');
    t.ok(interval, 'Interval parsed');

    var date = new CronDate();
    date.addMinute();

    var next = interval.next();

    t.ok(next, 'Found next scheduled interval');
    t.equal(next.getMinutes(), date.getMinutes(), 'Schedule matches');

    t.end();
  } catch (err) {
    t.ifError(err, 'Interval parse error');
  }
});

test('default expression test', function(t) {
  try {
    var interval = CronExpression.parse('* * * * *');
    t.ok(interval, 'Interval parsed');

    var date = new CronDate();
    date.addMinute();

    var next = interval.next();

    t.ok(next, 'Found next scheduled interval');
    t.equal(next.getMinutes(), date.getMinutes(), 'Schedule matches');

  } catch (err) {
    t.ifError(err, 'Interval parse error');
  }

  t.end();
});

test('default expression (tab separate) test', function(t) {
  try {
    var interval = CronExpression.parse('*	*	*	*	*');
    t.ok(interval, 'Interval parsed');

    var date = new CronDate();
    date.addMinute();

    var next = interval.next();

    t.ok(next, 'Found next scheduled interval');
    t.equal(next.getMinutes(), date.getMinutes(), 'Schedule matches');

  } catch (err) {
    t.ifError(err, 'Interval parse error');
  }

  t.end();
});

test('default expression (multi-space separated) test 1', function(t) {
  try {
    var interval = CronExpression.parse('* \t*\t\t  *\t   *  \t\t*');
    t.ok(interval, 'Interval parsed');

    var date = new CronDate();
    date.addMinute();

    var next = interval.next();

    t.ok(next, 'Found next scheduled interval');
    t.equal(next.getMinutes(), date.getMinutes(), 'Schedule matches');

  } catch (err) {
    t.ifError(err, 'Interval parse error');
  }

  t.end();
});


test('default expression (multi-space separated) test 1', function(t) {
  try {
    var interval = CronExpression.parse('* \t    *\t \t  *   *  \t \t  *');
    t.ok(interval, 'Interval parsed');

    var date = new CronDate();
    date.addMinute();

    var next = interval.next();

    t.ok(next, 'Found next scheduled interval');
    t.equal(next.getMinutes(), date.getMinutes(), 'Schedule matches');

  } catch (err) {
    t.ifError(err, 'Interval parse error');
  }

  t.end();
});

test('second value out of the range', function(t) {
  try {
    CronExpression.parse('61 * * * * *');
  } catch (err) {
    t.ok(err, 'Error expected');
    t.equal(err.message, 'Constraint error, got value 61 expected range 0-59');
  }

  t.end();
});

test('second value out of the range', function(t) {
  try {
    CronExpression.parse('-1 * * * * *');
  } catch (err) {
    t.ok(err, 'Error expected');
    t.equal(err.message, 'Constraint error, got value -1 expected range 0-59');
  }

  t.end();
});

test('minute value out of the range', function(t) {
  try {
    CronExpression.parse('* 32,72 * * * *');
  } catch (err) {
    t.ok(err, 'Error expected');
    t.equal(err.message, 'Constraint error, got value 72 expected range 0-59');
  }

  t.end();
});

test('hour value out of the range', function(t) {
  try {
    CronExpression.parse('* * 12-36 * * *');
  } catch (err) {
    t.ok(err, 'Error expected');
    t.equal(err.message, 'Constraint error, got range 12-36 expected range 0-23');
  }

  t.end();
});


test('day of the month value out of the range', function(t) {
  try {
    CronExpression.parse('* * * 10-15,40 * *');
  } catch (err) {
    t.ok(err, 'Error expected');
    t.equal(err.message, 'Constraint error, got value 40 expected range 1-31');
  }

  t.end();
});

test('month value out of the range', function(t) {
  try {
    CronExpression.parse('* * * * */10,12-13 *');
  } catch (err) {
    t.ok(err, 'Error expected');
    t.equal(err.message, 'Constraint error, got range 12-13 expected range 1-12');
  }

  t.end();
});

test('day of the week value out of the range', function(t) {
  try {
    CronExpression.parse('* * * * * 9');
  } catch (err) {
    t.ok(err, 'Error expected');
    t.equal(err.message, 'Constraint error, got value 9 expected range 0-7');
  }

  t.end();
});

test('incremental minutes expression test', function(t) {
  try {
    var interval = CronExpression.parse('*/3 * * * *');
    t.ok(interval, 'Interval parsed');

    var next = interval.next();

    t.ok(next, 'Found next scheduled interval');
    t.equal(next.getMinutes() % 3, 0, 'Schedule matches');
  } catch (err) {
    t.ifError(err, 'Interval parse error');
  }

  t.end();
});

test('fixed expression test', function(t) {
  try {
    var interval = CronExpression.parse('10 2 12 8 0');
    t.ok(interval, 'Interval parsed');

    var next = interval.next();

    t.ok(next, 'Found next scheduled interval');
    t.ok(next.getDay() === 0 || next.getDate() === 12, 'Day or day of Month matches');
    t.equal(next.getMonth(), 7, 'Month matches');
    t.equal(next.getHours(), 2, 'Hour matches');
    t.equal(next.getMinutes(), 10, 'Minute matches');
  } catch (err) {
    t.ifError(err, 'Interval parse error');
  }

  t.end();
});

test('invalid characters test - symbol', function(t) {
  try {
    CronExpression.parse('10 ! 12 8 0');
  } catch (err) {
    t.ok(err, 'Error expected');
    t.equal(err.message, 'Invalid characters, got value: !');
  }

  t.end();
});

test('invalid characters test - letter', function(t) {
  try {
    CronExpression.parse('10 x 12 8 0');
  } catch (err) {
    t.ok(err, 'Error expected');
    t.equal(err.message, 'Invalid characters, got value: x');
  }

  t.end();
});

test('invalid characters test - parentheses', function(t) {
  try {
    CronExpression.parse('10 ) 12 8 0');
  } catch (err) {
    t.ok(err, 'Error expected');
    t.equal(err.message, 'Invalid characters, got value: )');
  }

  t.end();
});

test('interval with invalid characters test', function(t) {
  try {
    CronExpression.parse('10 */A 12 8 0');
  } catch (err) {
    t.ok(err, 'Error expected');
    t.equal(err.message, 'Invalid characters, got value: */A');
  }

  t.end();
});

test('range with invalid characters test', function(t) {
  try {
    CronExpression.parse('10 0-z 12 8 0');
  } catch (err) {
    t.ok(err, 'Error expected');
    t.equal(err.message, 'Invalid characters, got value: 0-z');
  }

  t.end();
});

test('group with invalid characters test', function(t) {
  try {
    CronExpression.parse('10 0,1,z 12 8 0');
  } catch (err) {
    t.ok(err, 'Error expected');
    t.equal(err.message, 'Invalid characters, got value: 0,1,z');
  }

  t.end();
});

test('range test with iterator', function(t) {
  try {
    var interval = CronExpression.parse('10-30 2 12 8 0');
    t.ok(interval, 'Interval parsed');

    var intervals = interval.iterate(20);
    t.ok(intervals, 'Found intervals');

    for (var i = 0, c = intervals.length; i < c; i++) {
      var next = intervals[i];

      t.ok(next, 'Found next scheduled interval');
      t.ok(next.getDay() === 0 || next.getDate() === 12, 'Day or day of month matches');
      t.equal(next.getMonth(), 7, 'Month matches');
      t.equal(next.getHours(), 2, 'Hour matches');
      t.equal(next.getMinutes(), 10 + i, 'Minute matches');
    }
  } catch (err) {
    t.ifError(err, 'Interval parse error');
  }

  t.end();
});

test('incremental range test with iterator', function(t) {
  try {
    var interval = CronExpression.parse('10-30/2 2 12 8 0');
    t.ok(interval, 'Interval parsed');

    var intervals = interval.iterate(10);
    t.ok(intervals, 'Found intervals');

    for (var i = 0, c = intervals.length; i < c; i++) {
      var next = intervals[i];

      t.ok(next, 'Found next scheduled interval');
      t.ok(next.getDay() === 0 || next.getDate() === 12, 'Day or day of month matches');
      t.equal(next.getMonth(), 7, 'Month matches');
      t.equal(next.getHours(), 2, 'Hour matches');
      t.equal(next.getMinutes(), 10 + (i * 2), 'Minute matches');
    }
  } catch (err) {
    t.ifError(err, 'Interval parse error');
  }

  t.end();
});

test('predefined expression', function(t) {
  try {
    var interval = CronExpression.parse('@yearly');
    t.ok(interval, 'Interval parsed');

    var date = new CronDate();
    date.addYear();

    var next = interval.next();
    t.ok(next, 'Found next scheduled interval');

    t.equal(next.getFullYear(), date.getFullYear(), 'Year matches');
  } catch (err) {
    t.ifError(err, 'Interval parse error');
  }

  t.end();
});

test('expression limited with start and end date', function(t) {
  try {
    var options = {
      currentDate: new CronDate('Wed, 26 Dec 2012 14:38:53'),
      startDate: new CronDate('Wed, 26 Dec 2012 12:40:00'),
      endDate: new CronDate('Wed, 26 Dec 2012 16:40:00')
    };

    var interval = CronExpression.parse('*/20 * * * *', options);
    t.ok(interval, 'Interval parsed');

    var dates = interval.iterate(10);
    t.equal(dates.length, 7, 'Dates count matches for positive iteration');

    interval.reset();

    var dates = interval.iterate(-10);
    t.equal(dates.length, 6, 'Dates count matches for negative iteration');

    interval.reset();

    // Forward iteration
    var next = interval.next();
    t.equal(next.getHours(), 14, 'Hour matches');
    t.equal(next.getMinutes(), 40, 'Minute matches');

    next = interval.next();
    t.equal(next.getHours(), 15, 'Hour matches');
    t.equal(next.getMinutes(), 0, 'Minute matches');

    next = interval.next();
    t.equal(next.getHours(), 15, 'Hour matches');
    t.equal(next.getMinutes(), 20, 'Minute matches');

    next = interval.next();
    t.equal(next.getHours(), 15, 'Hour matches');
    t.equal(next.getMinutes(), 40, 'Minute matches');

    next = interval.next();
    t.equal(next.getHours(), 16, 'Hour matches');
    t.equal(next.getMinutes(), 0, 'Minute matches');

    next = interval.next();
    t.equal(next.getHours(), 16, 'Hour matches');
    t.equal(next.getMinutes(), 20, 'Minute matches');

    next = interval.next();
    t.equal(next.getHours(), 16, 'Hour matches');
    t.equal(next.getMinutes(), 40, 'Minute matches');

    try {
      interval.next();
      t.ok(false, 'Should fail');
    } catch (e) {
      t.ok(true, 'Failed as expected');
    }

    // TODO: Currently, encountering an out-of-range failure (as above)
    // still results in a new interval being set on the object.
    // Until/unless this is fixed, the below test will fail.
    // next = interval.prev();
    // t.equal(next.getHours(), 16, 'Hour matches');
    // t.equal(next.getMinutes(), 20, 'Minute matches');

    interval.reset();

    // Backward iteration
    var prev = interval.prev();
    t.equal(prev.getHours(), 14, 'Hour matches');
    t.equal(prev.getMinutes(), 20, 'Minute matches');

    prev = interval.prev();
    t.equal(prev.getHours(), 14, 'Hour matches');
    t.equal(prev.getMinutes(), 0, 'Minute matches');

    prev = interval.prev();
    t.equal(prev.getHours(), 13, 'Hour matches');
    t.equal(prev.getMinutes(), 40, 'Minute matches');

    prev = interval.prev();
    t.equal(prev.getHours(), 13, 'Hour matches');
    t.equal(prev.getMinutes(), 20, 'Minute matches');

    prev = interval.prev();
    t.equal(prev.getHours(), 13, 'Hour matches');
    t.equal(prev.getMinutes(), 0, 'Minute matches');

    prev = interval.prev();
    t.equal(prev.getHours(), 12, 'Hour matches');
    t.equal(prev.getMinutes(), 40, 'Minute matches');

    try {
      interval.prev();
      t.ok(false, 'Should fail');
    } catch (e) {
      t.ok(true, 'Failed as expected');
    }
  } catch (err) {
    t.ifError(err, 'Interval parse error');
  }

  t.end();
});

test('parse expression as UTC', function(t) {
  try {
    var options = {
      utc: true
    };

    var interval = CronExpression.parse('0 0 10 * * *', options);

    var date = interval.next();
    t.equal(date.getUTCHours(), 10, 'Correct UTC hour value');
    t.equal(date.getHours(), 10, 'Correct UTC hour value');

    interval = CronExpression.parse('0 */5 * * * *', options);

    var date = interval.next(), now = new Date();
    now.setMinutes(now.getMinutes() + 5 - (now.getMinutes() % 5));

    t.equal(date.getHours(), now.getUTCHours(), 'Correct local time for 5 minute interval');

  } catch (err) {
    t.ifError(err, 'UTC parse error');
  }

  t.end();
});

test('expression using days of week strings', function(t) {
  try {
    var interval = CronExpression.parse('15 10 * * MON-TUE');
    t.ok(interval, 'Interval parsed');

    var intervals = interval.iterate(8);
    t.ok(intervals, 'Found intervals');

    for (var i = 0, c = intervals.length; i < c; i++) {
      var next = intervals[i];
      var day = next.getDay();


      t.ok(next, 'Found next scheduled interval');
      t.ok(day == 1 || day == 2, 'Day matches')
      t.equal(next.getHours(), 10, 'Hour matches');
      t.equal(next.getMinutes(), 15, 'Minute matches');
    }
  } catch (err) {
    t.ifError(err, 'Interval parse error');
  }

  t.end();
});

test('expression using mixed days of week strings', function(t) {
  try {
    var options = {
      currentDate: new CronDate('Wed, 26 Dec 2012 14:38:53')
    };

    var interval = CronExpression.parse('15 10 * jAn-FeB mOn-tUE', options);
    t.ok(interval, 'Interval parsed');

    var intervals = interval.iterate(8);
    t.ok(intervals, 'Found intervals');

    for (var i = 0, c = intervals.length; i < c; i++) {
      var next = intervals[i];
      var day = next.getDay();
      var month = next.getMonth();

      t.ok(next, 'Found next scheduled interval');
      t.ok(month == 0 || month == 2, 'Month Matches');
      t.ok(day == 1 || day == 2, 'Day matches');
      t.equal(next.getHours(), 10, 'Hour matches');
      t.equal(next.getMinutes(), 15, 'Minute matches');
    }
  } catch (err) {
    t.ifError(err, 'Interval parse error');
  }

  t.end();
});

test('expression using non-standard second field (wildcard)', function(t) {
  try {
    var options = {
      currentDate: new CronDate('Wed, 26 Dec 2012 14:38:00'),
      endDate: new CronDate('Wed, 26 Dec 2012 15:40:00')
    };

    var interval = CronExpression.parse('* * * * * *', options);
    t.ok(interval, 'Interval parsed');

    var intervals = interval.iterate(10);
    t.ok(intervals, 'Found intervals');

    for (var i = 0, c = intervals.length; i < c; i++) {
      var next = intervals[i];
      t.ok(next, 'Found next scheduled interval');
      t.equal(next.getSeconds(), i + 1, 'Second matches');
    }
  } catch (err) {
    t.ifError(err, 'Interval parse error');
  }

  t.end();
});

test('expression using non-standard second field (step)', function(t) {
  try {
    var options = {
      currentDate: new CronDate('Wed, 26 Dec 2012 14:38:00'),
      endDate: new CronDate('Wed, 26 Dec 2012 15:40:00')
    };

    var interval = CronExpression.parse('*/20 * * * * *', options);
    t.ok(interval, 'Interval parsed');

    var intervals = interval.iterate(3);
    t.ok(intervals, 'Found intervals');

    t.equal(intervals[0].getSeconds(), 20, 'Second matches');
    t.equal(intervals[1].getSeconds(), 40, 'Second matches');
    t.equal(intervals[2].getSeconds(), 0, 'Second matches');
  } catch (err) {
    t.ifError(err, 'Interval parse error');
  }

  t.end();
});

test('expression using non-standard second field (range)', function(t) {
  try {
    var options = {
      currentDate: new CronDate('Wed, 26 Dec 2012 14:38:00'),
      endDate: new CronDate('Wed, 26 Dec 2012 15:40:00')
    };

    var interval = CronExpression.parse('20-40/10 * * * * *', options);
    t.ok(interval, 'Interval parsed');

    var intervals = interval.iterate(3);
    t.ok(intervals, 'Found intervals');

    for (var i = 0, c = intervals.length; i < c; i++) {
      var next = intervals[i];

      t.ok(next, 'Found next scheduled interval');
      t.equal(next.getSeconds(), 20 + (i * 10), 'Second matches');
    }
  } catch (err) {
    t.ifError(err, 'Interval parse error');
  }

  t.end();
});

test('day of month and week are both set', function(t) {
  try {
    var interval = CronExpression.parse('10 2 12 8 0');
    t.ok(interval, 'Interval parsed');

    var next = interval.next();

    t.ok(next, 'Found next scheduled interval');
    t.ok(next.getDay() === 0 || next.getDate() === 12, 'Day or day of month matches');
    t.equal(next.getMonth(), 7, 'Month matches');

    next = interval.next();

    t.ok(next, 'Found next scheduled interval');
    t.ok(next.getDay() === 0 || next.getDate() === 12, 'Day or day of month matches');
    t.equal(next.getMonth(), 7, 'Month matches');

    next = interval.next();

    t.ok(next, 'Found next scheduled interval');
    t.ok(next.getDay() === 0 || next.getDate() === 12, 'Day or day of month matches');
    t.equal(next.getMonth(), 7, 'Month matches');

    next = interval.next();

    t.ok(next, 'Found next scheduled interval');
    t.ok(next.getDay() === 0 || next.getDate() === 12, 'Day or day of month matches');
    t.equal(next.getMonth(), 7, 'Month matches');
  } catch (err) {
    t.ifError(err, 'Interval parse error');
  }

  t.end();
});

test('day of month is unspecified', function(t) {
  try {
    var interval = CronExpression.parse('10 2 ? * 3');

    t.ok(interval, 'Interval parsed');
    
    var next = interval.next();
    t.ok(next, 'Found next scheduled interal');
    t.ok(next.getDay() === 3, 'day of week matches');

    next = interval.next();
    t.ok(next, 'Found next scheduled interal');
    t.ok(next.getDay() === 3, 'day of week matches');

    next = interval.next();
    t.ok(next, 'Found next scheduled interal');
    t.ok(next.getDay() === 3, 'day of week matches');

    next = interval.next();
    t.ok(next, 'Found next scheduled interal');
    t.ok(next.getDay() === 3, 'day of week matches');

  } catch (err) {
    t.ifError(err, 'Interval parse error');
  }

  t.end();
});

test('day of week is unspecified', function(t) {
  try {
    var interval = CronExpression.parse('10 2 3,6 * ?');

    t.ok(interval, 'Interval parsed');

    var next = interval.next();
    t.ok(next, 'Found next scheduled interal');
    t.ok(next.getDate() === 3 || next.getDate() === 6, 'date matches');
    var prevDate = next.getDate();

    next = interval.next();
    t.ok(next, 'Found next scheduled interal');
    t.ok((next.getDate() === 3 || next.getDate() === 6) &&
      next.getDate() !== prevDate, 'date matches and is not previous date');
    prevDate = next.getDate();

    next = interval.next();
    t.ok(next, 'Found next scheduled interal');
    t.ok((next.getDate() === 3 || next.getDate() === 6) &&
      next.getDate() !== prevDate, 'date matches and is not previous date');
    prevDate = next.getDate();

    next = interval.next();
    t.ok(next, 'Found next scheduled interal');
    t.ok((next.getDate() === 3 || next.getDate() === 6) &&
      next.getDate() !== prevDate, 'date matches and is not previous date');
  } catch (err) {
    t.ifError(err, 'Interval parse error');
  }

  t.end();
});

test('Summertime bug test', function(t) {
  try {
    var month = new CronDate().getMonth() + 1;
    var interval = CronExpression.parse('0 0 0 1 '+month+' *');
    t.ok(interval, 'Interval parsed');

    var next = interval.next();

    // Before fix the bug it was getting a timeout error if you are
    // in a timezone that changes the DST to ST in the hour 00:00h.
    t.ok(next, 'Found next scheduled interval');
  } catch (err) {
    t.ifError(err, 'Interval parse error');
  }

  t.end();
});

test('day of month and week are both set and dow is 7', function(t) {
  try {
    var interval = CronExpression.parse('10 2 12 8 7');
    t.ok(interval, 'Interval parsed');

    var next = interval.next();

    t.ok(next, 'Found next scheduled interval');
    t.ok(next.getDay() === 0 || next.getDate() === 12, 'Day or day of month matches');
    t.equal(next.getMonth(), 7, 'Month matches');

    next = interval.next();

    t.ok(next, 'Found next scheduled interval');
    t.ok(next.getDay() === 0 || next.getDate() === 12, 'Day or day of month matches');
    t.equal(next.getMonth(), 7, 'Month matches');

    next = interval.next();

    t.ok(next, 'Found next scheduled interval');
    t.ok(next.getDay() === 0 || next.getDate() === 12, 'Day or day of month matches');
    t.equal(next.getMonth(), 7, 'Month matches');

    next = interval.next();

    t.ok(next, 'Found next scheduled interval');
    t.ok(next.getDay() === 0 || next.getDate() === 12, 'Day or day of month matches');
    t.equal(next.getMonth(), 7, 'Month matches');
  } catch (err) {
    t.ifError(err, 'Interval parse error');
  }

  t.end();
});

test('day of month and week are both set and dow is 6,0', function(t) {
  try {
    var interval = CronExpression.parse('10 2 12 8 6,0');
    t.ok(interval, 'Interval parsed');

    var next = interval.next();

    t.ok(next, 'Found next scheduled interval');
    t.ok(next.getDay() === 6 || next.getDate() === 12, 'Day or day of month matches');
    t.equal(next.getMonth(), 7, 'Month matches');

    next = interval.next();

    t.ok(next, 'Found next scheduled interval');
    t.ok(next.getDay() === 0 || next.getDate() === 12, 'Day or day of month matches');
    t.equal(next.getMonth(), 7, 'Month matches');

    next = interval.next();

    t.ok(next, 'Found next scheduled interval');
    t.ok(next.getDay() === 6 || next.getDate() === 12, 'Day or day of month matches');
    t.equal(next.getMonth(), 7, 'Month matches');

    next = interval.next();

    t.ok(next, 'Found next scheduled interval');
    t.ok(next.getDay() === 0 || next.getDate() === 12, 'Day or day of month matches');
    t.equal(next.getMonth(), 7, 'Month matches');
  } catch (err) {
    t.ifError(err, 'Interval parse error');
  }

  t.end();
});


test('day of month and week are both set and dow is 6-7', function(t) {
  try {
    var interval = CronExpression.parse('10 2 12 8 6-7');
    t.ok(interval, 'Interval parsed');

    var next = interval.next();

    t.ok(next, 'Found next scheduled interval');
    t.ok(next.getDay() === 6 || next.getDate() === 12, 'Day or day of month matches');
    t.equal(next.getMonth(), 7, 'Month matches');

    next = interval.next();

    t.ok(next, 'Found next scheduled interval');
    t.ok(next.getDay() === 6 || next.getDate() === 12, 'Day or day of month matches');
    t.equal(next.getMonth(), 7, 'Month matches');

    next = interval.next();

    t.ok(next, 'Found next scheduled interval');
    t.ok(next.getDay() === 6 || next.getDate() === 12, 'Day or day of month matches');
    t.equal(next.getMonth(), 7, 'Month matches');

    // next = interval.next();

    t.ok(next, 'Found next scheduled interval');
    t.ok(next.getDay() === 6 || next.getDate() === 12, 'Day or day of month matches');
    t.equal(next.getMonth(), 7, 'Month matches');
  } catch (err) {
    t.ifError(err, 'Interval parse error');
  }

  t.end();
});

test('day and date in week should matches', function(t){
  try {
    var interval = CronExpression.parse('0 1 1 1 * 1');
    t.ok(interval, 'Interval parsed');

    var next = interval.next();

    t.ok(next, 'Found next scheduled interval');
    t.equal(next.getHours(), 1, 'Hours matches');
    t.ok(next.getDay() === 1 || next.getDate() === 1, 'Day or day of month matches');

    next = interval.next();

    t.ok(next, 'Found next scheduled interval');
    t.equal(next.getHours(), 1, 'Hours matches');
    t.ok(next.getDay() === 1 || next.getDate() === 1, 'Day or day of month matches');

    next = interval.next();

    t.ok(next, 'Found next scheduled interval');
    t.equal(next.getHours(), 1, 'Hours matches');
    t.ok(next.getDay() === 1 || next.getDate() === 1, 'Day or day of month matches');

  } catch (err) {
    t.ifError(err, 'Interval parse error');
  }

  t.end();
});

test('day of month value can\'t be larger than days in month maximum value if it\'s defined explicitly', function(t) {
  try {
    var interval = CronExpression.parse('0 4 31 4 *');
    t.ok(interval, 'Interval parsed');

    try {
      interval.next();
      t.ok(false, 'Should fail');
    } catch (e) {
      t.ok(true, 'Failed as expected');
    }
  } catch (err) {
    t.ifError(err, 'Interval parse error');
  }

  t.end();
});

test('valid ES6 iterator should be returned if iterator options is set to true', function(t) {
  try {
    var options = {
      currentDate: new CronDate('Wed, 26 Dec 2012 14:38:53'),
      endDate: new CronDate('Wed, 26 Dec 2012 15:40:00'),
      iterator: true
    };

    var val = null;
    var interval = CronExpression.parse('*/25 * * * *', options);
    t.ok(interval, 'Interval parsed');

    val = interval.next();
    t.ok(val, 'Next iteration resolved');
    t.ok(val.value, 'Iterator value is set');
    t.notOk(val.done, 'Iterator is not finished');

    val = interval.next();
    t.ok(val, 'Next iteration resolved');
    t.ok(val.value, 'Iterator value is set');
    t.notOk(val.done, 'Iterator is not finished');

    val = interval.next();
    t.ok(val, 'Next iteration resolved');
    t.ok(val.value, 'Iterator value is set');
    t.ok(val.done, 'Iterator is finished');
  } catch (err) {
    t.ifError(err, 'Interval parse error');
  }

  t.end();
});

test('Must not parse an expression which has repeat 0 times', function(t) {
  try {
    var expression = CronExpression.parse('0 */0 * * *');
    var val = expression.next();
  } catch (err) {
    t.ok(err, 'Error expected');
    t.equal(err.message, 'Constraint error, cannot repeat at every 0 time.');
  }

  t.end();
});

test('Must not parse an expression which has repeat negative number times', function(t) {
  try {
    var expression = CronExpression.parse('0 */-5 * * *');
    var val = expression.next();
  } catch (err) {
    t.ok(err, 'Error expected');
    t.equal(err.message, 'Constraint error, cannot repeat at every -5 time.');
  }

  t.end();
});

test('dow 6,7 6,0 0,6 7,6 should be equivalent', function(t) {
  try {
    var options = {
      currentDate: new CronDate('Wed, 26 Dec 2012 14:38:53'),
    };

    var expressions = [
      '30 16 * * 6,7',
      '30 16 * * 6,0',
      '30 16 * * 0,6',
      '30 16 * * 7,6'
    ];

    expressions.forEach(function(expression) {
      var interval = CronExpression.parse(expression, options);
      t.ok(interval, 'Interval parsed');

      var val = interval.next();
      t.equal(val.getDay(), 6, 'Day matches');

      val = interval.next();
      t.equal(val.getDay(), 0, 'Day matches');

      val = interval.next();
      t.equal(val.getDay(), 6, 'Day matches');
    });
  } catch (err) {
    t.ifError(err, 'Interval parse error');
  }

  t.end();
});

test('hour 0 9,11,1 * * * and 0 1,9,11 * * * should be equivalent', function(t) {
  try {
    var options = {
      currentDate: new CronDate('Wed, 26 Dec 2012 00:00:00'),
    };

    var expressions = [
      '0 9,11,1 * * *',
      '0 1,9,11 * * *'
    ];

    expressions.forEach(function(expression) {
      var interval = CronExpression.parse(expression, options);
      t.ok(interval, 'Interval parsed');

      var val = interval.next();
      t.equal(val.getHours(), 1, 'Hour matches');

      val = interval.next();
      t.equal(val.getHours(), 9, 'Hour matches');

      val = interval.next();
      t.equal(val.getHours(), 11, 'Hour matches');

      val = interval.next();
      t.equal(val.getHours(), 1, 'Hour matches');

      val = interval.next();
      t.equal(val.getHours(), 9, 'Hour matches');

      val = interval.next();
      t.equal(val.getHours(), 11, 'Hour matches');
    });
  } catch (err) {
    t.ifError(err, 'Interval parse error');
  }

  t.end();
});
