'use strict';

var numberIsNaN = require('../');
var test = require('tape');

test('as a function', function (t) {
	require('./tests')(numberIsNaN, t);

	t.end();
});
