import test from 'ava'
import saslprep from '../'

const chr = String.fromCodePoint

test('should work with liatin letters', (t) => {
  const str = 'user'
  t.is(saslprep(str), str)
})

test('should work be case preserved', (t) => {
  const str = 'USER'
  t.is(saslprep(str), str)
})

test('should remove `mapped to nothing` characters', (t) => {
  t.is(saslprep('I\u00ADX'), 'IX')
})

test('should replace `Non-ASCII space characters` with space', (t) => {
  t.is(saslprep('a\u00A0b'), 'a\u0020b')
})

test('should normalize as NFKC', (t) => {
  t.is(saslprep('\u00AA'), 'a')
  t.is(saslprep('\u2168'), 'IX')
})

test('should throws when prohibited characters', (t) => {
  // C.2.1 ASCII control characters
  t.throws(() => saslprep('a\u007Fb'))

  // C.2.2 Non-ASCII control characters
  t.throws(() => saslprep('a\u06DDb'))

  // C.3 Private use
  t.throws(() => saslprep('a\uE000b'))

  // C.4 Non-character code points
  t.throws(() => saslprep(`a${chr(0x1FFFE)}b`))

  // C.5 Surrogate codes
  t.throws(() => saslprep('a\uD800b'))

  // C.6 Inappropriate for plain text
  t.throws(() => saslprep('a\uFFF9b'))

  // C.7 Inappropriate for canonical representation
  t.throws(() => saslprep('a\u2FF0b'))

  // C.8 Change display properties or are deprecated
  t.throws(() => saslprep('a\u200Eb'))

  // C.9 Tagging characters
  t.throws(() => saslprep(`a${chr(0xE0001)}b`))
})

test('should not containt RandALCat and LCat bidi', (t) => {
  t.throws(() => saslprep('a\u06DD\u00AAb'))
})

test('RandALCat should be first and last', (t) => {
  t.notThrows(() => saslprep('\u0627\u0031\u0628'))
  t.throws(() => saslprep('\u0627\u0031'))
})

test('should handle unassigned code points', (t) => {
  t.throws(() => saslprep('a\u0487'))
  t.notThrows(() => saslprep('a\u0487', {allowUnassigned: true}))
})
