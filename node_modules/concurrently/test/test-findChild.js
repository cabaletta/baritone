'use strict';

const assert = require('assert');

const findChild = require('../src/findChild');

describe('findChild', () => {

    const aChild = { pid: '111' };
    const bChild = { pid: '222' };

    const children = [ aChild, bChild ];

    const childrenInfo = {
        111: {
            index: 0,
            name: 'a child'
        },
        222: {
            index: 1,
            name: 'b child'
        }
    };

    it('finds child by index', () => {
        const child = findChild('1', children, childrenInfo);
        assert.strictEqual(child, bChild);
    });

    it('finds child by name', () => {
        const child = findChild('a child', children, childrenInfo);
        assert.strictEqual(child, aChild);
    });

    it('returns undefined when no matching child found', () => {
        const child = findChild('no child', children, childrenInfo);
        assert.strictEqual(child, undefined);
    });
});
