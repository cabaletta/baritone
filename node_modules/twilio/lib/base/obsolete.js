'use strict';

var util = require('util');


function ObsoleteClient(sid, tkn, options) {
  throw new Error(this.constructor.name + ' has been removed from this version of the library. Please refer to https://www.twilio.com/docs/libraries/node for more information.')
}


function RestClient(sid, tkn, options) {
  RestClient.super_.call(this, sid, tkn, options);
}
util.inherits(RestClient, ObsoleteClient);


function IpMessagingClient(sid, tkn, options) {
  IpMessagingClient.super_.call(this, sid, tkn, options);
}
util.inherits(IpMessagingClient, ObsoleteClient);


function PricingClient(sid, tkn, options) {
  PricingClient.super_.call(this, sid, tkn, options);
}
util.inherits(PricingClient, ObsoleteClient);


function MonitorClient(sid, tkn, options) {
  MonitorClient.super_.call(this, sid, tkn, options);
}
util.inherits(MonitorClient, ObsoleteClient);


function TaskRouterClient(sid, tkn, options) {
  TaskRouterClient.super_.call(this, sid, tkn, options);
}
util.inherits(TaskRouterClient, ObsoleteClient);


function LookupsClient(sid, tkn, options) {
  LookupsClient.super_.call(this, sid, tkn, options);
}
util.inherits(LookupsClient, ObsoleteClient);


function TrunkingClient(sid, tkn, options) {
  TrunkingClient.super_.call(this, sid, tkn, options);
}
util.inherits(TrunkingClient, ObsoleteClient);


module.exports = {
  RestClient: RestClient,
  IpMessagingClient: IpMessagingClient,
  PricingClient: PricingClient,
  MonitorClient: MonitorClient,
  TaskRouterClient: TaskRouterClient,
  LookupsClient: LookupsClient,
  TrunkingClient: TrunkingClient
};
