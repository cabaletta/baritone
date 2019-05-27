var tape = require('tape')
var sorted = require('./')

tape('add', function (t) {
  var list = []

  sorted.add(list, 3)
  sorted.add(list, 4)
  sorted.add(list, 3)
  sorted.add(list, 9)
  sorted.add(list, 0)
  sorted.add(list, 5)
  sorted.add(list, 8)

  t.same(list, [0, 3, 3, 4, 5, 8, 9])
  t.end()
})

tape('remove', function (t) {
  var list = []

  sorted.add(list, 3)
  sorted.add(list, 4)
  sorted.add(list, 3)
  sorted.add(list, 9)
  sorted.add(list, 0)
  sorted.add(list, 5)
  sorted.add(list, 8)

  sorted.remove(list, 3)
  sorted.remove(list, 5)
  sorted.remove(list, 6)

  t.same(list, [0, 3, 4, 8, 9])
  t.end()
})

tape('has', function (t) {
  var list = []

  sorted.add(list, 3)
  t.same(sorted.has(list, 3), true)
  t.same(sorted.has(list, 2), false)

  sorted.add(list, 5)
  t.same(sorted.has(list, 5), true)
  t.same(sorted.has(list, 3), true)
  t.same(sorted.has(list, 2), false)

  sorted.add(list, 1)
  t.same(sorted.has(list, 1), true)
  t.same(sorted.has(list, 5), true)
  t.same(sorted.has(list, 3), true)
  t.same(sorted.has(list, 2), false)
  t.same(sorted.has(list, 8), false)

  t.end()
})

tape('eq', function (t) {
  var list = []

  sorted.add(list, 3)
  t.same(sorted.eq(list, 3), 0)
  t.same(sorted.eq(list, 2), -1)

  sorted.add(list, 5)
  t.same(sorted.eq(list, 5), 1)
  t.same(sorted.eq(list, 3), 0)
  t.same(sorted.eq(list, 2), -1)

  sorted.add(list, 1)
  t.same(sorted.eq(list, 1), 0)
  t.same(sorted.eq(list, 5), 2)
  t.same(sorted.eq(list, 3), 1)
  t.same(sorted.eq(list, 2), -1)
  t.same(sorted.eq(list, 8), -1)

  t.end()
})

tape('gte', function (t) {
  var list = []

  sorted.add(list, 3)
  t.same(sorted.gte(list, 3), 0)
  t.same(sorted.gte(list, 2), 0)

  sorted.add(list, 5)
  t.same(sorted.gte(list, 5), 1)
  t.same(sorted.gte(list, 3), 0)
  t.same(sorted.gte(list, 2), 0)

  sorted.add(list, 1)
  t.same(sorted.gte(list, 1), 0)
  t.same(sorted.gte(list, 5), 2)
  t.same(sorted.gte(list, 3), 1)
  t.same(sorted.gte(list, 2), 1)
  t.same(sorted.gte(list, 8), -1)

  t.end()
})

tape('gt', function (t) {
  var list = []

  sorted.add(list, 3)
  t.same(sorted.gt(list, 3), -1)
  t.same(sorted.gt(list, 2), 0)

  sorted.add(list, 5)
  t.same(sorted.gt(list, 5), -1)
  t.same(sorted.gt(list, 3), 1)
  t.same(sorted.gt(list, 2), 0)

  sorted.add(list, 1)
  t.same(sorted.gt(list, 1), 1)
  t.same(sorted.gt(list, 5), -1)
  t.same(sorted.gt(list, 3), 2)
  t.same(sorted.gt(list, 2), 1)
  t.same(sorted.gt(list, 8), -1)

  t.end()
})

tape('lte', function (t) {
  var list = []

  sorted.add(list, 3)
  t.same(sorted.lte(list, 3), 0)
  t.same(sorted.lte(list, 2), -1)

  sorted.add(list, 5)
  t.same(sorted.lte(list, 6), 1)
  t.same(sorted.lte(list, 5), 1)
  t.same(sorted.lte(list, 3), 0)
  t.same(sorted.lte(list, 2), -1)

  sorted.add(list, 1)
  t.same(sorted.lte(list, 1), 0)
  t.same(sorted.lte(list, 5), 2)
  t.same(sorted.lte(list, 3), 1)
  t.same(sorted.lte(list, 2), 0)
  t.same(sorted.lte(list, 8), 2)

  t.end()
})

tape('lt', function (t) {
  var list = []

  sorted.add(list, 3)
  t.same(sorted.lt(list, 3), -1)
  t.same(sorted.lt(list, 2), -1)
  t.same(sorted.lt(list, 4), 0)

  sorted.add(list, 5)
  t.same(sorted.lt(list, 6), 1)
  t.same(sorted.lt(list, 5), 0)
  t.same(sorted.lt(list, 3), -1)
  t.same(sorted.lt(list, 2), -1)

  sorted.add(list, 1)
  t.same(sorted.lt(list, 1), -1)
  t.same(sorted.lt(list, 5), 1)
  t.same(sorted.lt(list, 3), 0)
  t.same(sorted.lt(list, 2), 0)
  t.same(sorted.lt(list, 8), 2)

  t.end()
})

tape('custom compare add', function (t) {
  var list = []

  sorted.add(list, {foo: 3}, cmp)
  sorted.add(list, {foo: 4}, cmp)
  sorted.add(list, {foo: 3}, cmp)
  sorted.add(list, {foo: 9}, cmp)
  sorted.add(list, {foo: 0}, cmp)
  sorted.add(list, {foo: 5}, cmp)
  sorted.add(list, {foo: 8}, cmp)

  t.same(list, [{foo: 0}, {foo: 3}, {foo: 3}, {foo: 4}, {foo: 5}, {foo: 8}, {foo: 9}])
  t.end()
})

tape('custom compare remove', function (t) {
  var list = []

  sorted.add(list, {foo: 3}, cmp)
  sorted.add(list, {foo: 4}, cmp)
  sorted.add(list, {foo: 3}, cmp)
  sorted.add(list, {foo: 9}, cmp)
  sorted.add(list, {foo: 0}, cmp)
  sorted.add(list, {foo: 5}, cmp)
  sorted.add(list, {foo: 8}, cmp)

  sorted.remove(list, {foo: 3}, cmp)
  sorted.remove(list, {foo: 5}, cmp)
  sorted.remove(list, {foo: 6}, cmp)

  t.same(list, [{foo: 0}, {foo: 3}, {foo: 4}, {foo: 8}, {foo: 9}])
  t.end()
})

tape('custom compare has', function (t) {
  var list = []

  sorted.add(list, {foo: 3}, cmp)
  t.same(sorted.has(list, {foo: 3}, cmp), true)
  t.same(sorted.has(list, {foo: 2}, cmp), false)

  sorted.add(list, {foo: 5}, cmp)
  t.same(sorted.has(list, {foo: 5}, cmp), true)
  t.same(sorted.has(list, {foo: 3}, cmp), true)
  t.same(sorted.has(list, {foo: 2}, cmp), false)

  sorted.add(list, {foo: 1}, cmp)
  t.same(sorted.has(list, {foo: 1}, cmp), true)
  t.same(sorted.has(list, {foo: 5}, cmp), true)
  t.same(sorted.has(list, {foo: 3}, cmp), true)
  t.same(sorted.has(list, {foo: 2}, cmp), false)
  t.same(sorted.has(list, {foo: 8}, cmp), false)

  t.end()
})

tape('custom compare eq', function (t) {
  var list = []

  sorted.add(list, {foo: 3}, cmp)
  t.same(sorted.eq(list, {foo: 3}, cmp), 0)
  t.same(sorted.eq(list, {foo: 2}, cmp), -1)

  sorted.add(list, {foo: 5}, cmp)
  t.same(sorted.eq(list, {foo: 5}, cmp), 1)
  t.same(sorted.eq(list, {foo: 3}, cmp), 0)
  t.same(sorted.eq(list, {foo: 2}, cmp), -1)

  sorted.add(list, {foo: 1}, cmp)
  t.same(sorted.eq(list, {foo: 1}, cmp), 0)
  t.same(sorted.eq(list, {foo: 5}, cmp), 2)
  t.same(sorted.eq(list, {foo: 3}, cmp), 1)
  t.same(sorted.eq(list, {foo: 2}, cmp), -1)
  t.same(sorted.eq(list, {foo: 8}, cmp), -1)

  t.end()
})

tape('custom compare gte', function (t) {
  var list = []

  sorted.add(list, {foo: 3}, cmp)
  t.same(sorted.gte(list, {foo: 3}, cmp), 0)
  t.same(sorted.gte(list, {foo: 2}, cmp), 0)

  sorted.add(list, {foo: 5}, cmp)
  t.same(sorted.gte(list, {foo: 5}, cmp), 1)
  t.same(sorted.gte(list, {foo: 3}, cmp), 0)
  t.same(sorted.gte(list, {foo: 2}, cmp), 0)

  sorted.add(list, {foo: 1}, cmp)
  t.same(sorted.gte(list, {foo: 1}, cmp), 0)
  t.same(sorted.gte(list, {foo: 5}, cmp), 2)
  t.same(sorted.gte(list, {foo: 3}, cmp), 1)
  t.same(sorted.gte(list, {foo: 2}, cmp), 1)
  t.same(sorted.gte(list, {foo: 8}, cmp), -1)

  t.end()
})

tape('custom compare gt', function (t) {
  var list = []

  sorted.add(list, {foo: 3}, cmp)
  t.same(sorted.gt(list, {foo: 3}, cmp), -1)
  t.same(sorted.gt(list, {foo: 2}, cmp), 0)

  sorted.add(list, {foo: 5}, cmp)
  t.same(sorted.gt(list, {foo: 5}, cmp), -1)
  t.same(sorted.gt(list, {foo: 3}, cmp), 1)
  t.same(sorted.gt(list, {foo: 2}, cmp), 0)

  sorted.add(list, {foo: 1}, cmp)
  t.same(sorted.gt(list, {foo: 1}, cmp), 1)
  t.same(sorted.gt(list, {foo: 5}, cmp), -1)
  t.same(sorted.gt(list, {foo: 3}, cmp), 2)
  t.same(sorted.gt(list, {foo: 2}, cmp), 1)
  t.same(sorted.gt(list, {foo: 8}, cmp), -1)

  t.end()
})

tape('custom compare lte', function (t) {
  var list = []

  sorted.add(list, {foo: 3}, cmp)
  t.same(sorted.lte(list, {foo: 3}, cmp), 0)
  t.same(sorted.lte(list, {foo: 2}, cmp), -1)

  sorted.add(list, {foo: 5}, cmp)
  t.same(sorted.lte(list, {foo: 6}, cmp), 1)
  t.same(sorted.lte(list, {foo: 5}, cmp), 1)
  t.same(sorted.lte(list, {foo: 3}, cmp), 0)
  t.same(sorted.lte(list, {foo: 2}, cmp), -1)

  sorted.add(list, {foo: 1}, cmp)
  t.same(sorted.lte(list, {foo: 1}, cmp), 0)
  t.same(sorted.lte(list, {foo: 5}, cmp), 2)
  t.same(sorted.lte(list, {foo: 3}, cmp), 1)
  t.same(sorted.lte(list, {foo: 2}, cmp), 0)
  t.same(sorted.lte(list, {foo: 8}, cmp), 2)

  t.end()
})

tape('custom compare lt', function (t) {
  var list = []

  sorted.add(list, {foo: 3}, cmp)
  t.same(sorted.lt(list, {foo: 3}, cmp), -1)
  t.same(sorted.lt(list, {foo: 2}, cmp), -1)
  t.same(sorted.lt(list, {foo: 4}, cmp), 0)

  sorted.add(list, {foo: 5}, cmp)
  t.same(sorted.lt(list, {foo: 6}, cmp), 1)
  t.same(sorted.lt(list, {foo: 5}, cmp), 0)
  t.same(sorted.lt(list, {foo: 3}, cmp), -1)
  t.same(sorted.lt(list, {foo: 2}, cmp), -1)

  sorted.add(list, {foo: 1}, cmp)
  t.same(sorted.lt(list, {foo: 1}, cmp), -1)
  t.same(sorted.lt(list, {foo: 5}, cmp), 1)
  t.same(sorted.lt(list, {foo: 3}, cmp), 0)
  t.same(sorted.lt(list, {foo: 2}, cmp), 0)
  t.same(sorted.lt(list, {foo: 8}, cmp), 2)

  t.end()
})

tape('find nearest value', function (t) {
  var list = []

  sorted.add(list, 0.001)
  sorted.add(list, 10)
  sorted.add(list, 20)
  sorted.add(list, 30)
  sorted.add(list, 40)
  sorted.add(list, 50)
  sorted.add(list, 70)

  t.equal(sorted.nearest(list, 66), 6)
  t.equal(sorted.nearest(list, 51), 5)
  t.equal(sorted.nearest(list, 1), 0)
  t.equal(sorted.nearest(list, 0), 0)
  t.equal(sorted.nearest(list, 69.999), 6)
  t.equal(sorted.nearest(list, 72), 6)

  t.end()
})

function cmp (a, b) {
  return a.foo - b.foo
}

