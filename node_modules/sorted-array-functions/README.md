# sorted-array-functions

Maintain and search through a sorted array using some low level functions

```
npm install sorted-array-functions
```

[![build status](http://img.shields.io/travis/mafintosh/sorted-array-functions.svg?style=flat)](http://travis-ci.org/mafintosh/sorted-array-functions)

## Usage

``` js
var sorted = require('sorted-array-functions')
var list = []

sorted.add(list, 1)
sorted.add(list, 4)
sorted.add(list, 2)

console.log(list) // prints out [1, 2, 4]
console.log(sorted.has(list, 2)) // returns true
console.log(sorted.has(list, 3)) // returns false
console.log(sorted.eq(list, 2)) // returns 1 (the index)
console.log(sorted.gt(list, 2)) // returns 2
console.log(sorted.gt(list, 4)) // returns -1
```

## API

#### `sorted.add(list, value, [compare])`

Insert a new value into the list sorted.
Optionally you can use a custom compare function that returns, `compare(a, b)` that returns 1 if `a > b`, 0 if `a === b` and -1 if `a < b`.

#### `var bool = sorted.remove(list, value, [compare])`

Remove a value. Returns true if the value was in the list.

#### `var bool = sorted.has(list, value, [compare])`

Check if a value is in the list.

#### `var index = sorted.eq(list, value, [compare])`

Get the index of a value in the list (uses binary search).
If the value could not be found -1 is returned.

#### `var index = sorted.gte(list, value, [compare])`

Get the index of the first value that is `>=`.
If the value could not be found -1 is returned.

#### `var index = sorted.gt(list, value, [compare])`

Get the index of the first value that is `>`.
If the value could not be found -1 is returned.

#### `var index = sorted.lte(list, value, [compare])`

Get the index of the first value that is `<=`.
If the value could not be found -1 is returned.

#### `var index = sorted.lt(list, value, [compare])`

Get the index of the first value that is `<`.
If the value could not be found -1 is returned.

## License

MIT
