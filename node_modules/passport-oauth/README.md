# Passport-OAuth

General-purpose OAuth 1.0 and OAuth 2.0 authentication strategies for [Passport](https://github.com/jaredhanson/passport).

This module lets you authenticate using OAuth in your Node.js applications.
By plugging into Passport, OAuth authentication can be easily and unobtrusively
integrated into any application or framework that supports
[Connect](http://www.senchalabs.org/connect/)-style middleware, including
[Express](http://expressjs.com/).

Note that this strategy provides generic OAuth support.  In many cases, a
provider-specific strategy can be used instead, which cuts down on unnecessary
configuration, and accommodates any provider-specific quirks.  See the list
below for supported providers.

Developers who need to implement authentication against an OAuth provider that
is not already supported are encouraged to sub-class this strategy.  If you
choose to open source the new provider-specific strategy, send me a message and
I will update the list.

## Installation

    $ npm install passport-oauth

## Strategies using OAuth

<table>
  <thead>
    <tr><th>Strategy</th><th>OAuth Version</th>
  </thead>
  <tbody>
    <tr><td><a href="https://github.com/jaredhanson/passport-37signals">37signals</a></td><td>2.0</td></tr>
    <tr><td><a href="https://github.com/allplayers/passport-allplayers">AllPlayers.com</a></td><td>1.0</td></tr>
    <tr><td><a href="https://github.com/jaredhanson/passport-angellist">AngelList</a></td><td>2.0</td></tr>
    <tr><td><a href="https://github.com/jaredhanson/passport-bitbucket">Bitbucket</a></td><td>1.0a</td></tr>
    <tr><td><a href="https://github.com/rajaraodv/passport-cloudfoundry">Cloud Foundry (UAA)</a></td><td>2.0</td></tr>
    <tr><td><a href="https://github.com/jaredhanson/passport-digg">Digg</a></td><td>1.0a</td></tr>
    <tr><td><a href="https://github.com/jaredhanson/passport-dropbox">Dropbox</a></td><td>1.0</td></tr>
    <tr><td><a href="https://github.com/jaredhanson/passport-dwolla">Dwolla</a></td><td>2.0</td></tr>
    <tr><td><a href="https://github.com/jaredhanson/passport-evernote">Evernote</a></td><td>1.0a</td></tr>
    <tr><td><a href="https://github.com/jaredhanson/passport-facebook">Facebook</a></td><td>2.0</td></tr>
    <tr><td><a href="https://github.com/jaredhanson/passport-fitbit">Fitbit</a></td><td>1.0a</td></tr>
    <tr><td><a href="https://github.com/jaredhanson/passport-flickr">Flickr</a></td><td>1.0a</td></tr>
    <tr><td><a href="https://github.com/jaredhanson/passport-foursquare">Foursquare</a></td><td>2.0</td></tr>
    <tr><td><a href="https://github.com/jaredhanson/passport-geoloqi">Geoloqi</a></td><td>2.0</td></tr>
    <tr><td><a href="https://github.com/jaredhanson/passport-github">GitHub</a></td><td>2.0</td></tr>
    <tr><td><a href="https://github.com/jaredhanson/passport-goodreads">Goodreads</a></td><td>1.0</td></tr>
    <tr><td><a href="https://github.com/jaredhanson/passport-google-oauth">Google</a></td><td>1.0a, 2.0</td></tr>
    <tr><td><a href="https://github.com/jaredhanson/passport-gowalla">Gowalla</a></td><td>2.0</td></tr>
    <tr><td><a href="https://github.com/jaredhanson/passport-instagram">Instagram</a></td><td>2.0</td></tr>
    <tr><td><a href="https://github.com/jaredhanson/passport-justintv">Justin.tv</a></td><td>1.0a</td></tr>
    <tr><td><a href="https://github.com/jaredhanson/passport-linkedin">LinkedIn</a></td><td>1.0a</td></tr>
    <tr><td><a href="https://github.com/jaredhanson/passport-meetup">Meetup</a></td><td>1.0a</td></tr>
    <tr><td><a href="https://github.com/jaredhanson/passport-netflix">Netflix</a></td><td>1.0a</td></tr>
    <tr><td><a href="https://github.com/jaredhanson/passport-ohloh">Ohloh</a></td><td>1.0</td></tr>
    <tr><td><a href="https://github.com/jaredhanson/passport-openstreetmap">OpenStreetMap</a></td><td>1.0a</td></tr>
    <tr><td><a href="https://github.com/jaredhanson/passport-picplz">picplz</a></td><td>2.0</td></tr>
    <tr><td><a href="https://github.com/jaredhanson/passport-rdio">Rdio</a></td><td>1.0a</td></tr>
    <tr><td><a href="https://github.com/jaredhanson/passport-readability">Readability</a></td><td>1.0a</td></tr>
    <tr><td><a href="https://github.com/jaredhanson/passport-runkeeper">RunKeeper</a></td><td>2.0</td></tr>
    <tr><td><a href="https://github.com/jaredhanson/passport-smugmug">SmugMug</a></td><td>1.0a</td></tr>
    <tr><td><a href="https://github.com/jaredhanson/passport-soundcloud">SoundCloud</a></td><td>2.0</td></tr>
    <tr><td><a href="https://github.com/jaredhanson/passport-tripit">TripIt</a></td><td>1.0</td></tr>
    <tr><td><a href="https://github.com/jaredhanson/passport-tumblr">Tumblr</a></td><td>1.0a</td></tr>
    <tr><td><a href="https://github.com/jaredhanson/passport-twitter">Twitter</a></td><td>1.0a</td></tr>
    <tr><td><a href="https://github.com/jaredhanson/passport-vimeo">Vimeo</a></td><td>1.0a</td></tr>
    <tr><td><a href="https://github.com/jaredhanson/passport-windowslive">Windows Live</a></td><td>2.0</td></tr>
    <tr><td><a href="https://github.com/jaredhanson/passport-yahoo-oauth">Yahoo!</a></td><td>1.0a</td></tr>
    <tr><td><a href="https://github.com/jaredhanson/passport-yammer">Yammer</a></td><td>2.0</td></tr>
  </tbody>
</table>

## Tests

    $ npm install --dev
    $ make test

[![Build Status](https://secure.travis-ci.org/jaredhanson/passport-oauth.png)](http://travis-ci.org/jaredhanson/passport-oauth)


## Credits

  - [Jared Hanson](http://github.com/jaredhanson)

## License

[The MIT License](http://opensource.org/licenses/MIT)

Copyright (c) 2011-2013 Jared Hanson <[http://jaredhanson.net/](http://jaredhanson.net/)>
