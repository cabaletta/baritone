var bCrypt = require("./bCrypt");

var compares = 0;
var salts = [];
var hashes = [];

console.log("\n\n Salts \n");
bCrypt.genSalt(8, saltCallback);
bCrypt.genSalt(10, saltCallback);

function saltCallback(error, result) {
	if(!error) {
		console.log(result);
	} else {
		console.log(error);
	}
	salts.push(result);
	if(salts.length == 2) {
		console.log("\n\n Hashes \n");
		createHash(salts[0]);
	}
}

function createHash(salt) {
	bCrypt.hash("bacon", salt, null, hashCallback);
	bCrypt.hash("bacon", salt, null, hashCallback);
}

function hashCallback(error, result) {
	if(!error) {
		console.log(result);
	} else {
		console.log(error);
	}
	hashes.push(result);
	if(hashes.length == 2) {
		createHash(salts[1]);
	} else if(hashes.length == 4) {
		console.log("\n\n True Compares \n");
		compares = 0;
		startCompares("bacon", trueCompareCallback);
	}
}

function startCompares(string, callback) {
	bCrypt.compare(string, hashes[0], callback);
	bCrypt.compare(string, hashes[1], callback);
	bCrypt.compare(string, hashes[2], callback);
	bCrypt.compare(string, hashes[3], callback);
}

function trueCompareCallback(error, result) {
	if(!error) {
		console.log(result);
	} else {
		console.log(error);
	}
	if(++compares == 4) {
		console.log("\n\n False Compares \n");
		compares = 0;
		startCompares("veggies", falseCompareCallback);
	}
}

function falseCompareCallback(error, result) {
	if(!error) {
		console.log(result);
	} else {
		console.log(error);
	}
}