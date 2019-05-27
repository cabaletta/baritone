declare class ClientCapability {
  constructor(options: ClientCapability.ClientCapabilityOptions);

  accountSid: string;
  authToken: string;
  ttl: number;
  scopes: ClientCapability.Scope[];
  addScope(scope: ClientCapability.Scope): void;
  toJwt(): string;
}

declare namespace ClientCapability {
  export interface Scope {
    scope: string;
    payload(): string;
  }

  export interface OutgoingClientScopeOptions {
    applicationSid: string;
    clientName?: string;
    params?: object;
  }

  export class EventStreamScope implements Scope {
    constructor(filters: object);
    filters: object;
    scope: 'scope:stream:subscribe';
    payload(): string;
  }

  export class IncomingClientScope implements Scope {
    constructor(clientName: string);
    clientName: string
    scope: 'scope:client:incoming';
    payload(): string;
  }

  export class OutgoingClientScope implements Scope {
    constructor(options: OutgoingClientScopeOptions);
    applicationSid: string;
    clientName?: string;
    params?: object;
    scope: 'scope:client:outgoing';
    payload(): string;
  }

  export interface ClientCapabilityOptions {
    accountSid: string;
    authToken: string;
  }
}

export = ClientCapability;
