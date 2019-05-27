var fs = require('fs'),
    needle = require('./..'),
    path = require('path');

var url  = process.argv[2] || 'https://upload.wikimedia.org/wikipedia/commons/a/af/Tux.png';
var file = path.basename(url);

console.log('Downloading ' + file);

needle.get(url, { output: file, follow: true }, function(err, resp, data){
  console.log('File saved: ' + process.cwd() + '/' + file);
  console.log(resp.bytes + ' bytes transferred.');
});
