var http = require('http');

var helpers = {};

helpers.server = function(opts) {

  var default_headers = {'Content-Type': 'text/html'};

  var finish = function(req, res) {
    res.writeHead(opts.code || 200, opts.headers || default_headers);
    res.end(opts.response || 'Hello there.');
  }

  var server = http.createServer(function(req, res){

    setTimeout(function(){
      finish(req, res);
    }, opts.wait || 0);

  })

  server.listen(opts.port);
  return server;

}

module.exports = helpers;
