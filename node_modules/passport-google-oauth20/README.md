# passport-google-oauth20

[![Build](https://img.shields.io/travis/jaredhanson/passport-google-oauth2.svg)](https://travis-ci.org/jaredhanson/passport-google-oauth2)
[![Coverage](https://img.shields.io/coveralls/jaredhanson/passport-google-oauth2.svg)](https://coveralls.io/r/jaredhanson/passport-google-oauth2)
[![Quality](https://img.shields.io/codeclimate/github/jaredhanson/passport-google-oauth2.svg?label=quality)](https://codeclimate.com/github/jaredhanson/passport-google-oauth2)
[![Dependencies](https://img.shields.io/david/jaredhanson/passport-google-oauth2.svg)](https://david-dm.org/jaredhanson/passport-google-oauth2)


[Passport](http://passportjs.org/) strategy for authenticating with [Google](http://www.google.com/)
using the OAuth 2.0 API.

This module lets you authenticate using Google in your Node.js applications.
By plugging into Passport, Google authentication can be easily and
unobtrusively integrated into any application or framework that supports
[Connect](http://www.senchalabs.org/connect/)-style middleware, including
[Express](http://expressjs.com/).

## Install

    $ npm install passport-google-oauth20

## Usage

#### Create an Application

Before using `passport-google-oauth20`, you must register an application with
Google.  If you have not already done so, a new project can be created in the
[Google Developers Console](https://console.developers.google.com/).
Your application will be issued a client ID and client secret, which need to be
provided to the strategy.  You will also need to configure a redirect URI which
matches the route in your application.

#### Configure Strategy

The Google authentication strategy authenticates users using a Google account
and OAuth 2.0 tokens.  The client ID and secret obtained when creating an
application are supplied as options when creating the strategy.  The strategy
also requires a `verify` callback, which receives the access token and optional
refresh token, as well as `profile` which contains the authenticated user's
Google profile.  The `verify` callback must call `cb` providing a user to
complete authentication.

    var GoogleStrategy = require('passport-google-oauth20').Strategy;

    passport.use(new GoogleStrategy({
        clientID: GOOGLE_CLIENT_ID,
        clientSecret: GOOGLE_CLIENT_SECRET,
        callbackURL: "http://www.example.com/auth/google/callback"
      },
      function(accessToken, refreshToken, profile, cb) {
        User.findOrCreate({ googleId: profile.id }, function (err, user) {
          return cb(err, user);
        });
      }
    ));

#### Authenticate Requests

Use `passport.authenticate()`, specifying the `'google'` strategy, to
authenticate requests.

For example, as route middleware in an [Express](http://expressjs.com/)
application:

    app.get('/auth/google',
      passport.authenticate('google', { scope: ['profile'] }));

    app.get('/auth/google/callback', 
      passport.authenticate('google', { failureRedirect: '/login' }),
      function(req, res) {
        // Successful authentication, redirect home.
        res.redirect('/');
      });

## Examples

Developers using the popular [Express](http://expressjs.com/) web framework can
refer to an [example](https://github.com/passport/express-4.x-facebook-example)
as a starting point for their own web applications.  The example shows how to
authenticate users using Facebook.  However, because both Facebook and Google
use OAuth 2.0, the code is similar.  Simply replace references to Facebook with
corresponding references to Google.

## Contributing

#### Tests

The test suite is located in the `test/` directory.  All new features are
expected to have corresponding test cases.  Ensure that the complete test suite
passes by executing:

```bash
$ make test
```

#### Coverage

The test suite covers 100% of the code base.  All new feature development is
expected to maintain that level.  Coverage reports can be viewed by executing:

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

Copyright (c) 2012-2016 Jared Hanson <[http://jaredhanson.net/](http://jaredhanson.net/)>
