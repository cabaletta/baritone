#!/usr/bin/env node
'use strict';

const Rx = require('rx');
const path = require('path');
const formatDate = require('date-fns/format');
const program = require('commander');
const _ = require('lodash');
const treeKill = require('tree-kill');
const chalk = require('chalk');
const spawn = require('spawn-command');
const supportsColor = require('supports-color');
const IS_WINDOWS = /^win/.test(process.platform);

const findChild = require('./findChild.js');
const parseCmds = require('./parseCmds');

let config = {
    // Kill other processes if one dies
    killOthers: false,

    // Kill other processes if one exits with non zero status code
    killOthersOnFail: false,

    // Return success or failure of the 'first' child to terminate, the 'last' child,
    // or succeed only if 'all' children succeed
    success: 'all',

    // Prefix logging with pid
    // Possible values: 'pid', 'none', 'time', 'command', 'index', 'name'
    prefix: '',

    // List of custom names to be used in prefix template
    names: '',

    // What to split the list of custom names on
    nameSeparator: ',',

    // Comma-separated list of chalk color paths to use on prefixes.
    prefixColors: 'gray.dim',

    // moment/date-fns format
    timestampFormat: 'YYYY-MM-DD HH:mm:ss.SSS',

    // How many characters to display from start of command in prefix if
    // command is defined. Note that also '..' will be added in the middle
    prefixLength: 10,

    // By default, color output
    color: true,

    // If true, the output will only be raw output of processes, nothing more
    raw: false,

    // If true, the process restart when it exited with status code non-zero
    allowRestart: false,

    // By default, restart instantly
    restartAfter: 0,

    // By default, restart once
    restartTries: 1,

    // Default identifier for child to which input on stdin should be sent.
    defaultInputTarget: '0'
};

function main() {
    const firstBase = path.basename(process.argv[0]);
    const secondBase = path.basename(process.argv[1]);
    if (firstBase === 'concurrent' || secondBase === 'concurrent') {
        console.error('Warning: "concurrent" command is deprecated, use "concurrently" instead.\n');
    }

    parseArgs();
    config = mergeDefaultsWithArgs(config);

    const cmds = parseCmds(program.args, config);
    run(cmds);
}

function parseArgs() {
    program
        .version(require('../package.json').version)
        .usage('[options] <command ...>')
        .option(
            '-k, --kill-others',
            'kill other processes if one exits or dies'
        )
        .option(
            '--kill-others-on-fail',
            'kill other processes if one exits with non zero status code'
        )
        .option(
            '--no-color',
            'disable colors from logging'
        )
        .option(
            '-p, --prefix <prefix>',
            'prefix used in logging for each process.\n' +
            'Possible values: index, pid, time, command, name, none, or a template. Default: ' +
            'index or name (when --names is set). Example template: "{time}-{pid}"\n'
        )
        .option(
            '-n, --names <names>',
            'List of custom names to be used in prefix template.\n' +
            'Example names: "main,browser,server"\n'
        )
        .option(
            '--name-separator <char>',
            'The character to split <names> on.\n' +
            'Default: "' + config.nameSeparator + '". Example usage: ' +
            'concurrently -n "styles,scripts|server" --name-separator "|" <command ...>\n'
        )
        .option(
            '-c, --prefix-colors <colors>',
            'Comma-separated list of chalk colors to use on prefixes. If there are more commands than colors, the last color will be repeated.\n' +
            'Available modifiers: reset, bold, dim, italic, underline, inverse, hidden, strikethrough\n' +
            'Available colors: black, red, green, yellow, blue, magenta, cyan, white, gray\n' +
            'Available background colors: bgBlack, bgRed, bgGreen, bgYellow, bgBlue, bgMagenta, bgCyan, bgWhite\n' +
            'See https://www.npmjs.com/package/chalk for more information.\n' +
            'Default: "' + config.prefixColors + '". Example: "black.bgWhite,cyan,gray.dim"\n'
        )
        .option(
            '-t, --timestamp-format <format>',
            'specify the timestamp in moment/date-fns format. Default: ' +
            config.timestampFormat + '\n'
        )
        .option(
            '-r, --raw',
            'output only raw output of processes,' +
            ' disables prettifying and concurrently coloring'
        )
        .option(
            '-s, --success <first|last|all>',
            'Return exit code of zero or one based on the success or failure ' +
            'of the "first" child to terminate, the "last" child, or succeed ' +
            ' only if "all" child processes succeed. Default: ' +
            config.success + '\n'
        )
        .option(
            '-l, --prefix-length <length>',
            'limit how many characters of the command is displayed in prefix.\n' +
            'The option can be used to shorten long commands.\n' +
            'Works only if prefix is set to "command". Default: ' +
            config.prefixLength + '\n'
        )
        .option(
            '--allow-restart',
            'Restart a process which died. Default: ' +
            config.allowRestart + '\n'
        )
        .option(
            '--restart-after <miliseconds>',
            'delay time to respawn the process. Default: ' +
            config.restartAfter + '\n'
        )
        .option(
            '--restart-tries <times>',
            'limit the number of respawn tries. Default: ' +
            config.restartTries + '\n'

        )
        .option(
            '--default-input-target <identifier>',
            'identifier for child process to which input on ' +
            'stdin should be sent if not specified at start ' +
            'of input. Can be either the index or the name ' +
            'of the process. Default: ' + config.defaultInputTarget + '\n'
        );

    program.on('--help', function() {
        const help = [
            '  Input:',
            '',
            '  Input can be sent to any of the child processes using either the name or',
            '  index of the command followed by a colon. If no child identifier is',
            '  specified then the input will be sent to the child specified by the',
            '  `--default-input-target` option, which defaults to index 0.',
            '',
            '  Examples:',
            '',
            '   - Kill other processes if one exits or dies',
            '',
            '       $ concurrently --kill-others "grunt watch" "http-server"',
            '',
            '   - Kill other processes if one exits with non zero status code',
            '',
            '       $ concurrently --kill-others-on-fail "npm run build:client" "npm run build:server"',
            '',
            '   - Output nothing more than stdout+stderr of child processes',
            '',
            '       $ concurrently --raw "npm run watch-less" "npm run watch-js"',
            '',
            '   - Normal output but without colors e.g. when logging to file',
            '',
            '       $ concurrently --no-color "grunt watch" "http-server" > log',
            '',
            '   - Custom prefix',
            '',
            '       $ concurrently --prefix "{time}-{pid}" "npm run watch" "http-server"',
            '',
            '   - Custom names and colored prefixes',
            '',
            '       $ concurrently --names "HTTP,WATCH" -c "bgBlue.bold,bgMagenta.bold" "http-server" "npm run watch"',
            '',
            '   - Send input to default',
            '',
            '       $ concurrently "nodemon" "npm run watch-js"',
            '       rs  # Sends rs command to nodemon process',
            '',
            '   - Specify a default-input-target',
            '',
            '       $ concurrently --default-input-target 1 "npm run watch-js" nodemon',
            '       rs',
            '',
            '   - Send input to specific child identified by index',
            '',
            '       $ concurrently "npm run watch-js" nodemon',
            '       1:rs',
            '',
            '   - Send input to specific child identified by name',
            '',
            '       $ concurrently -n js,srv "npm run watch-js" nodemon',
            '       srv:rs',
            '',
            '   - Shortened NPM run commands',
            '',
            '       $ concurrently npm:watch-node npm:watch-js npm:watch-css',
            '',
            '   - Shortened NPM run command with wildcard',
            '',
            '       $ concurrently npm:watch-*',
            ''
        ];
        console.log(help.join('\n'));

        const url = 'https://github.com/kimmobrunfeldt/concurrently';
        console.log('  For more details, visit ' + url);
        console.log('');
    });

    program.parse(process.argv);
}

function mergeDefaultsWithArgs(config) {
    // This will pollute config object with other attributes from program too
    return _.merge(config, program);
}

function run(commands) {
    const childrenInfo = {};
    let lastPrefixColor = _.get(chalk, chalk.gray.dim);
    const children = _.map(commands, function(cmdInfo, index) {

        const spawnOpts = config.raw ? {stdio: 'inherit'} : {};
        if (IS_WINDOWS) {
            spawnOpts.detached = false;
        }
        if (supportsColor) {
            spawnOpts.env = Object.assign({FORCE_COLOR: supportsColor.level}, process.env);
        }

        const child = spawnChild(cmdInfo.cmd, spawnOpts);

        if (cmdInfo.color) {
            const prefixColorPath = cmdInfo.color;
            lastPrefixColor = _.get(chalk, prefixColorPath, chalk.gray.dim);
        }

        childrenInfo[child.pid] = {
            command: cmdInfo.cmd,
            index: index,
            name: cmdInfo.name,
            options: spawnOpts,
            restartTries: config.restartTries,
            prefixColor: lastPrefixColor
        };
        return child;
    });

    const streams = toStreams(children);

    handleChildEvents(streams, children, childrenInfo);

    ['SIGINT', 'SIGTERM'].forEach(function(signal) {
        process.on(signal, function() {
            children.forEach(function(child) {
                treeKill(child.pid, signal);
            });
        });
    });

    process.stdin.on('data', (chunk) => {
        let line = chunk.toString();

        let targetId = config.defaultInputTarget;
        if (line.indexOf(':') >= 0) {
            const parts = line.split(':');
            targetId = parts[0];
            line = parts[1];
        }

        const target = findChild(targetId, children, childrenInfo);
        if (target) {
            target.stdin.write(line);
        } else {
            logError('', chalk.gray.dim, `Unable to find command [${targetId}]\n`);
        }
    });
}

function spawnChild(cmd, options) {
    let child;
    try {
        child = spawn(cmd, options);
    } catch (e) {
        logError('', chalk.gray.dim, 'Error occured when executing command: ' + cmd);
        logError('', chalk.gray.dim, e.stack);
        process.exit(1);
    }
    return child;
}

function toStreams(children) {
    // Transform all process events to rx streams
    return _.map(children, function(child) {
        const childStreams = {
            error: Rx.Node.fromEvent(child, 'error'),
            close: Rx.Node.fromEvent(child, 'close')
        };
        if (!config.raw) {
            childStreams.stdout = Rx.Node.fromReadableStream(child.stdout);
            childStreams.stderr = Rx.Node.fromReadableStream(child.stderr);
        }

        return _.reduce(childStreams, function(memo, stream, key) {
            memo[key] = stream.map(function(data) {
                return {child: child, data: data};
            });

            return memo;
        }, {});
    });
}

function handleChildEvents(streams, children, childrenInfo) {
    handleClose(streams, children, childrenInfo);
    handleError(streams, childrenInfo);
    if (!config.raw) {
        handleOutput(streams, childrenInfo, 'stdout');
        handleOutput(streams, childrenInfo, 'stderr');
    }
}

function handleOutput(streams, childrenInfo, source) {
    const sourceStreams = _.map(streams, source);
    const combinedSourceStream = Rx.Observable.merge.apply(this, sourceStreams);

    combinedSourceStream.subscribe(function(event) {
        const prefix = getPrefix(childrenInfo, event.child);
        const prefixColor = childrenInfo[event.child.pid].prefixColor;
        log(prefix, prefixColor, event.data.toString());
    });
}

function handleClose(streams, children, childrenInfo) {
    let aliveChildren = _.clone(children);
    const exitCodes = [];
    const closeStreams = _.map(streams, 'close');
    const closeStream = Rx.Observable.merge.apply(this, closeStreams);
    let othersKilled = false;

    // TODO: Is it possible that amount of close events !== count of spawned?
    closeStream.subscribe(function(event) {
        const exitCode = event.data;
        const nonSuccess = exitCode !== 0;
        exitCodes.push(exitCode);

        const prefix = getPrefix(childrenInfo, event.child);
        const childInfo = childrenInfo[event.child.pid];
        const prefixColor = childInfo.prefixColor;
        const command = childInfo.command;
        logEvent(prefix, prefixColor, command + ' exited with code ' + exitCode);

        aliveChildren = _.filter(aliveChildren, function(child) {
            return child.pid !== event.child.pid;
        });

        if (nonSuccess && config.allowRestart && childInfo.restartTries--) {
            respawnChild(event, childrenInfo);
            return;
        }

        if (aliveChildren.length === 0) {
            exit(exitCodes);
        }
        if (!othersKilled) {
            if (config.killOthers) {
                killOtherProcesses(aliveChildren);
                othersKilled = true;
            } else if (config.killOthersOnFail && nonSuccess) {
                killOtherProcesses(aliveChildren);
                othersKilled = true;
            }
        }
    });
}

function respawnChild(event, childrenInfo) {
    setTimeout(function() {
        const childInfo = childrenInfo[event.child.pid];
        const prefix = getPrefix(childrenInfo, event.child);
        const prefixColor = childInfo.prefixColor;
        logEvent(prefix, prefixColor, childInfo.command + ' restarted');
        const newChild = spawnChild(childInfo.command, childInfo.options);

        childrenInfo[newChild.pid] = childrenInfo[event.child.pid];
        delete childrenInfo[event.child.pid];

        const children = [newChild];
        const streams = toStreams(children);
        handleChildEvents(streams, children, childrenInfo);
    }, config.restartAfter);
}

function killOtherProcesses(processes) {
    logEvent('--> ', chalk.gray.dim, 'Sending SIGTERM to other processes..');

    // Send SIGTERM to alive children
    _.each(processes, function(child) {
        treeKill(child.pid, 'SIGTERM');
    });
}

function exit(childExitCodes) {
    let success;
    switch (config.success) {
    case 'first':
        success = _.first(childExitCodes) === 0;
        break;
    case 'last':
        success = _.last(childExitCodes) === 0;
        break;
    default:
        success = _.every(childExitCodes, function(code) {
            return code === 0;
        });
    }
    process.exit(success ? 0 : 1);
}

function handleError(streams, childrenInfo) {
    // Output emitted errors from child process
    const errorStreams = _.map(streams, 'error');
    const processErrorStream = Rx.Observable.merge.apply(this, errorStreams);

    processErrorStream.subscribe(function(event) {
        const command = childrenInfo[event.child.pid].command;
        logError('', chalk.gray.dim, 'Error occured when executing command: ' + command);
        logError('', chalk.gray.dim, event.data.stack);
    });
}

function colorText(text, color) {
    if (!config.color) {
        return text;
    } else {
        return color(text);
    }
}

function getPrefix(childrenInfo, child) {
    const prefixes = getPrefixes(childrenInfo, child);
    const prefixType = config.prefix || (prefixes.name ? 'name' : 'index');
    if (_.includes(_.keys(prefixes), prefixType)) {
        return '[' + prefixes[prefixType] + '] ';
    }

    return _.reduce(prefixes, function(memo, val, key) {
        const re = new RegExp('{' + key + '}', 'g');
        return memo.replace(re, val);
    }, config.prefix) + ' ';
}

function getPrefixes(childrenInfo, child) {
    const prefixes = {};

    prefixes.none = '';
    prefixes.pid = child.pid;
    prefixes.index = childrenInfo[child.pid].index;
    prefixes.name = childrenInfo[child.pid].name;
    prefixes.time = formatDate(Date.now(), config.timestampFormat);

    const command = childrenInfo[child.pid].command;
    prefixes.command = shortenText(command, config.prefixLength);
    return prefixes;
}

function shortenText(text, length, cut) {
    if (text.length <= length) {
        return text;
    }
    cut = _.isString(cut) ? cut : '..';

    const endLength = Math.floor(length / 2);
    const startLength = length - endLength;

    const first = text.substring(0, startLength);
    const last = text.substring(text.length - endLength, text.length);
    return first + cut + last;
}

function log(prefix, prefixColor, text) {
    logWithPrefix(prefix, prefixColor, text);
}

function logEvent(prefix, prefixColor, text) {
    if (config.raw) {
        return;
    }

    logWithPrefix(prefix, prefixColor, text + '\n', chalk.gray.dim);
}

function logError(prefix, prefixColor, text) {
    // This is for now same as log, there might be separate colors for stderr
    // and stdout
    logWithPrefix(prefix, prefixColor, text, chalk.red.bold);
}

let lastChar;

function logWithPrefix(prefix, prefixColor, text, color) {

    if (config.raw) {
        process.stdout.write(text);
        return;
    }

    text = text.replace(/\u2026/g,'...'); // Ellipsis

    const lines = text.split('\n');
    // Do not bgColor trailing space
    const coloredPrefix = colorText(prefix.replace(/ $/, ''), prefixColor) + ' ';
    const paddedLines = _.map(lines, function(line, index) {
        let coloredLine = color ? colorText(line, color) : line;
        if (index !== 0 && index !== (lines.length - 1)) {
            coloredLine = coloredPrefix + coloredLine;
        }
        return coloredLine;
    });

    if (!lastChar || lastChar === '\n' ) {
        process.stdout.write(coloredPrefix);
    }

    lastChar = text[text.length - 1];

    process.stdout.write(paddedLines.join('\n'));
}

main();
