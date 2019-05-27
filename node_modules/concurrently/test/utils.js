'use strict';
const childProcess = require('child_process');
const _ = require('lodash');
const readline = require('readline');
const shellQuote = require('shell-quote');

// If true, output of commands are shown
const DEBUG_TESTS = process.env.DEBUG_TESTS === 'true';

function run(cmd, opts) {
    opts = _.merge({
        // If set to a function, it will be called for each line
        // written to the child process's stdout as (line, child)
        onOutputLine: undefined,
        onErrorLine: undefined
    }, opts);

    let child;
    const parts = shellQuote.parse(cmd);
    try {
        child = childProcess.spawn(_.head(parts), _.tail(parts), {
            stdio: DEBUG_TESTS && !opts.onOutputLine ? 'inherit': null,
        });
    } catch (e) {
        return Promise.reject(e);
    }

    if (opts.onOutputLine) {
        readLines(child, opts.onOutputLine);
    }

    if (opts.onErrorLine) {
        readLines(child, opts.onErrorLine, 'stderr');
    }

    readLines(child, (l) => { console.log(l); }, 'stderr');

    return new Promise(function(resolve, reject) {
        child.on('error', function(err) {
            reject(err);
        });

        child.on('close', function(exitCode) {
            resolve(exitCode);
        });
    });
}

function readLines(child, callback, src) {
    src = src || 'stdout';

    const rl = readline.createInterface({
        input: child[src],
        output: null
    });

    rl.on('line', function(line) {
        if (DEBUG_TESTS) {
            console.log(line);
        }

        callback(line, child);
    });
}

module.exports = {
    run: run
};
