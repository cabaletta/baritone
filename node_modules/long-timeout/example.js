var lt = require('./')

/*
  Timeouts
 */
lt.setTimeout(function() {
  console.log('in a long time')
}, Number.MAX_VALUE)

lt.setTimeout(function() {
  console.log('2 seconds')
}, 2000)

/*
  Intervals
 */
lt.setInterval(function() {
  console.log('long interval')
}, Number.MAX_VALUE)

lt.setInterval(function() {
  console.log("2 second interval")
}, 2000)
