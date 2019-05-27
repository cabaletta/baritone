assert = require('assert')

describe ('pathSetup', function () {

  it ('should load the library', function () {

    require('../pathSetup')()
    var status = require('sub/lib/testLib')
    assert(status, 'OK')

  });

});