# google-p12-pem

[![NPM Version][npm-image]][npm-url]
[![Build Status][travis-image]][travis-url]
[![Dependency Status][david-image]][david-url]
[![devDependency Status][david-dev-image]][david-dev-url]
[![Known Vulnerabilities][snyk-image]][snyk-url]
[![Greenkeeper badge](https://badges.greenkeeper.io/google/google-p12-pem.svg)](https://greenkeeper.io/)

Convert Google `.p12` keys to `.pem` keys.

## Installation

``` sh
npm install google-p12-pem
```

## Usage

### async/await style
```js
const {getPem} = require('google-p12-pem');
async function foo() {
  const pem = await getPem('/path/to/key.p12');
  console.log(pem); // '-----BEGIN RSA PRIVATE KEY-----\nMIICXQIBAAK...'
}
```

### promise style
```js
const {getPem} = require('google-p12-pem');
getPem('/path/to/key.p12')
  .then(pem => {
    console.log(pem); // '-----BEGIN RSA PRIVATE KEY-----\nMIICXQIBAAK...'
  })
  .catch(err => {
    console.error(err); // :(
  });

```

### callback style
```js
const {getPem} = require('google-p12-pem');
getPem('/path/to/key.p12', function(err, pem) {
  console.log(pem); // '-----BEGIN RSA PRIVATE KEY-----\nMIICXQIBAAK...'
});
```

### CLI style

``` sh
gp12-pem myfile.p12 > output.pem
```

## License
[MIT](LICENSE)

[david-image]: https://david-dm.org/google/google-p12-pem.svg
[david-url]: https://david-dm.org/google/google-p12-pem
[david-dev-image]: https://david-dm.org/google/google-p12-pem/dev-status.svg
[david-dev-url]: https://david-dm.org/google/google-p12-pem?type=dev
[npm-image]: https://img.shields.io/npm/v/google-p12-pem.svg
[npm-url]: https://www.npmjs.com/package/google-p12-pem
[snyk-image]: https://snyk.io/test/github/google/google-p12-pem/badge.svg
[snyk-url]: https://snyk.io/test/github/google/google-p12-pem
[travis-image]: https://travis-ci.org/google/google-p12-pem.svg?branch=master
[travis-url]: https://travis-ci.org/google/google-p12-pem
