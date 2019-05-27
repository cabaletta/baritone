Needle
======

The leanest and most handsome HTTP client in the Nodelands. Supports SSL, basic & digest auth, proxied requests, multipart form-data (e.g. file uploads), gzip/deflate compression, automatic XML/JSON parsing, follows redirects and decodes non-UTF-8 content. Two dependencies only.

Ideal for performing simple, quick HTTP requests in Node.js. If you need OAuth, AWS support or anything fancier, you should check out mikeal's request module. 

Install
-----

```
npm install needle
```

Usage
-----

``` js
var needle = require('needle');

needle.get('http://www.google.com', function(error, response, body){
  console.log("Got status code: " + response.statusCode);
});
```

Methods
-------

``` js
needle.get(url, [options], callback);
needle.head(url, [options], callback);
needle.post(url, data, [options], callback);
needle.put(url, data, [options], callback);
needle.delete(url, data, [options], callback);
```
Callback receives `(error, response, body)`. Needle returns the response stream, which means you can pipe it to your heart's content.

``` js
needle.get('google.com/images/logo.png').pipe(fs.createWriteStream('logo.png'));
```

For more examples, scroll down exactly seven turns of your mousewheel. Perhaps eight.


Request options
---------------

 - `timeout`   : Returns error if no response received in X milisecs. Defaults to `10000` (10 secs). `0` means no timeout.
 - `follow`    : Number of redirects to follow. `false` means don't follow any (default), `true` means 10. 
 - `multipart` : Enables multipart/form-data encoding. Defaults to `false`.
 - `proxy`     : Forwards request through HTTP(s) proxy. Eg. `proxy: 'http://proxy.server.com:3128'`
 - `agent`     : Uses an http.Agent of your choice, instead of the global (default) one.
 - `headers`   : Object containing custom HTTP headers for request. Overrides defaults described below.
 - `auth`      : Determines what to do with provided username/password. Options are `auto`, `digest` or `basic` (default).

Response options
----------------

 - `decode`    : Whether to decode response to UTF-8 if Content-Type charset is different. Defaults to `true`.
 - `parse`     : Whether to parse XML or JSON response bodies automagically. Defaults to `true`.
 - `output`    : Dump response output to file. When response is text, this occurs after parsing/decoding is done.

Note: To stay light on dependencies, Needle doesn't include the `xml2js` module used for XML parsing. To enable it, simply do `npm install xml2js`.

HTTP Header options
-------------------

These are basically shortcuts to the `headers` option described above.

 - `compressed`: If `true`, sets 'Accept-Encoding' header to 'gzip,deflate', and inflates content if zipped. Defaults to `false`.
 - `username`  : For HTTP basic auth.
 - `password`  : For HTTP basic auth. Requires username to be passed, obviously.
 - `accept`    : Sets 'Accept' HTTP header. Defaults to `*/*`.
 - `connection`: Sets 'Connection' HTTP header. Defaults to `close`.
 - `user_agent`: Sets the 'User-Agent' HTTP header. Defaults to `Needle/{version} (Node.js {node_version})`.

Node.js TLS Options
-------------------

These options are passed directly to `https.request` if present. Taken from the [original documentation](http://nodejs.org/docs/latest/api/https.html):

 - `pfx`: Certificate, Private key and CA certificates to use for SSL.
 - `key`: Private key to use for SSL.
 - `passphrase`: A string of passphrase for the private key or pfx.
 - `cert`: Public x509 certificate to use.
 - `ca`: An authority certificate or array of authority certificates to check the remote host against.
 - `ciphers`: A string describing the ciphers to use or exclude.
 - `rejectUnauthorized`: If true, the server certificate is verified against the list of supplied CAs. An 'error' event is emitted if verification fails. Verification happens at the connection level, before the HTTP request is sent.

Examples
--------

### GET with querystring

``` js
needle.get('http://www.google.com/search?q=syd+barrett', function(err, resp, body){
  if (!err && resp.statusCode == 200)
    console.log(body); // prints HTML
});
```

### HTTPS GET with Basic Auth

``` js
needle.get('https://api.server.com', { username: 'you', password: 'secret' },
  function(err, resp, body){
    // used HTTP auth
});
```

### Digest Auth

``` js
needle.get('other.server.com', { username: 'you', password: 'secret', auth: 'digest' },
  // needle prepends 'http://' to your URL, if missing
});
```

### Custom headers, deflate

``` js
var options = {
  compressed : true,       // request a deflated response
  parse      : true,       // parse JSON
  headers    : {
    'X-Custom-Header': "Bumbaway atuna"
  }
}

needle.get('http://server.com/posts.json', options, function(err, resp, body){
  // body will contain a JSON.parse(d) object
  // if parsing fails, you'll simply get the original body
});
```

### GET XML object

``` js
needle.get('https://news.ycombinator.com/rss', function(err, resp, body){
  // if xml2js is installed, you'll get a nice object containing the nodes in the RSS
});
```

### GET binary, output to file

``` js
needle.get('http://upload.server.com/tux.png', { output: '/tmp/tux.png' }, function(err, resp, body){
  // you can dump any response to a file, not only binaries.
});
```

### GET through proxy

``` js
needle.get('http://search.npmjs.org', { proxy: 'http://localhost:1234' }, function(err, resp, body){
  // request passed through proxy
});
```

### Simple POST

``` js
needle.post('https://my.app.com/endpoint', 'foo=bar', function(err, resp, body){
  // you can pass params as a string or as an object
});
```

### PUT with data object

``` js
var nested = {
  params: {
    are: {
      also: 'supported'
    }
  }
}

needle.put('https://api.app.com/v2', nested, function(err, resp, body){
  // if you don't pass any data, needle will throw an exception.
});
```

### File upload using multipart, passing file path

``` js
var data = {
  foo: 'bar',
  image: { file: '/home/tomas/linux.png', content_type: 'image/png' }
}

needle.post('http://my.other.app.com', data, { multipart: true }, function(err, resp, body){
  // needle will read the file and include it in the form-data as binary
});
```

### Multipart POST, passing data buffer

``` js
var buffer = fs.readFileSync('/path/to/package.zip');

var data = {
  zip_file: {
    buffer       : buffer,
    filename     : 'mypackage.zip',
    content_type : 'application/octet-stream'
  },
}

needle.post('http://somewhere.com/over/the/rainbow', data, { multipart: true }, function(err, resp, body){
  // if you see, when using buffers we need to pass the filename for the multipart body.
  // you can also pass a filename when using the file path method, in case you want to override
  // the default filename to be received on the other end.
});
```

### Multipart with custom Content-Type

``` js
var data = {
  token: 'verysecret',
  payload: {
    value: JSON.stringify({ title: 'test', version: 1 }),
    content_type: 'application/json'
  }
}

needle.post('http://test.com/', data, { timeout: 5000, multipart: true }, function(err, resp, body){
  // in this case, if the request takes more than 5 seconds
  // the callback will return a [Socket closed] error
});
```

Credits
-------

Written by Tomás Pollak, with the help of contributors.

Copyright
---------

(c) 2013 Fork Ltd. Licensed under the MIT license.
