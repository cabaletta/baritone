'use strict';
const assert = require('assert');
const sinon = require('sinon');

const parseCmds = require('../src/parseCmds');

const sandbox = sinon.createSandbox();

describe('parseCmds', () => {

    afterEach(() => {
        sandbox.restore();
    });

    it('returns a list of command objects', () => {
        const cmds = parseCmds([ 'echo test' ]);

        assert.deepStrictEqual(cmds, [
            {
                cmd: 'echo test',
                name: '',
                color: undefined
            }
        ]);
    });

    it('strips quotes', () => {
        const cmds = parseCmds([ '"echo test"' ]);

        assert.deepStrictEqual(cmds, [
            {
                cmd: 'echo test',
                name: '',
                color: undefined
            }
        ]);
    });

    it('assigns names', () => {
        const cmds = parseCmds([ 'echo test', 'echo test2' ], {
            names: 'echo-test,echo-test2'
        });

        assert.deepStrictEqual(cmds, [
            {
                cmd: 'echo test',
                name: 'echo-test',
                color: undefined
            },
            {
                cmd: 'echo test2',
                name: 'echo-test2',
                color: undefined
            }
        ]);
    });

    it('assigns names with custom separator', () => {
        const cmds = parseCmds([ 'echo test', 'echo test2' ], {
            names: 'echo-test|echo-test2',
            nameSeparator: '|'
        });

        assert.deepStrictEqual(cmds, [
            {
                cmd: 'echo test',
                name: 'echo-test',
                color: undefined
            },
            {
                cmd: 'echo test2',
                name: 'echo-test2',
                color: undefined
            }
        ]);
    });

    it('assigns colours', () => {
        const cmds = parseCmds([ 'echo test', 'echo test2' ], {
            prefixColors: 'blue'
        });

        assert.deepStrictEqual(cmds, [
            {
                cmd: 'echo test',
                name: '',
                color: 'blue'
            },
            {
                cmd: 'echo test2',
                name: '',
                color: undefined
            }
        ]);
    });

    it('expands npm: shortcut', () => {
        const cmds = parseCmds([ 'npm:watch:js' ]);

        assert.deepStrictEqual(cmds, [
            {
                cmd: 'npm run watch:js',
                name: 'watch:js',
                color: undefined
            }
        ]);
    });

    it('expands npm: shortcut with assigned name', () => {
        const cmds = parseCmds([ 'npm:watch:js' ], {
            names: 'js'
        });

        assert.deepStrictEqual(cmds, [
            {
                cmd: 'npm run watch:js',
                name: 'js',
                color: undefined
            }
        ]);
    });

    it('expands npm: shortcut with wildcard', () => {
        sandbox.stub(require('../src/pkgInfo'), 'getScripts').returns([
            'test', 'start', 'watch:js', 'watch:css', 'watch:node'
        ]);

        const cmds = parseCmds([ 'npm:watch:*' ]);

        assert.deepStrictEqual(cmds, [
            {
                cmd: 'npm run watch:js',
                name: 'js',
                color: undefined
            },
            {
                cmd: 'npm run watch:css',
                name: 'css',
                color: undefined
            },
            {
                cmd: 'npm run watch:node',
                name: 'node',
                color: undefined
            }
        ]);
    });

    it('expands npm: shortcut with wildcard and name prefix', () => {
        sandbox.stub(require('../src/pkgInfo'), 'getScripts').returns([
            'test', 'start', 'watch:js', 'watch:css', 'watch:node'
        ]);

        const cmds = parseCmds([ 'npm:watch:*' ], {
            names: 'w:'
        });

        assert.deepStrictEqual(cmds, [
            {
                cmd: 'npm run watch:js',
                name: 'w:js',
                color: undefined
            },
            {
                cmd: 'npm run watch:css',
                name: 'w:css',
                color: undefined
            },
            {
                cmd: 'npm run watch:node',
                name: 'w:node',
                color: undefined
            }
        ]);
    });

    it('applies prefix colors to expanded commands', () => {
        sandbox.stub(require('../src/pkgInfo'), 'getScripts').returns([
            'test', 'start', 'watch:js', 'watch:css', 'watch:node'
        ]);

        const cmds = parseCmds([ 'npm:watch:*' ], {
            prefixColors: 'blue,magenta,cyan'
        });

        assert.deepStrictEqual(cmds, [
            {
                cmd: 'npm run watch:js',
                name: 'js',
                color: 'blue'
            },
            {
                cmd: 'npm run watch:css',
                name: 'css',
                color: 'magenta'
            },
            {
                cmd: 'npm run watch:node',
                name: 'node',
                color: 'cyan'
            }
        ]);
    });
});
