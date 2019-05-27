import { setFlagsFromString } from 'v8'
import { range } from '../lib/util'
import test from 'ava'

// 984 by default.
setFlagsFromString('--stack_size=500')

test('should work', (t) => {
  const list = range(1, 3)
  t.deepEqual(list, [1, 2, 3])
})

test('should work for large ranges', (t) => {
  t.notThrows(() => range(1, 1e6))
})
