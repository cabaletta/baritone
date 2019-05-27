declare class AccessToken {
  static DEFAULT_ALGORITHM: 'HS256';
  static ALGORITHMS: ['HS256', 'HS384', 'HS512'];

  /**
   * Creates new AccessToken instance
   *
   * @param accountSid - The account's unique ID to which access is scoped
   * @param keySid - The signing key's unique ID
   * @param secret - The secret to sign the token with
   * @param options - Options
   */
  constructor(
    accountSid: string,
    keySid: string,
    secret: string,
    options?: AccessToken.AccessTokenOptions
  );

  /**
   * Adds a grant for a respective Twilio service to the access token
   * @param grant The grant to add
   */
  addGrant<T extends AccessToken.Grant<any, any, any>>(grant: T): void;

  /**
   * Turns the access token instance into a JWT that can be used in the front-end
   * @param algorithm The algorithm to sign the JWT
   */
  toJwt(algorithm: 'HS256' | 'HS384' | 'HS512'): string;
}

declare namespace AccessToken {
  export abstract class Grant<TOptions, TPayload, TKey> {
    constructor(opts?: TOptions);
    key: TKey;
    toPayload(): TPayload;
  }

  export interface TaskRouterGrantOptions {
    workspaceSid?: string;
    workerSid?: string;
    role?: string;
  }

  export interface TaskRouterGrantPayload {
    workspace_sid?: string;
    worker_id?: string;
    role?: string;
  }

  export class TaskRouterGrant extends Grant<
    TaskRouterGrantOptions,
    TaskRouterGrantPayload,
    'task_router'
  > {}

  export interface ChatGrantOptions {
    serviceSid?: string;
    endpointId?: string;
    deploymentRoleSid?: string;
    pushCredentialSid?: string;
  }

  export interface ChatGrantPayload {
    service_sid?: string;
    endpoint_id?: string;
    deployment_role_sid?: string;
    push_credential_sid?: string;
  }

  export class ChatGrant extends Grant<
    ChatGrantOptions,
    ChatGrantPayload,
    'chat'
  > {}

  export class IpMessagingGrant extends Grant<
    ChatGrantOptions,
    ChatGrantPayload,
    'ip_messaging'
  > {}

  export interface ConversationsGrantOptions {
    configurationProfileSid?: string;
  }

  export interface ConversationsGrantPayload {
    configuration_profile_sid?: string;
  }

  export class ConversationsGrant extends Grant<
    ConversationsGrantOptions,
    ConversationsGrantPayload,
    'rtc'
  > {}

  export interface VideoGrantOptions {
    room?: string;
  }

  export interface VideoGrantPayload {
    room?: string;
  }

  export class VideoGrant extends Grant<
    VideoGrantOptions,
    VideoGrantPayload,
    'video'
  > {}

  export interface SyncGrantOptions {
    serviceSid?: string;
    endpointId?: string;
  }

  export interface SyncGrantPayload {
    service_sid?: string;
    endpoint_id?: string;
  }

  export class SyncGrant extends Grant<
    SyncGrantOptions,
    SyncGrantPayload,
    'data_sync'
  > {}

  export interface VoiceGrantOptions {
    outgoingApplicationSid?: string;
    outgoingApplicationParams?: object;
    pushCredentialSid?: string;
    endpointId?: string;
  }

  export interface VoiceGrantPayload {
    outgoing?: { application_sid: string; params?: object };
    push_credential_sid?: string;
    endpoint_id?: string;
  }

  export class VoiceGrant extends Grant<
    VoiceGrantOptions,
    VoiceGrantPayload,
    'voice'
  > {}

  export interface AccessTokenOptions {
    /**
     * Time to live in seconds
     */
    ttl: number /**
     * The identity of the first person
     */;
    identity: string /**
     * Time from epoch in seconds for not before value
     */;
    nbf: number;
  }
}

export = AccessToken;
