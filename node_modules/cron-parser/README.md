cron-parser
================

[![Build Status](https://travis-ci.org/harrisiirak/cron-parser.svg?branch=master)](https://travis-ci.org/harrisiirak/cron-parser)
[![NPM version](https://badge.fury.io/js/cron-parser.png)](http://badge.fury.io/js/cron-parser)

Node.js library for parsing crontab instructions. It includes support for timezones and DST transitions.

Setup
========
```bash
npm install cron-parser
```

Supported format
========

```
*    *    *    *    *    *
┬    ┬    ┬    ┬    ┬    ┬
│    │    │    │    │    |
│    │    │    │    │    └ day of week (0 - 7) (0 or 7 is Sun)
│    │    │    │    └───── month (1 - 12)
│    │    │    └────────── day of month (1 - 31)
│    │    └─────────────── hour (0 - 23)
│    └──────────────────── minute (0 - 59)
└───────────────────────── second (0 - 59, optional)
```

Supports mixed use of ranges and range increments (L, W and # characters are not supported currently). See tests for examples.

Usage
========

Simple expression.

```javascript
var parser = require('cron-parser');

try {
  var interval = parser.parseExpression('*/2 * * * *');

  console.log('Date: ', interval.next().toString()); // Sat Dec 29 2012 00:42:00 GMT+0200 (EET)
  console.log('Date: ', interval.next().toString()); // Sat Dec 29 2012 00:44:00 GMT+0200 (EET)

  console.log('Date: ', interval.prev().toString()); // Sat Dec 29 2012 00:42:00 GMT+0200 (EET)
  console.log('Date: ', interval.prev().toString()); // Sat Dec 29 2012 00:40:00 GMT+0200 (EET)
} catch (err) {
  console.log('Error: ' + err.message);
}

```

Iteration with limited timespan. Also returns ES6 compatible iterator (when iterator flag is set to true).

```javascript
var parser = require('cron-parser');

var options = {
  currentDate: new Date('Wed, 26 Dec 2012 12:38:53 UTC'),
  endDate: new Date('Wed, 26 Dec 2012 14:40:00 UTC'),
  iterator: true
};

try {
  var interval = parser.parseExpression('*/22 * * * *', options);

  while (true) {
    try {
      var obj = interval.next();
      console.log('value:', obj.value.toString(), 'done:', obj.done);
    } catch (e) {
      break;
    }
  }

  // value: Wed Dec 26 2012 14:44:00 GMT+0200 (EET) done: false
  // value: Wed Dec 26 2012 15:00:00 GMT+0200 (EET) done: false
  // value: Wed Dec 26 2012 15:22:00 GMT+0200 (EET) done: false
  // value: Wed Dec 26 2012 15:44:00 GMT+0200 (EET) done: false
  // value: Wed Dec 26 2012 16:00:00 GMT+0200 (EET) done: false
  // value: Wed Dec 26 2012 16:22:00 GMT+0200 (EET) done: true
} catch (err) {
  console.log('Error: ' + err.message);
}

```

Timezone support

```javascript
var parser = require('cron-parser');

var options = {
  currentDate: '2016-03-27 00:00:01',
  tz: 'Europe/Athens'
};

try {
  var interval = parser.parseExpression('0 * * * *', options);

  console.log('Date: ', interval.next().toString()); // Date:  Sun Mar 27 2016 01:00:00 GMT+0200
  console.log('Date: ', interval.next().toString()); // Date:  Sun Mar 27 2016 02:00:00 GMT+0200
  console.log('Date: ', interval.next().toString()); // Date:  Sun Mar 27 2016 04:00:00 GMT+0300 (Notice DST transition)
} catch (err) {
  console.log('Error: ' + err.message);
}
```

Options
========

* *currentDate* - Start date of the iteration
* *endDate* - End date of the iteration

`currentDate` and `endDate` accept `string`, `integer` and `Date` as input.

In case of using `string` as input, not every string format accepted
by the `Date` constructor will work correctly. The supported formats are: [`ISO8601`](http://momentjs.com/docs/#/parsing/string/) and the older
[`ASP.NET JSON Date`](http://momentjs.com/docs/#/parsing/asp-net-json-date/) format. The reason being that those are the formats accepted by the
[`moment`](http://momentjs.com) library which is being used to handle dates.

Using `Date` as an input can be problematic specially when using the `tz` option. The issue being that, when creating a new `Date` object without
any timezone information, it will be created in the timezone of the system that is running the code. This (most of times) won't be what the user
will be expecting. Using one of the supported `string` formats will solve the issue(see timezone example).

* *iterator* - Return ES6 compatible iterator object 
* *utc* - Enable UTC
* *tz* - Timezone string. It won't be used in case `utc` is enabled
