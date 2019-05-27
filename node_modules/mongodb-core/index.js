'use strict';

var BSON = require('bson');
var require_optional = require('require_optional');
const EJSON = require('./lib/utils').retrieveEJSON();

try {
  // Attempt to grab the native BSON parser
  var BSONNative = require_optional('bson-ext');
  // If we got the native parser, use it instead of the
  // Javascript one
  if (BSONNative) {
    BSON = BSONNative;
  }
} catch (err) {} // eslint-disable-line

module.exports = {
  // Errors
  MongoError: require('./lib/error').MongoError,
  MongoNetworkError: require('./lib/error').MongoNetworkError,
  MongoParseError: require('./lib/error').MongoParseError,
  MongoTimeoutError: require('./lib/error').MongoTimeoutError,
  MongoWriteConcernError: require('./lib/error').MongoWriteConcernError,
  mongoErrorContextSymbol: require('./lib/error').mongoErrorContextSymbol,
  // Core
  Connection: require('./lib/connection/connection'),
  Server: require('./lib/topologies/server'),
  ReplSet: require('./lib/topologies/replset'),
  Mongos: require('./lib/topologies/mongos'),
  Logger: require('./lib/connection/logger'),
  Cursor: require('./lib/cursor'),
  ReadPreference: require('./lib/topologies/read_preference'),
  Sessions: require('./lib/sessions'),
  BSON: BSON,
  EJSON: EJSON,
  // Raw operations
  Query: require('./lib/connection/commands').Query,
  // Auth mechanisms
  defaultAuthProviders: require('./lib/auth/defaultAuthProviders').defaultAuthProviders,
  MongoCR: require('./lib/auth/mongocr'),
  X509: require('./lib/auth/x509'),
  Plain: require('./lib/auth/plain'),
  GSSAPI: require('./lib/auth/gssapi'),
  ScramSHA1: require('./lib/auth/scram').ScramSHA1,
  ScramSHA256: require('./lib/auth/scram').ScramSHA256,
  // Utilities
  parseConnectionString: require('./lib/uri_parser')
};
