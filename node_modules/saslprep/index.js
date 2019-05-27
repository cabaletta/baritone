'use strict'

const {
  unassigned_code_points,
  commonly_mapped_to_nothing,
  non_ASCII_space_characters,
  prohibited_characters,
  bidirectional_r_al,
  bidirectional_l,
} = require('./lib/code-points')

module.exports = saslprep

// 2.1.  Mapping

/**
 * non-ASCII space characters [StringPrep, C.1.2] that can be
 * mapped to SPACE (U+0020)
 * @type {Set}
 */
const mapping2space = non_ASCII_space_characters

/**
 * the "commonly mapped to nothing" characters [StringPrep, B.1]
 * that can be mapped to nothing.
 * @type {Set}
 */
const mapping2nothing = commonly_mapped_to_nothing

// utils
const getCodePoint = character => character.codePointAt(0)
const first = x => x[0]
const last = x => x[x.length - 1]

/**
 * SASLprep.
 * @param {string} input
 * @param {object} opts
 * @param {boolean} opts.allowUnassigned
 */
function saslprep(input, opts = {}) {
  if (typeof input !== 'string') {
    throw new TypeError('Expected string.')
  }

  if (input.length === 0) {
    return ''
  }

  // 1. Map
  const mapped_input = input
    .split('')
    .map(getCodePoint)
    // 1.1 mapping to space
    .map(character => (mapping2space.has(character) ? 0x20 : character))
    // 1.2 mapping to nothing
    .filter(character => !mapping2nothing.has(character))

  // 2. Normalize
  const normalized_input = String.fromCodePoint(...mapped_input).normalize('NFKC')

  const normalized_map = normalized_input.split('').map(getCodePoint)

  // 3. Prohibit
  const hasProhibited = normalized_map.some(character =>
    prohibited_characters.has(character)
  )

  if (hasProhibited) {
    throw new Error(
      'Prohibited character, see https://tools.ietf.org/html/rfc4013#section-2.3'
    )
  }

  // Unassigned Code Points
  if (opts.allowUnassigned !== true) {
    const hasUnassigned = normalized_map.some(character =>
      unassigned_code_points.has(character)
    )

    if (hasUnassigned) {
      throw new Error(
        'Unassigned code point, see https://tools.ietf.org/html/rfc4013#section-2.5'
      )
    }
  }

  // 4. check bidi

  const hasBidiRAL = normalized_map
    .some((character) => bidirectional_r_al.has(character))

  const hasBidiL = normalized_map
    .some((character) => bidirectional_l.has(character))

  // 4.1 If a string contains any RandALCat character, the string MUST NOT
  // contain any LCat character.
  if (hasBidiRAL && hasBidiL) {
    throw new Error(
      'String must not contain RandALCat and LCat at the same time,' +
      ' see https://tools.ietf.org/html/rfc3454#section-6'
    )
  }

  /**
   * 4.2 If a string contains any RandALCat character, a RandALCat
   * character MUST be the first character of the string, and a
   * RandALCat character MUST be the last character of the string.
   */

  const isFirstBidiRAL = bidirectional_r_al.has(getCodePoint(first(normalized_input)))
  const isLastBidiRAL = bidirectional_r_al.has(getCodePoint(last(normalized_input)))

  if (hasBidiRAL && !(isFirstBidiRAL && isLastBidiRAL)) {
    throw new Error(
      'Bidirectional RandALCat character must be the first and the last' +
      ' character of the string, see https://tools.ietf.org/html/rfc3454#section-6'
    )
  }

  return normalized_input
}
