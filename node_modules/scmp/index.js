/**
 * Does a constant-time string comparison by not short-circuiting
 * on first sign of non-equivalency.
 * 
 * @param {String} a The first string to be compared against the second
 * @param {String} b The second string to be compared against the first 
 * @return {Boolean}
 */
module.exports = function scmp(a, b) {
  a = String(a);
  b = String(b);
  if (a.length !== b.length) {
    return false;
  }
  var result = 0;
  for (var i = 0; i < a.length; ++i) {
    result |= a.charCodeAt(i) ^ b.charCodeAt(i);
  }
  return result === 0;
};
