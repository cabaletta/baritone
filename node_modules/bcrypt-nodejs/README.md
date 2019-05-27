bcrypt-nodejs
===========================================

Warning : A change was made in v0.0.3 to allow encoding of UTF-8 encoded strings. This causes strings encoded in v0.0.2 or earlier to not work in v0.0.3 anymore.

Native JS implementation of BCrypt for Node.
Has the same functionality as [node.bcrypt.js] expect for a few tiny differences.
Mainly, it doesn't let you set the seed length for creating the random byte array.

I created this version due to a small [problem](https://github.com/ncb000gt/node.bcrypt.js/issues/102) I faced with [node.bcrypt.js].
Basically, to deploy one of my apps which uses [node.bcrypt.js] on a winx64 platform, I have to force the user to download about 1.6gb of sdks, buildtools and other requirements of which some fail to install ! Microsoft :(

This code is based on [javascript-bcrypt] and uses [crypto] (http://nodejs.org/api/crypto.html) to create random byte arrays.

Basic usage:
-----------
Synchronous
```
var hash = bcrypt.hashSync("bacon");

bcrypt.compareSync("bacon", hash); // true
bcrypt.compareSync("veggies", hash); // false
```

Asynchronous
```
bcrypt.hash("bacon", null, null, function(err, hash) {
	// Store hash in your password DB.
});

// Load hash from your password DB.
bcrypt.compare("bacon", hash, function(err, res) {
    // res == true
});
bcrypt.compare("veggies", hash, function(err, res) {
    // res = false
});
```

In the above examples, the salt is automatically generated and attached to the hash.
Though you can use your custom salt and there is no need for salts to be persisted as it will always be included in the final hash result and can be retrieved.

API
-------------------------
* `genSaltSync(rounds)`
	* `rounds` - [OPTIONAL] - the number of rounds to process the data for. (default - 10)
* `genSalt(rounds, callback)`
	* `rounds` - [OPTIONAL] - the number of rounds to process the data for. (default - 10)
	* `callback` - [REQUIRED] - a callback to be fired once the salt has been generated.
		* `error` - First parameter to the callback detailing any errors.
		* `result` - Second parameter to the callback providing the generated salt.
* `hashSync(data, salt)`
	* `data` - [REQUIRED] - the data to be encrypted.
	* `salt` - [REQUIRED] - the salt to be used in encryption.
* `hash(data, salt, progress, cb)`
	* `data` - [REQUIRED] - the data to be encrypted.
	* `salt` - [REQUIRED] - the salt to be used to hash the password.
	* `progress` - a callback to be called during the hash calculation to signify progress
	* `callback` - [REQUIRED] - a callback to be fired once the data has been encrypted.
		* `error` - First parameter to the callback detailing any errors.
		* `result` - Second parameter to the callback providing the encrypted form.
* `compareSync(data, encrypted)`
	* `data` - [REQUIRED] - data to compare.
	* `encrypted` - [REQUIRED] - data to be compared to.
* `compare(data, encrypted, cb)`
	* `data` - [REQUIRED] - data to compare.
	* `encrypted` - [REQUIRED] - data to be compared to.
	* `callback` - [REQUIRED] - a callback to be fired once the data has been compared.
		* `error` - First parameter to the callback detailing any errors.
		* `result` - Second parameter to the callback providing whether the data and encrypted forms match [true | false].
* `getRounds(encrypted)` - return the number of rounds used to encrypt a given hash
	* `encrypted` - [REQUIRED] - hash from which the number of rounds used should be extracted.
	
Contributors
============

* [Alex Murray][alexmurray]
* [Nicolas Pelletier][NicolasPelletier]
* [Josh Rogers][geekymole]	
	
Credits
-------------------------
I heavily reused code from [javascript-bcrypt]. Though "Clipperz Javascript Crypto Library" was removed and its functionality replaced with "crypto".

[node.bcrypt.js]:https://github.com/ncb000gt/node.bcrypt.js.git
[javascript-bcrypt]:http://code.google.com/p/javascript-bcrypt/

[alexmurray]:https://github.com/alexmurray
[NicolasPelletier]:https://github.com/NicolasPelletier
[geekymole]:https://github.com/geekymole