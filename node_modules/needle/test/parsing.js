var needle = require('./..'),
    should = require('should'),
    xml2js_present;

try {
  require('xml2js');
  xml2js_present = true;
} catch(e) {
  xml2js_present = false;
}


describe('parsing', function() {

  var url, opts = {};

  if (!xml2js_present)
    return;

  describe('when xml2js is present', function(){

    describe('and parse option is true', function(){

      before(function() {
        opts.parse = true;
      })

      describe('and a valid XML is requested', function(){

        url = 'https://news.ycombinator.com/rss';

        it('parses it', function(done) {

          needle.get(url, opts, function(err, resp, body){
            resp.statusCode.should.equal(200);
            should.exist(resp.body.rss);
            done();
          })

        });

      });

    });

  });

})
