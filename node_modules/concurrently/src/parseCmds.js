'use strict';
const _ = require('lodash');

const pkgInfo = require('./pkgInfo');

module.exports = function(cmds, config) {
    config = config || {};

    const names = (config.names || '').split(config.nameSeparator || ',');
    const prefixColors = config.prefixColors ? config.prefixColors.split(',') : [];

    cmds = cmds.map(stripCmdQuotes);

    cmds = cmds.map((cmd, idx) => ({
        cmd: cmd,
        name: names[idx] || ''
    }));

    cmds = _.flatMap(cmds, expandCmdShortcuts);

    return cmds.map((cmd, idx) => Object.assign(cmd, {
        color: prefixColors[idx]
    }));
};

function stripCmdQuotes(cmd) {
    // Removes the quotes surrounding a command.
    if (cmd[0] === '"' || cmd[0] === '\'') {
        return cmd.substr(1, cmd.length - 2);
    } else {
        return cmd;
    }
}

function expandCmdShortcuts(cmd) {
    const shortcut = cmd.cmd.match(/^npm:(\S+)(.*)/);
    if (shortcut) {
        const cmdName = shortcut[1];
        const args = shortcut[2];

        const wildcard = cmdName.indexOf('*');
        if (wildcard >= 0) {
            return expandNpmWildcard(cmd, cmdName, wildcard, args);
        }

        if (!cmd.name) {
            cmd.name = cmdName;
        }
        cmd.cmd = `npm run ${cmdName}${args}`;
    }
    return [ cmd ];
}

function expandNpmWildcard(cmd, cmdName, wildcardPos, args) {
    const rePre = _.escapeRegExp(cmdName.substr(0, wildcardPos));
    const reSuf = _.escapeRegExp(cmdName.substr(wildcardPos + 1));
    const wildcardRe = new RegExp(`^${rePre}(.*?)${reSuf}$`);

    return pkgInfo.getScripts()
        .filter(script => script.match(wildcardRe))
        .map(script => Object.assign({}, cmd,  {
            cmd: `npm run ${script}${args}`,
            name: cmd.name + script.match(wildcardRe)[1]
        }));
}
