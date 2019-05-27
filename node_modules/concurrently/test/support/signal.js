['SIGINT', 'SIGTERM'].forEach(function(signal) {
    process.on(signal, function() {
        console.log(signal);
        process.exit(1);
    });
});

function doNothing() {
    setTimeout(doNothing, 1000);
}

doNothing();
console.log('STARTED');
