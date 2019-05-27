# passport-oauth2

[![Build](https://img.shields.io/travis/jaredhanson/passport-oauth2.svg)](https://travis-ci.org/jaredhanson/passport-oauth2)
[![Coverage](https://img.shields.io/coveralls/jaredhanson/passport-oauth2.svg)](https://coveralls.io/r/jaredhanson/passport-oauth2)
[![Quality](https://img.shields.io/codeclimate/github/jaredhanson/passport-oauth2.svg?label=quality)](https://codeclimate.com/github/jaredhanson/passport-oauth2)
[![Dependencies](https://img.shields.io/david/jaredhanson/passport-oauth2.svg)](https://david-dm.org/jaredhanson/passport-oauth2)


General-purpose OAuth 2.0 authentication strategy for [Passport](http://passportjs.org/).

This module lets you authenticate using OAuth 2.0 in your Node.js applications.
By plugging into Passport, OAuth 2.0 authentication can be easily and
unobtrusively integrated into any application or framework that supports
[Connect](http://www.senchalabs.org/connect/)-style middleware, including
[Express](http://expressjs.com/).

Note that this strategy provides generic OAuth 2.0 support.  In many cases, a
provider-specific strategy can be used instead, which cuts down on unnecessary
configuration, and accommodates any provider-specific quirks.  See the
[list](https://github.com/jaredhanson/passport/wiki/Strategies) for supported
providers.

Developers who need to implement authentication against an OAuth 2.0 provider
that is not already supported are encouraged to sub-class this strategy.  If you
choose to open source the new provider-specific strategy, please add it to the
list so other people can find it.

## Install

    $ npm install passport-oauth2

## Usage

#### Configure Strategy

The OAuth 2.0 authentication strategy authenticates users using a third-party
account and OAuth 2.0 tokens.  The provider's OAuth 2.0 endpoints, as well as
the client identifer and secret, are specified as options.  The strategy
requires a `verify` callback, which receives an access token and profile,
and calls `cb` providing a user.

```js
passport.use(new OAuth2Strategy({
    authorizationURL: 'https://www.example.com/oauth2/authorize',
    tokenURL: 'https://www.example.com/oauth2/token',
    clientID: EXAMPLE_CLIENT_ID,
    clientSecret: EXAMPLE_CLIENT_SECRET,
    callbackURL: "http://localhost:3000/auth/example/callback"
  },
  function(accessToken, refreshToken, profile, cb) {
    User.findOrCreate({ exampleId: profile.id }, function (err, user) {
      return cb(err, user);
    });
  }
));
```

#### Authenticate Requests

Use `passport.authenticate()`, specifying the `'oauth2'` strategy, to
authenticate requests.

For example, as route middleware in an [Express](http://expressjs.com/)
application:

```js
app.get('/auth/example',
  passport.authenticate('oauth2'));

app.get('/auth/example/callback',
  passport.authenticate('oauth2', { failureRedirect: '/login' }),
  function(req, res) {
    // Successful authentication, redirect home.
    res.redirect('/');
  });
```

## Related Modules

- [passport-oauth1](https://github.com/jaredhanson/passport-oauth1) — OAuth 1.0 authentication strategy
- [passport-http-bearer](https://github.com/jaredhanson/passport-http-bearer) — Bearer token authentication strategy for APIs
- [OAuth2orize](https://github.com/jaredhanson/oauth2orize) — OAuth 2.0 authorization server toolkit

## Contributing

#### Tests

The test suite is located in the `test/` directory.  All new features are
expected to have corresponding test cases.  Ensure that the complete test suite
passes by executing:

```bash
$ make test
```

#### Coverage

All new feature development is expected to have test coverage.  Patches that
increse test coverage are happily accepted.  Coverage reports can be viewed by
executing:

```bash
$ make test-cov
$ make view-cov
```

## Support

#### Funding

This software is provided to you as open source, free of charge.  The time and
effort to develop and maintain this project is dedicated by [@jaredhanson](https://github.com/jaredhanson).
If you (or your employer) benefit from this project, please consider a financial
contribution.  Your contribution helps continue the efforts that produce this
and other open source software.

Funds are accepted via [PayPal](https://paypal.me/jaredhanson), [Venmo](https://venmo.com/jaredhanson),
and [other](http://jaredhanson.net/pay) methods.  Any amount is appreciated.

## License

[The MIT License](http://opensource.org/licenses/MIT)

Copyright (c) 2011-2016 Jared Hanson <[http://jaredhanson.net/](http://jaredhanson.net/)>
