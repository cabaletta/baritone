//////////////////////////////////////////
// Needle -- Node.js HTTP Client
// Written by Tomás Pollak <tomas@forkhq.com>
// (c) 2012-2013 - Fork Ltd.
// MIT Licensed
//////////////////////////////////////////

var fs          = require('fs'),
    http        = require('http'),
    https       = require('https'),
    url         = require('url'),
    Readable    = require('stream').Readable,
    stringify   = require('qs').stringify,
    multipart   = require('./multipart'),
    auth        = require('./auth');

//////////////////////////////////////////
// variabilia
//////////////////////////////////////////

var version     = JSON.parse(fs.readFileSync(__dirname + '/../package.json').toString()).version,
    debugging   = !!process.env.DEBUG;

try { var unzip = require('zlib').unzip } catch(e) { /* unzip not supported */ }

var user_agent = 'Needle/' + version;
user_agent    += ' (Node.js ' + process.version + '; ' + process.platform + ' ' + process.arch + ')';

var node_tls_opts = 'agent pfx key passphrase cert ca ciphers rejectUnauthorized secureProtocol';

var debug = function(str, obj) {
  if (debugging)
    console.log(str, obj);
}

//////////////////////////////////////////
// content type parsers
//////////////////////////////////////////

var parsers = {
  'application/json': function(data, callback){
    try {
      callback(null, data && JSON.parse(data));
    } catch(e) {
      callback(e, data);
    }
  }
};

try {
  var xml2js = require('xml2js');
  parsers['application/xml'] = function(data, callback){
    var xml_parser = new xml2js.Parser({ explicitRoot: true, explicitArray: false });
    xml_parser.parseString(data, function(err, result){
      callback(err, err ? data : result); // return original if err failed
    });
  };
  parsers['text/xml'] = parsers['application/xml'];
  parsers['application/rss+xml'] = parsers['application/xml'];
} catch(e) { /* xml2js not found */ }

//////////////////////////////////////////
// defaults
//////////////////////////////////////////

var defaults = {
  accept          : '*/*',
  connection      : 'close',
  user_agent      : user_agent,
  follow          : 0,
  decode_response : true,
  parse_response  : true,
  timeout         : 10000,
  encoding        : 'utf8',
  boundary        : '--------------------NODENEEDLEHTTPCLIENT'
}

//////////////////////////////////////////
// the main act
//////////////////////////////////////////

var Needle = {

  request: function(method, uri, data, options, callback){

    var self     = this;
    var callback = (typeof options == 'function') ? options : callback;
    var options  = options || {};

    // if no 'http' is found on URL, prepend it
    if (uri.indexOf('http') == -1) uri = 'http://' + uri;

    var config = {
      base_opts       : {},
      proxy           : options.proxy,
      output          : options.output,
      encoding        : options.encoding || (options.multipart ? 'binary' : defaults.encoding),
      decode_response : options.decode === false ? false : defaults.decode_response,
      parse_response  : options.parse === false ? false : defaults.parse_response,
      follow          : options.follow === true ? 10 : typeof options.follow == 'number' ? options.follow : defaults.follow,
      timeout         : (typeof options.timeout == 'number') ? options.timeout : defaults.timeout
    }

    // if any of node's TLS options are passed, let them be passed to https.request()
    node_tls_opts.split(' ').forEach(function(key){
      if (typeof options[key] != 'undefined') {
        config.base_opts[key] = options[key];
        if (typeof options.agent == 'undefined')
          config.base_opts.agent = false; // otherwise tls options are skipped
      }
    });

    config.headers = {
      'Accept'     : options.accept     || defaults.accept,
      'Connection' : options.connection || defaults.connection,
      'User-Agent' : options.user_agent || defaults.user_agent
    }

    if (options.compressed && typeof unzip != 'undefined')
      config.headers['Accept-Encoding'] = 'gzip,deflate';

    for (var h in options.headers)
      config.headers[h] = options.headers[h];

    if (options.username && options.password) {
      if (options.auth && (options.auth == 'auto' || options.auth == 'digest')) {
        config.credentials = [options.username, options.password];
      } else {
        var auth_header = options.proxy ? 'Proxy-Authorization' : 'Authorization';
        config.headers[auth_header] = auth.basic(options.username, options.password);
      }
    }

    if (data) {
      if (options.multipart) {
        var boundary = options.boundary || defaults.boundary;
        return multipart.build(data, boundary, function(err, body){
          if (err) throw(err);
          config.headers['Content-Type'] = 'multipart/form-data; boundary=' + boundary;
          config.headers['Content-Length'] = body.length;
          self.send_request(1, method, uri, config, body, callback);
        });

      } else {
        var post_data = (typeof(data) === 'string') ? data :
            options.json ? JSON.stringify(data) : stringify(data);

        if (!config.headers['Content-Type']) {
          config.headers['Content-Type'] = options.json
          ? 'application/json'
          : 'application/x-www-form-urlencoded';
        }

        post_data = new Buffer(post_data, config.encoding)
        config.headers['Content-Length'] = post_data.length;
      }
    }

    return this.send_request(1, method, uri, config, post_data, callback);
  },

  get_request_opts: function(method, uri, config){
    var opts      = config.base_opts, proxy = config.proxy;
    var remote    = proxy ? url.parse(proxy) : url.parse(uri);

    opts.protocol = remote.protocol;
    opts.host     = remote.hostname;
    opts.port     = remote.port || (remote.protocol == 'https:' ? 443 : 80);
    opts.path     = proxy ? uri : remote.pathname + (remote.search || '');
    opts.method   = method;
    opts.headers  = config.headers;
    opts.headers['Host'] = proxy ? url.parse(uri).hostname : remote.hostname;

    return opts;
  },

  get_auth_header: function(header, credentials, request_opts) {
    var type = header.split(' ')[0],
        user = credentials[0],
        pass = credentials[1];

    if (type == 'Digest') {
      return auth.digest(header, user, pass, request_opts.method, request_opts.path);
    } else if (type == 'Basic') {
      return auth.basic(user, pass);
    }
  },

  send_request: function(count, method, uri, config, post_data, callback){

    var timer,
        self = this,
        request_opts = this.get_request_opts(method, uri, config),
        protocol = request_opts.protocol == 'https:' ? https : http;

    if (Readable && !config.out) {
      config.out = new Readable();
      config.out.on('error', function(er){ /* noop */ })
    }

    debug('Making request #' + count, request_opts);
    var request = protocol.request(request_opts, function(resp){

      var headers = resp.headers;
      debug('Got response', headers);
      if (timer) clearTimeout(timer);

      // if redirect code is found, send a GET request to that location if enabled via 'follow' option
      if ([301, 302].indexOf(resp.statusCode) != -1 && headers.location) {
        if (count <= config.follow)
          return self.send_request(++count, 'GET', url.resolve(uri, headers.location), config, null, callback);
        else if (config.follow > 0)
          return callback(new Error('Max redirects reached. Possible loop in: ' + headers.location));
      }

      // if authentication is requested and credentials were not passed, resend request if we have user/pass
      if (resp.statusCode == 401 && headers['www-authenticate'] && config.credentials) {
        if (!config.headers['Authorization']) { // only if authentication hasn't been sent
          var auth_header = self.get_auth_header(headers['www-authenticate'], config.credentials, request_opts);

          if (auth_header) {
            config.headers['Authorization'] = auth_header;
            return self.send_request(count, method, uri, config, post_data, callback);
          }
        }
      }

      var chunks = [], length = 0,
          compressed = /gzip|deflate/.test(headers['content-encoding']),
          mime = self.parse_content_type(headers['content-type']);

      var response_opts = {
        output       : config.output,
        parse        : config.parse_response, // parse XML or JSON
        content_type : mime.type,
        text         : mime.type && mime.type.indexOf('text/') != -1,
        charset      : mime.charset
      }

      if (response_opts.text)
        response_opts.decode = config.decode_response; // only allow iconv decoding on text bodies

    // response.setEncoding(response_opts.utf8 ? 'utf8' : 'binary');

      resp.on('data', function(chunk){
        chunks.push(chunk);
        if (config.out) config.out.push(chunk);
        length += chunk.length;
        // debug('Got ' + length + ' bytes.')
      });

      resp.on('end', function(){
        if (config.out) config.out.push(null); // close readable out stream

        // build the body by merging all the chunks we received
        var pos = 0;
        resp.bytes = length;
        resp.body  = new Buffer(resp.bytes);
        for (var i = 0, len = chunks.length; i < len; i++) {
          chunks[i].copy(resp.body, pos);
          pos += chunks[i].length;
        }

        // if no unzip support, or no deflated body requested, return
        if (!compressed || typeof unzip == 'undefined')
          return self.response_end(response_opts, resp, callback);

        unzip(resp.body, function(err, buff){
          if (!err && buff) resp.body = buff; // only if no errors
          self.response_end(response_opts, resp, callback);
        });

      });

    });

    // unless timeout was disabled, set a timeout to abort the request
    if (config.timeout > 0) {
      timer = setTimeout(function() {
        request.abort();
      }, config.timeout)
    }

    request.on('error', function(err) {
      debug('Request error', err);
      if (timer) clearTimeout(timer);
      if (callback) callback(err || new Error('Unknown error when making request.'));
    });

    if (post_data) request.write(post_data, config.encoding);
    request.end();

    return config.out || request;
  },

  parse_content_type: function(header){
    if (!header || header == '') return {};

    var charset = 'iso-8859-1', arr = header.split(';');
    try { charset = arr[1].match(/charset=(.+)/)[1] } catch (e) { /* not found */ }

    return { type: arr[0], charset: charset };
  },

  response_end: function(opts, resp, callback){
    if (!callback) return; // no point in going ahead

    var handle_output = function(err, final){
      if (final) resp.body = final;

      if (err || !opts.output || resp.statusCode != 200)
        return callback(err, resp, resp.body);

      // err is nil and output requested, so write to file
      fs.writeFile(opts.output, resp.body, function(e){
        callback(e, resp, resp.body);
      })
    }

    // if there's a parser for the content type received, process it
    if (opts.parse && parsers[opts.content_type]) {
      parsers[opts.content_type](resp.body.toString(), handle_output);
    } else {
      // if charset is not UTF-8 and decode option is true, transmogrify it
      if (opts.decode && opts.charset && !opts.charset.match(/utf-?8$/i))
        resp.body = require('iconv-lite').decode(resp.body, opts.charset);

      handle_output(null, opts.text ? resp.body.toString() : resp.body);
    }

  }

}

exports.version = version;

exports.defaults = function(obj) {
  for (var key in obj) {
    if (defaults[key] && typeof obj[key] != 'undefined')
      defaults[key] = obj[key];
  }
  return defaults;
}

exports.head   = function(uri, options, callback){
  return Needle.request('HEAD', uri, null, options, callback);
}

exports.get    = function(uri, options, callback){
  return Needle.request('GET', uri, null, options, callback);
}

exports.post   = function(uri, data, options, callback){
  return Needle.request('POST', uri, data, options, callback);
}

exports.put    = function(uri, data, options, callback){
  return Needle.request('PUT', uri, data, options, callback);
}

exports.delete = function(uri, data, options, callback){
  return Needle.request('DELETE', uri, data, options, callback);
}

exports.request = function(method, uri, data, opts, callback) {
  return Needle.request(method.toUpperCase(), uri, data, opts, callback);
};
