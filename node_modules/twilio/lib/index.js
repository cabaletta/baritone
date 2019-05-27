'use strict';

var Twilio = require('./rest/Twilio');
var webhooks = require('./webhooks/webhooks');
var obsolete = require('./base/obsolete');

// Shorthand to automatically create a RestClient
var initializer = function(accountSid, authToken, opts) {
  return new Twilio(accountSid, authToken, opts);
};

// Main functional components of the Twilio module
initializer.Twilio = Twilio;
initializer.jwt = {
  AccessToken: require('./jwt/AccessToken'),
  ClientCapability: require('./jwt/ClientCapability'),
  taskrouter: {
    TaskRouterCapability: require('./jwt/taskrouter/TaskRouterCapability'),
    util: require('./jwt/taskrouter/util')
  }
};
initializer.twiml = {
  VoiceResponse: require('./twiml/VoiceResponse'),
  MessagingResponse: require('./twiml/MessagingResponse'),
  FaxResponse: require('./twiml/FaxResponse')
};

// Add obsolete clients
initializer.RestClient = obsolete.RestClient;
initializer.PricingClient = obsolete.PricingClient;
initializer.MonitorClient = obsolete.MonitorClient;
initializer.TaskRouterClient = obsolete.TaskRouterClient;
initializer.IpMessagingClient = obsolete.IpMessagingClient;
initializer.LookupsClient = obsolete.LookupsClient;
initializer.TrunkingClient = obsolete.TrunkingClient;

// Setup webhook helper functionality
initializer.validateRequest = webhooks.validateRequest;
initializer.validateRequestWithBody = webhooks.validateRequestWithBody;
initializer.validateExpressRequest = webhooks.validateExpressRequest;
initializer.webhook = webhooks.webhook;

// Public module interface is a function, which passes through to RestClient constructor
module.exports = initializer;
