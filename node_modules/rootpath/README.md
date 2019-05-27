rootpath
========

Little helper to make node.js `require` relative to your project root

When working on a node.js project with subfolders you might find it difficult to remember the correct relative path to include a common module or library which is not published on npm. 
Using this module you just need to remember your own project directory structure and make all `require` absolute to the project root directory

[![Build Status](https://travis-ci.org/fabriziomoscon/rootpath.png)](https://travis-ci.org/fabriziomoscon/rootpath)

install
-------

`npm install rootpath`


BEFORE
```JavaScript
// from $HOME_PROJECT/lib/math/
var myLib = require('../myLibrary');
var myUtils = require('../../utils/myUtils');
var myTest = require('../../test/myTest');
``` 

AFTER
```JavaScript
// from $HOME_PROJECT/lib/math/

require('rootpath')();

var myLib = require('lib/myLibrary');
var myUtils = require('utils/myUtils');
var myTest = require('test/myTest');
```

improvements
------------

Forks and pull requests are welcome. There is always a better way of achieving a goal. This project will benefit from the contribution of the community.

test
----

```bash
npm test
```
license
-------

MIT