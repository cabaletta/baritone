var path = require('path');
var fs = require('fs');

module.exports = function(filePath) {
  var orig, p, root, stat;
  p = filePath || path.join(__filename, '../../');
  stat = fs.lstatSync(p);
  if (stat.isSymbolicLink()) {
    p = fs.readlinkSync(p);
  }
  root = path.dirname(p);
  orig = process.env.NODE_PATH ? process.env.NODE_PATH : '';
  if (filePath) {
    process.env.NODE_PATH = path.join(root, '../libs') + path.delimiter + orig;
  } else {
    process.env.NODE_PATH = root + path.delimiter + orig;
  }
  return require('module')._initPaths();
};

