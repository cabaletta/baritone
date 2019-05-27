var os = require('os');

var hits = {
};
var deprecate = module.exports = function(methodName, message) {
  if(deprecate.silence) return;
  if(hits[deprecate.caller]) return;
  hits[deprecate.caller] = true;
  deprecate.stream.write(os.EOL);
  if(deprecate.color) {
    deprecate.stream.write(deprecate.color);
  }
  deprecate.stream.write('WARNING!!');
  deprecate.stream.write(os.EOL);
  for(var i = 0; i < arguments.length; i++) {
    deprecate.stream.write(arguments[i]);
    deprecate.stream.write(os.EOL);
  }
  if(deprecate.color) {
    deprecate.stream.write('\x1b[0m');
  }
  deprecate.stream.write(os.EOL);
  deprecate.stream.write(os.EOL);
};

deprecate.stream = process.stderr;
deprecate.silence = false;
deprecate.color = deprecate.stream.isTTY && '\x1b[31;1m';
