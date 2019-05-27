'use strict';
// Test basic usage of cli

const path = require('path');
const assert = require('assert');
const run = require('./utils').run;
const IS_WINDOWS = /^win/.test(process.platform);

// Note: Set the DEBUG_TESTS environment variable to `true` to see output of test commands.

const TEST_DIR = 'dir/';

// Abs path to test directory
const testDir = path.resolve(__dirname);
process.chdir(path.join(testDir, '..'));

describe('concurrently', function() {
    this.timeout(5000);

    it('help should be successful', () => {
        return run('node ./src/main.js --help')
            .then(function(exitCode) {
                // exit code 0 means success
                assert.strictEqual(exitCode, 0);
            });
    });

    it('version should be successful', () => {
        return run('node ./src/main.js -V')
            .then(function(exitCode) {
                assert.strictEqual(exitCode, 0);
            });
    });

    it('two successful commands should exit 0', () => {
        return run('node ./src/main.js "echo test" "echo test"')
            .then(function(exitCode) {
                assert.strictEqual(exitCode, 0);
            });
    });

    it('at least one unsuccessful commands should exit non-zero', () => {
        return run('node ./src/main.js "echo test" "nosuchcmd" "echo test"')
            .then(function(exitCode) {
                assert.notStrictEqual(exitCode, 0);
            });
    });

    it('--kill-others should kill other commands if one dies', () => {
        return run('node ./src/main.js --kill-others "sleep 1" "echo test" "sleep 0.1 && nosuchcmd"')
            .then(function(exitCode) {
                assert.notStrictEqual(exitCode, 0);
            });
    });

    it('--kill-others-on-fail should kill other commands if one exits with non-zero status code', () => {
        return run('node ./src/main.js --kill-others-on-fail "sleep 1" "exit 1" "sleep 1"')
            .then(function(exitCode) {
                assert.notStrictEqual(exitCode, 0);
            });
    });

    it('--kill-others-on-fail should NOT kill other commands if none of them exits with non-zero status code', (done) => {
        const readline = require('readline');
        let exits = 0;
        let sigtermInOutput = false;

        run('node ./src/main.js --kill-others-on-fail "echo killTest1" "echo killTest2" "echo killTest3"', {
            onOutputLine: function(line) {
                if (/SIGTERM/.test(line)) {
                    sigtermInOutput = true;
                }

                // waiting for exits
                if (/killTest\d$/.test(line)) {
                    exits++;
                }
            }
        }).then(function() {
            if (sigtermInOutput) {
                done(new Error('There was a "SIGTERM" in console output'));
            } else if (exits !== 3) {
                done(new Error('There was wrong number of echoes(' + exits + ') from executed commands'));
            } else {
                done();
            }
        });
    });

    it('--success=first should return first exit code', () => {
        return run('node ./src/main.js -k --success first "echo test" "sleep 0.1 && nosuchcmd"')
            // When killed, sleep returns null exit code
            .then(function(exitCode) {
                assert.strictEqual(exitCode, 0);
            });
    });

    it('--success=last should return last exit code', () => {
        // When killed, sleep returns null exit code
        return run('node ./src/main.js -k --success last "echo test" "sleep 0.1 && nosuchcmd"')
            .then(function(exitCode) {
                assert.notStrictEqual(exitCode, 0);
            });
    });

    it('&& nosuchcmd should return non-zero exit code', () => {
        return run('node ./src/main.js "echo 1 && nosuchcmd" "echo 1 && nosuchcmd" ')
            .then(function(exitCode) {
                assert.strictEqual(exitCode, 1);
            });
    });

    it('--prefix-colors should handle non-existent colors without failing', () => {
        return run('node ./src/main.js -c "not.a.color" "echo colors"')
            .then(function(exitCode) {
                assert.strictEqual(exitCode, 0);
            });
    });

    it('--prefix can contain PID', () => {
        const collectedLines = [];
        return run('node ./src/main.js --prefix pid "echo one" "echo two"', {
            onOutputLine(line) {
                collectedLines.push(line);
            }
        }).then(() => {
            assert.ok(collectedLines.every(line => /^\[\d+\] /.test(line)));
        });
    });

    it('--prefix can contain command itself', () => {
        const collectedLines = [];
        return run('node ./src/main.js --prefix "[{command}]" "echo one" "echo two"', {
            onOutputLine(line) {
                collectedLines.push(line);
            }
        }).then(() => {
            assert.ok(collectedLines.every(line => /^\[echo (one|two)\] /.test(line)));
        });
    });

    it('--prefix can contain time', () => {
        const collectedLines = [];
        return run('node ./src/main.js --prefix time "echo one" "echo two"', {
            onOutputLine(line) {
                collectedLines.push(line);
            }
        }).then(() => {
            assert.ok(collectedLines.every(line => /^\[\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}\] /.test(line)));
        });
    });

    it('--prefix should default to "index"', () => {
        const collectedLines = [];

        return run('node ./src/main.js "echo one" "echo two"', {
            onOutputLine: (line) => {
                if (/(one|two)$/.exec(line)) {
                    collectedLines.push(line);
                }
            }
        })
            .then(function(exitCode) {
                assert.strictEqual(exitCode, 0);

                collectedLines.sort();
                assert.deepEqual(collectedLines, [
                    '[0] one',
                    '[1] two'
                ]);
            });
    });

    it('--names should set a different default prefix', () => {
        const collectedLines = [];

        return run('node ./src/main.js -n aa,bb "echo one" "echo two"', {
            onOutputLine: (line) => {
                if (/(one|two)$/.exec(line)) {
                    collectedLines.push(line);
                }
            }
        })
            .then(function(exitCode) {
                assert.strictEqual(exitCode, 0);

                collectedLines.sort();
                assert.deepEqual(collectedLines, [
                    '[aa] one',
                    '[bb] two'
                ]);
            });
    });

    it('--allow-restart should restart a proccess with non-zero exit code', (done) => {
        const readline = require('readline');
        let exitedWithOne = false;
        let restarted = false;

        run('node ./src/main.js --allow-restart "sleep 0.1 && exit 1" "sleep 1"', {
            pipe: false,
            onOutputLine: (line) => {
                const re = /exited with code (.+)/.exec(line);
                if (re && re[1] === '1') {
                    exitedWithOne = true;
                }

                if (/restarted/.test(line)) {
                    restarted = true;
                }
            }
        }).then(function() {
            if (exitedWithOne && restarted) {
                done();
            } else {
                done(new Error('No restarted process exited with code 1'));
            }
        });
    });

    it('--restart-after=n should restart a proccess after n miliseconds', (done) => {
        const readline = require('readline');
        let start, end;

        run('node ./src/main.js --allow-restart --restart-after 300 "exit 1" "sleep 1"', {
            pipe: false,
            onOutputLine: (line) => {
                if (!start && /exited with code (.+)/.test(line)) {
                    start = new Date().getTime();
                }

                if (!end && /restarted/.test(line)) {
                    end = new Date().getTime();
                }
            }
        }).then(function() {
            // we accept 100 miliseconds of error
            if (end - start >= 300 && end - start < 400) {
                done();
            } else {
                done(new Error('No restarted process after 300 miliseconds'));
            }
        });
    });
    it('--restart-tries=n should restart a proccess at most n times', (done) => {
        const readline = require('readline');
        let restartedTimes = 0;

        run('node ./src/main.js --allow-restart --restart-tries 2 "exit 1" "sleep 1"', {
            pipe: false,
            onOutputLine: (line) => {
                if (/restarted/.test(line)) {
                    restartedTimes++;
                }
            }
        }).then(function() {
            if (restartedTimes === 2) {
                done();
            } else {
                done(new Error('No restarted process twice'));
            }
        });
    });

    ['SIGINT', 'SIGTERM'].forEach((signal) => {
        if (IS_WINDOWS) {
            console.log('IS_WINDOWS=true');
            console.log('Skipping SIGINT/SIGTERM propagation tests ..');
            return;
        }

        it('killing it with ' + signal + ' should propagate the signal to the children', function(done) {
            const readline = require('readline');
            let waitingStart = 2;
            let waitingSignal = 2;

            function waitForSignal(cb) {
                if (waitingSignal) {
                    setTimeout(waitForSignal, 100);
                } else {
                    cb();
                }
            }

            run('node ./src/main.js "node ./test/support/signal.js" "node ./test/support/signal.js"', {
                onOutputLine: function(line, child) {
                    // waiting for startup
                    if (/STARTED/.test(line)) {
                        waitingStart--;
                    }
                    if (!waitingStart) {
                        // both processes are started
                        child.kill(signal);
                    }

                    // waiting for signal
                    if (new RegExp(signal).test(line)) {
                        waitingSignal--;
                    }
                }
            }).then(function() {
                waitForSignal(done);
            });
        });
    });

    it('sends input to default stdin target process', (done) => {
        let echoed = false;
        run('node ./src/main.js "node ./test/support/read-echo.js"', {
            onOutputLine: (line, child) => {
                if (/READING/.test(line)) {
                    child.stdin.write('stop\n');
                }

                if (/\[\d+\] stop/.test(line)) {
                    echoed = true;
                }
            }
        })
            .then(() => {
                assert(echoed);
            })
            .then(done, done);
    });

    it('sends input to specified default stdin target process', (done) => {
        let echoed = false;
        run('node ./src/main.js --default-input-target 1 "echo test" "node ./test/support/read-echo.js"', {
            onOutputLine: (line, child) => {
                if (/READING/.test(line)) {
                    child.stdin.write('stop\n');
                }

                if (/\[1\] stop/.test(line)) {
                    echoed = true;
                }
            }
        })
            .then(() => {
                assert(echoed);
            })
            .then(done, done);
    });

    it('sends input to child specified by index', (done) => {
        let echoed = false;
        run('node ./src/main.js "echo test" "node ./test/support/read-echo.js"', {
            onOutputLine: (line, child) => {
                if (/READING/.test(line)) {
                    child.stdin.write('1:stop\n');
                }

                if (/\[1\] stop/.test(line)) {
                    echoed = true;
                }
            }
        })
            .then(() => {
                assert(echoed);
            })
            .then(done, done);
    });

    it('emits error when specified read stream is not found', (done) => {
        let errorEmitted = false;
        run('node ./src/main.js "echo test" "node ./test/support/read-echo.js"', {
            onOutputLine: (line, child) => {
                if (/READING/.test(line)) {
                    child.stdin.write('2:stop\n');
                }

                if ('Unable to find command [2]' === line.trim()) {
                    errorEmitted = true;

                    // Stop read process to continue the test
                    child.stdin.write('1:stop\n');
                }
            }
        })
            .then(() => {
                assert(errorEmitted);
            })
            .then(done, done);
    });

    it('should expand npm: command shortcuts', (done) => {
        let echo1 = false;
        let echo2 = false;
        run('node ./src/main.js "npm:echo-test" "npm:echo -- testarg"', {
            onOutputLine: function(line, child) {
                if (line === '[echo-test] test') {
                    echo1 = true;
                } else if (line === '[echo] testarg') {
                    echo2 = true;
                }
            }
        })
            .then((exitCode) => {
                assert.strictEqual(exitCode, 0);
                assert.ok(echo1);
                assert.ok(echo2);
            })
            .then(done, done);
    });

    it('expands npm run shortcut wildcards', (done) => {
        let echoBeep = false;
        let echoBoop = false;
        run('node ./src/main.js "npm:echo-sound-*"', {
            onOutputLine: (line, child) => {
                if (line === '[beep] beep') {
                    echoBeep = true;
                } else if (line === '[boop] boop') {
                    echoBoop = true;
                }
            }
        })
            .then((exitCode) => {
                assert.strictEqual(exitCode, 0);
                assert.ok(echoBeep);
                assert.ok(echoBoop);
            })
            .then(done, done);
    });
});

function resolve(relativePath) {
    return path.join(testDir, relativePath);
}
