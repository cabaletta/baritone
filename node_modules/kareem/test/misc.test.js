'use strict';

const assert = require('assert');
const Kareem = require('../');

describe('hasHooks', function() {
  it('returns false for toString (Automattic/mongoose#6538)', function() {
    const k = new Kareem();
    assert.ok(!k.hasHooks('toString'));
  });
});

describe('merge', function() {
  it('handles async pres if source doesnt have them', function() {
    const k1 = new Kareem();
    k1.pre('cook', true, function(next, done) {
      execed.first = true;
      setTimeout(
        function() {
          done('error!');
        },
        5);

      next();
    });

    assert.equal(k1._pres.get('cook').numAsync, 1);

    const k2 = new Kareem();
    const k3 = k2.merge(k1);
    assert.equal(k3._pres.get('cook').numAsync, 1);
  });
});
