exports.add = add
exports.remove = remove
exports.has = has
exports.eq = eq
exports.lte = lte
exports.lt = lt
exports.gte = gte
exports.gt = gt
exports.nearest = nearest

function defaultCmp (a, b) {
  if (a === b) return 0
  return a > b ? 1 : -1
}

function add (list, value, cmp) {
  if (!cmp) cmp = defaultCmp

  var top = list.push(value) - 1

  while (top) {
    if (cmp(list[top - 1], value) < 0) return
    list[top] = list[top - 1]
    list[top - 1] = value
    top--
  }
}

function lte (list, value, cmp) {
  if (!cmp) cmp = defaultCmp

  var i = indexOf(list, value, cmp)
  if (i === -1) return -1

  for (; i >= 0; i--) {
    var c = cmp(list[i], value)
    if (c <= 0) return i
  }

  return -1
}

function lt (list, value, cmp) {
  if (!cmp) cmp = defaultCmp

  var i = indexOf(list, value, cmp)
  if (i === -1) return -1

  for (; i >= 0; i--) {
    var c = cmp(list[i], value)
    if (c < 0) return i
  }

  return -1
}

function gte (list, value, cmp) {
  if (!cmp) cmp = defaultCmp

  var i = indexOf(list, value, cmp)
  if (i === -1) return -1

  for (; i < list.length; i++) {
    var c = cmp(list[i], value)
    if (c >= 0) return i
  }

  return -1
}

function gt (list, value, cmp) {
  if (!cmp) cmp = defaultCmp

  var i = indexOf(list, value, cmp)
  if (i === -1) return -1

  for (; i < list.length; i++) {
    var c = cmp(list[i], value)
    if (c > 0) return i
  }

  return -1
}

function eq (list, value, cmp) {
  if (!cmp) cmp = defaultCmp

  var i = indexOf(list, value, cmp)
  if (i === -1) return -1
  return cmp(list[i], value) === 0 ? i : -1
}

function nearest (list, value, cmp) {
  if (!cmp) cmp = defaultCmp

  var len = list.length
  var top = len - 1
  var btm = 0
  var mid = -1
  var trending = 1 // 0 = down, 2 = up

  while (top >= btm && btm >= 0 && top < len) {
    mid = Math.floor((top + btm) / 2)

    var c = cmp(list[mid], value)
    if (c === 0) return mid

    if (c >= 0) {
      if (trending === 1) trending = 0
      else if (trending === 2) {
        if (Math.abs(list[mid] - value) > Math.abs(list[mid - 1] - value)) return mid - 1
        return mid
      }

      top = mid - 1
    } else {
      if (trending === 1) trending = 2
      else if (trending === 0) return mid

      btm = mid + 1
    }
  }

  return mid
}

function indexOf (list, value, cmp) {
  if (!cmp) cmp = defaultCmp

  var len = list.length
  var top = len - 1
  var btm = 0
  var mid = -1

  while (top >= btm && btm >= 0 && top < len) {
    mid = Math.floor((top + btm) / 2)

    var c = cmp(list[mid], value)
    if (c === 0) return mid

    if (c >= 0) {
      top = mid - 1
    } else {
      btm = mid + 1
    }
  }

  return mid
}

function has (list, value, cmp) {
  return eq(list, value, cmp) > -1
}

function remove (list, value, cmp) {
  var i = eq(list, value, cmp)
  if (i === -1) return false
  list.splice(i, 1)
  return true
}
