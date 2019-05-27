var sorted = require('./')
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
