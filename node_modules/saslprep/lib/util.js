'use strict'

function range(from, to) {
  // TODO: make this inlined.
  const list = new Array(to - from + 1)

  for(let i = 0; i < list.length; ++i) {
    list[i] = from + i
  }
  return list
}

module.exports = {
  range
}
