var createHash = require('crypto').createHash;

var md5 = function(string) {
  return createHash('md5').update(string).digest('hex');
}

var basic = function(user, pass) {
  var buff = new Buffer([user, pass].join(':'));
  return 'Basic ' + buff.toString('base64');
}

// digest logic inspired from https://github.com/simme/node-http-digest-client
var digest = {};

digest.parse_header = function(header) {
  var challenge = {},
      matches   = header.match(/([a-z0-9_-]+)="([^"]+)"/gi);

  for (var i = 0, l = matches.length; i < l; i++) {
    var pos  = matches[i].indexOf('='),
        key  = matches[i].substring(0, pos),
        val  = matches[i].substring(pos + 1);
    challenge[key] = val.substring(1, val.length - 1);
  }

  return challenge;
}

digest.update_nc = function(nc) {
  var max = 99999999;
  nc++;

  if (nc > max)
    nc = 1;

  var padding = new Array(8).join('0') + '';
  nc = nc + '';
  return padding.substr(0, 8 - nc.length) + nc;
}

digest.generate = function(header, user, pass, method, path) {

  var nc        = 1,
      cnonce    = null,
      challenge = digest.parse_header(header);

  var ha1    = md5(user + ':' + challenge.realm + ':' + pass),
      ha2    = md5(method + ':' + path),
      resp   = [ha1, challenge.nonce];

  if (typeof challenge.qop === 'string') {
    cnonce = md5(Math.random().toString(36)).substr(0, 8);
    nc = digest.update_nc(nc);
    resp = resp.concat(nc, cnonce);
  }
  
  resp = resp.concat(challenge.qop, ha2);

  var params = {
    username: user,
    realm: challenge.realm,
    nonce: challenge.nonce,
    uri: path,
    qop: challenge.qop,
    response: md5(resp.join(':'))
  }
  
//  if (challenge.opaque) {
//    params.opaque = challenge.opaque;
//  }
  
  if (cnonce) {
    params.nc = nc;
    params.cnonce = cnonce;
  }

  header = []
  for (var k in params)
    header.push(k + '="' + params[k] + '"')

  return 'Digest ' + header.join(', ');
}

module.exports = {
  basic: basic,
  digest: digest.generate
}
