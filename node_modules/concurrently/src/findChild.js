'use strict';

module.exports = function(targetId, children, childrenInfo) {

    return children.find((child) => {
        return (
            childrenInfo[child.pid].index.toString() === targetId ||
            childrenInfo[child.pid].name === targetId
        );
    });
};
