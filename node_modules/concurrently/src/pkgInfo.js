'use strict';

const readPkg = require('read-pkg');

let pkg;

function getPkg() {
    if (!pkg) {
        pkg = readPkg.sync();
    }
    return pkg;
}

exports.getScripts = function() {
    const pkg = getPkg();
    return Object.keys(pkg.scripts || {});
};
