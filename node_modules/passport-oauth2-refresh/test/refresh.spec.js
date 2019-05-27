'use strict';

require('mocha');

var chai = require('chai'),
    sinon = require('sinon'),
    expect = chai.expect,
    AuthTokenRefresh = require('../lib/refresh.js');

chai.use(require('sinon-chai'));

// Dummy OAuth2 object
function OAuth2(clientId, clientSecret, baseSite, authorizeUrl, accessTokenUrl) {
  this._accessTokenUrl = accessTokenUrl;
}

// Add dummy method
OAuth2.prototype.getOAuthAccessToken = new Function();

// Makes it easy to invocate in the specs
var newOAuth2 = function(accessTokenUrl) {
  return new OAuth2(null, null, null, null, accessTokenUrl);
};

describe('Auth token refresh', function() {

  beforeEach(function() {
    AuthTokenRefresh._strategies = {};
  });

  describe('use', function() {
    it('should add a strategy with an explicitly defined name', function() {
      var strategy = {
        name: 'internal_name',
        _oauth2: newOAuth2()
      };

      AuthTokenRefresh.use('explicit_name', strategy);

      expect(AuthTokenRefresh._strategies.explicit_name.strategy).to.equal(strategy);expect(AuthTokenRefresh._strategies.strategy).to.be.undefined;
    });

    it('should add a strategy without an explicitly defined name', function() {
      var strategy = {
        name: 'internal_name',
        _oauth2: newOAuth2()
      };

      AuthTokenRefresh.use(strategy);

      expect(AuthTokenRefresh._strategies.internal_name.strategy).to.equal(strategy);
    });

    it('should add a strategy with a refreshURL', function() {
      var strategy = {
        name: 'test_strategy',
        _refreshURL: 'refreshURL',
        _oauth2: newOAuth2('accessTokenUrl')
      };

      AuthTokenRefresh.use(strategy);
      expect(AuthTokenRefresh._strategies.test_strategy.strategy).to.equal(strategy);
      expect(AuthTokenRefresh._strategies.test_strategy.refreshOAuth2._accessTokenUrl).to.equal('refreshURL');
    });

    it('should add a strategy without a refreshURL', function() {
      var strategy = {
        name: 'test_strategy',
        _oauth2: newOAuth2('accessTokenUrl')
      };

      AuthTokenRefresh.use(strategy);
      expect(AuthTokenRefresh._strategies.test_strategy.strategy).to.equal(strategy);
      expect(AuthTokenRefresh._strategies.test_strategy.refreshOAuth2._accessTokenUrl).to.equal('accessTokenUrl');
    });

    it('should create a new oauth2 object with the same prototype as the strategy\'s _oauth2 object', function() {
      var strategyOAuth2 = newOAuth2();
      var strategy = {
        name: 'test_strategy',
        _oauth2: strategyOAuth2
      };

      AuthTokenRefresh.use(strategy);
      expect(AuthTokenRefresh._strategies.test_strategy.refreshOAuth2).to.not.equal(strategyOAuth2);
      expect(AuthTokenRefresh._strategies.test_strategy.refreshOAuth2).to.be.instanceof(OAuth2);

    });

    it('should not add a null strategy', function() {
      var strategy = null;
      var fn = function() {
        AuthTokenRefresh.use(strategy);
      };

      expect(fn).to.throw(Error, 'Cannot register: strategy is null');
    });

    it('should not add a strategy with no name', function() {
      var strategy = {
        name: '',
        _oauth2: newOAuth2()
      };

      var fn = function() {
        AuthTokenRefresh.use(strategy);
      };

      expect(fn).to.throw(Error, 'Cannot register: name must be specified, or strategy must include name');
    });

    it('should not add a non-OAuth 2.0 strategy', function() {
      var strategy = {
        name: 'test_strategy',
        _oauth2: null
      };

      var fn = function() {
        AuthTokenRefresh.use(strategy);
      };

      expect(fn).to.throw(Error, 'Cannot register: not an OAuth2 strategy');
    });

    it('should use the default getOAuthAccessToken function if not overwritten by strategy', function() {
      var strategy = {
        name: 'test_strategy',
        _oauth2: newOAuth2()
      };

      AuthTokenRefresh.use(strategy);
      expect(AuthTokenRefresh._strategies.test_strategy.refreshOAuth2.getOAuthAccessToken).to.equal(OAuth2.prototype.getOAuthAccessToken);
    });

    it('should use the overwritten getOAuthAccessToken function if overwritten by strategy', function() {
      var strategy = {
        name: 'test_strategy',
        _oauth2: newOAuth2()
      };

      strategy._oauth2.getOAuthAccessToken = new Function();

      AuthTokenRefresh.use(strategy);
      expect(AuthTokenRefresh._strategies.test_strategy.refreshOAuth2.getOAuthAccessToken).to.equal(strategy._oauth2.getOAuthAccessToken);
      expect(AuthTokenRefresh._strategies.test_strategy.refreshOAuth2.getOAuthAccessToken).not.equal(OAuth2.prototype.getOAuthAccessToken);
    });
  });

  describe('has', function() {
    it('should return true if a strategy has been added', function() {
      var strategy = {
        name: 'test_strategy',
        _oauth2: newOAuth2()
      };

      AuthTokenRefresh.use(strategy);
      expect(AuthTokenRefresh.has('test_strategy')).to.be.true;
    });

    it('should return false if a strategy has not been added', function() {
      expect(AuthTokenRefresh.has('test_strategy')).to.be.false;
    });
  });

  describe('request new access token', function() {
    it('should refresh an access token', function() {
      var getOAuthAccessTokenSpy = sinon.spy();
      var done = sinon.spy();

      AuthTokenRefresh._strategies = {
        test_strategy: {
          refreshOAuth2: {
            getOAuthAccessToken: getOAuthAccessTokenSpy
          }
        }
      };

      AuthTokenRefresh.requestNewAccessToken('test_strategy', 'refresh_token', done);

      expect(getOAuthAccessTokenSpy).to.have.been.calledWith('refresh_token', { grant_type: 'refresh_token' }, done);
    });

    it('should refresh a new access token with extra params', function() {
      var getOAuthAccessTokenSpy = sinon.spy();
      var done = sinon.spy();

      AuthTokenRefresh._strategies = {
        test_strategy: {
          refreshOAuth2: {
            getOAuthAccessToken: getOAuthAccessTokenSpy
          }
        }
      };

      AuthTokenRefresh.requestNewAccessToken('test_strategy', 'refresh_token', { some: 'extra_param' }, done);

      expect(getOAuthAccessTokenSpy).to.have.been.calledWith('refresh_token', { grant_type: 'refresh_token', some: 'extra_param' }, done);
    });

    it('should not refresh if the strategy was not previously registered', function() {
      var done = sinon.spy();
      var expected = sinon.match.instanceOf(Error).and(sinon.match.has('message', 'Strategy was not registered to refresh a token'));

      AuthTokenRefresh.requestNewAccessToken('test_strategy', 'refresh_token', done);

      expect(done).to.have.been.calledWith(expected);
    });
  });
});
