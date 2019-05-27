var needle  = require('../'),
    sinon   = require('sinon'),
    should  = require('should'),
    http    = require('http'),
    Emitter = require('events').EventEmitter,
    helpers = require('./helpers');

var get_catch = function(url, opts) {
  var err;
  try {
    needle.get(url, opts);
  } catch(e) {
    err = e;
  }
  return err;
}

describe('errors', function(){

  describe('null URL', function(){

    it('throws', function(){
      var ex = get_catch(); // null
      should.exist(ex);
      ex.message.should.include('Cannot call method');
    })

  })

/*

  describe('invalid protocol', function(){

    var url = 'foo://www.google.com/what'

    it('throws', function(){
      var ex = get_catch(url);
      should.exist(ex);
    })

  })

  describe('invalid host', function(){

    var url = 'http://s1\\\2.com/'

    it('throws', function(){
      var ex = get_catch(url);
      should.exist(ex);
    })

  })

  describe('invalid path', function(){

    var url = 'http://www.google.com\\\/x\\\    /x2.com/'

    it('throws', function(){
      var ex = get_catch(url);
      should.exist(ex);
    })

  })

*/

  describe('when host does not exist', function(){

    var url = 'http://unexistinghost/foo';

    it('does not throw', function(){
      var ex = get_catch(url);
      should.not.exist(ex);
    })

    it('callbacks an error', function(done){
      needle.get(url, function(err){
        err.should.an.instanceOf(Error);
        done();
      })
    })

    it('error should be ENOTFOUND', function(done){
      needle.get(url, function(err){
        err.code.should.match(/ENOTFOUND|EADDRINFO/)
        done();
      })
    })

    it('does not callback a response', function(done){
      needle.get(url, function(err, resp){
        should.not.exist(resp);
        done();
      })
    })

  })

  describe('when request timeouts', function(){

    var server,
        url = 'http://localhost:3333/foo';

    before(function(){
      server = helpers.server({ port: 3333, wait: 1000 });
    })

    after(function(){
      server.close();
    })

    it('aborts the request', function(done){

      var time = new Date();

      needle.get(url, { timeout: 200 }, function(err){
        var timediff = (new Date() - time);
        timediff.should.be.within(200, 250);
        done();
      })

    })

    it('callbacks an error', function(done){
      needle.get(url, { timeout: 200 }, function(err){
        err.should.an.instanceOf(Error);
        done();
      })
    })

    it('error should be ECONNRESET', function(done){
      needle.get(url, { timeout: 200 }, function(err){
        err.code.should.equal('ECONNRESET')
        done();
      })
    })

    it('does not callback a response', function(done){
      needle.get(url, { timeout: 200 }, function(err, resp){
        should.not.exist(resp);
        done();
      })
    })

  })

})
