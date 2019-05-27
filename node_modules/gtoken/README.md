# node-gtoken

[![NPM Version][npm-image]][npm-url]
[![CircleCI][circle-image]][circle-url]
[![Dependency Status][david-image]][david-url]
[![devDependency Status][david-dev-image]][david-dev-url]
[![Known Vulnerabilities][snyk-image]][snyk-url]
[![codecov][codecov-image]][codecov-url]
[![Greenkeeper badge][greenkeeper-image]][greenkeeper-url]
[![style badge][gts-image]][gts-url]

Node.js Google Authentication Service Account Tokens

## Installation

``` sh
npm install gtoken
```

## Usage

### Use with a `.pem` or `.p12` key file:

``` js
const { GoogleToken } = require('gtoken');
const gtoken = new GoogleToken({
  keyFile: 'path/to/key.pem', // or path to .p12 key file
  email: 'my_service_account_email@developer.gserviceaccount.com',
  scope: ['https://scope1', 'https://scope2'] // or space-delimited string of scopes
});

gtoken.getToken(function(err, token) {
  if (err) {
    console.log(err);
    return;
  }
  console.log(token);
});
```

You can also use the async/await style API:

``` js
const token = await gtoken.getToken()
console.log(token);
```

Or use promises:

```js
gtoken.getToken()
  .then(token => {
    console.log(`Token: ${token}`)
  })
  .catch(e => console.error);
```

### Use with a service account `.json` key file:

``` js
const { GoogleToken } = require('gtoken');
const gtoken = new GoogleToken({
  keyFile: 'path/to/key.json',
  scope: ['https://scope1', 'https://scope2'] // or space-delimited string of scopes
});

gtoken.getToken(function(err, token) {
  if (err) {
    console.log(err);
    return;
  }
  console.log(token);
});
```

### Pass the private key as a string directly:

``` js
const key = '-----BEGIN RSA PRIVATE KEY-----\nXXXXXXXXXXX...';
const { GoogleToken } = require('gtoken');
const gtoken = new GoogleToken({
  email: 'my_service_account_email@developer.gserviceaccount.com',
  scope: ['https://scope1', 'https://scope2'], // or space-delimited string of scopes
  key: key
});
```

## Options

> Various options that can be set when creating initializing the `gtoken` object.

- `options.email or options.iss`: The service account email address.
- `options.scope`: An array of scope strings or space-delimited string of scopes.
- `options.sub`: The email address of the user requesting delegated access.
- `options.keyFile`: The filename of `.json` key, `.pem` key or `.p12` key.
- `options.key`: The raw RSA private key value, in place of using `options.keyFile`.

### .getToken(callback)

> Returns the cached token or requests a new one and returns it.

``` js
gtoken.getToken(function(err, token) {
  console.log(err || token);
  // gtoken.token value is also set
});
```

### .getCredentials('path/to/key.json')

> Given a keyfile, returns the key and (if available) the client email.

```js
const creds = await gtoken.getCredentials('path/to/key.json');
```

### Properties

> Various properties set on the gtoken object after call to `.getToken()`.

- `gtoken.token`: The access token.
- `gtoken.expiresAt`: The expiry date as milliseconds since 1970/01/01
- `gtoken.key`: The raw key value.
- `gtoken.rawToken`: Most recent raw token data received from Google.

### .hasExpired()

> Returns true if the token has expired, or token does not exist.

``` js
gtoken.getToken(function(err, token) {
  if(token) {
    gtoken.hasExpired(); // false
  }
});
```

### .revokeToken()

> Revoke the token if set.

``` js
gtoken.revokeToken(function(err) {
  if (err) {
    console.log(err);
    return;
  }
  console.log('Token revoked!');
});
```

## Downloading your private `.p12` key from Google

1. Open the [Google Developer Console][gdevconsole].
2. Open your project and under "APIs & auth", click Credentials.
3. Generate a new `.p12` key and download it into your project.

## Converting your `.p12` key to a `.pem` key

You can just specify your `.p12` file (with `.p12` extension) as the `keyFile` and it will automatically be converted to a `.pem` on the fly, however this results in a slight performance hit. If you'd like to convert to a `.pem` for use later, use OpenSSL if you have it installed.

``` sh
$ openssl pkcs12 -in key.p12 -nodes -nocerts > key.pem
```

Don't forget, the passphrase when converting these files is the string `'notasecret'`

## Changelog

### 1.2.2 -> 2.0.0
New features:
- API now supports callback and promise based workflows

Breaking changes:
- `GoogleToken` is now a class type, and must be instantiated.
- `GoogleToken.expires_at` renamed to `GoogleToken.expiresAt`
- `GoogleToken.raw_token` renamed to `GoogleToken.rawToken`
- `GoogleToken.token_expires` renamed to `GoogleToken.tokenExpires`

## License

[MIT](LICENSE)

[circle-image]: https://circleci.com/gh/google/node-gtoken.svg?style=svg
[circle-url]: https://circleci.com/gh/google/node-gtoken
[codecov-image]: https://codecov.io/gh/google/node-gtoken/branch/master/graph/badge.svg
[codecov-url]: https://codecov.io/gh/google/node-gtoken
[david-image]: https://david-dm.org/google/node-gtoken.svg
[david-url]: https://david-dm.org/google/node-gtoken
[david-dev-image]: https://david-dm.org/google/node-gtoken/dev-status.svg
[david-dev-url]: https://david-dm.org/google/node-gtoken?type=dev
[gdevconsole]: https://console.developers.google.com
[greenkeeper-image]: https://badges.greenkeeper.io/google/node-gtoken.svg
[greenkeeper-url]: https://greenkeeper.io/
[gts-image]: https://img.shields.io/badge/code%20style-Google%20%E2%98%82%EF%B8%8F-blue.svg
[gts-url]: https://www.npmjs.com/package/gts
[npm-image]: https://img.shields.io/npm/v/gtoken.svg
[npm-url]: https://npmjs.org/package/gtoken
[snyk-image]: https://snyk.io/test/github/google/node-gtoken/badge.svg
[snyk-url]: https://snyk.io/test/github/google/node-gtoken
