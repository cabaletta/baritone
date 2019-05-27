'use strict';

var moment = require('moment-timezone');

CronDate.prototype.addYear = function() {
  this._date.add(1, 'year');
};

CronDate.prototype.addMonth = function() {
  this._date.add(1, 'month').startOf('month');
};

CronDate.prototype.addDay = function() {
  this._date.add(1, 'day').startOf('day');
};

CronDate.prototype.addHour = function() {
  var prev = this.getTime();
  this._date.add(1, 'hour').startOf('hour');
  if (this.getTime() <= prev) {
    this._date.add(1, 'hour');
  }
};

CronDate.prototype.addMinute = function() {
  var prev = this.getTime();
  this._date.add(1, 'minute').startOf('minute');
  if (this.getTime() < prev) {
    this._date.add(1, 'hour');
  }
};

CronDate.prototype.addSecond = function() {
  var prev = this.getTime();
  this._date.add(1, 'second').startOf('second');
  if (this.getTime() < prev) {
    this._date.add(1, 'hour');
  }
};

CronDate.prototype.subtractYear = function() {
  this._date.subtract(1, 'year');
};

CronDate.prototype.subtractMonth = function() {
  this._date.subtract(1, 'month').endOf('month');
};

CronDate.prototype.subtractDay = function() {
  this._date.subtract(1, 'day').endOf('day');
};

CronDate.prototype.subtractHour = function() {
  var prev = this.getTime();
  this._date.subtract(1, 'hour').endOf('hour');
  if (this.getTime() >= prev) {
    this._date.subtract(1, 'hour');
  }
};

CronDate.prototype.subtractMinute = function() {
  var prev = this.getTime();
  this._date.subtract(1, 'minute').endOf('minute');
  if (this.getTime() > prev) {
    this._date.subtract(1, 'hour');
  }
};

CronDate.prototype.subtractSecond = function() {
  var prev = this.getTime();
  this._date.subtract(1, 'second').startOf('second');
  if (this.getTime() > prev) {
    this._date.subtract(1, 'hour');
  }
};

CronDate.prototype.getDate = function() {
  return this._date.date();
};

CronDate.prototype.getFullYear = function() {
  return this._date.year();
};

CronDate.prototype.getDay = function() {
  return this._date.day();
};

CronDate.prototype.getMonth = function() {
  return this._date.month();
};

CronDate.prototype.getHours = function() {
  return this._date.hours();
};

CronDate.prototype.getMinutes = function() {
  return this._date.minute();
};

CronDate.prototype.getSeconds = function() {
  return this._date.second();
};

CronDate.prototype.getMilliseconds = function() {
  return this._date.millisecond();
};

CronDate.prototype.getTime = function() {
  return this._date.valueOf();
};

CronDate.prototype.getUTCDate = function() {
  return this._getUTC().date();
};

CronDate.prototype.getUTCFullYear = function() {
  return this._getUTC().year();
};

CronDate.prototype.getUTCDay = function() {
  return this._getUTC().day();
};

CronDate.prototype.getUTCMonth = function() {
  return this._getUTC().month();
};

CronDate.prototype.getUTCHours = function() {
  return this._getUTC().hours();
};

CronDate.prototype.getUTCMinutes = function() {
  return this._getUTC().minute();
};

CronDate.prototype.getUTCSeconds = function() {
  return this._getUTC().second();
};

CronDate.prototype.toISOString = function() {
  return this._date.toISOString();
};

CronDate.prototype.toJSON = function() {
  return this._date.toJSON();
};

CronDate.prototype.setDate = function(d) {
  return this._date.date(d);
};

CronDate.prototype.setFullYear = function(y) {
  return this._date.year(y);
};

CronDate.prototype.setDay = function(d) {
  return this._date.day(d);
};

CronDate.prototype.setMonth = function(m) {
  return this._date.month(m);
};

CronDate.prototype.setHours = function(h) {
  return this._date.hour(h);
};

CronDate.prototype.setMinutes = function(m) {
  return this._date.minute(m);
};

CronDate.prototype.setSeconds = function(s) {
  return this._date.second(s);
};

CronDate.prototype.setMilliseconds = function(s) {
  return this._date.millisecond(s);
};

CronDate.prototype.getTime = function() {
  return this._date.valueOf();
};

CronDate.prototype._getUTC = function() {
  return moment.utc(this._date);
};

CronDate.prototype.toString = function() {
  return this._date.toString();
};

CronDate.prototype.toDate = function() {
  return this._date.toDate();
};

function CronDate (timestamp, tz) {
  if (timestamp instanceof CronDate) {
    timestamp = timestamp._date;
  }

  if (!tz) {
    this._date = moment(timestamp);
  } else {
    this._date = moment.tz(timestamp, tz);
  }
}

module.exports = CronDate;
