declare class TaskRouterCapability {
  /**
   * TaskRouterCapability class
   * @param options Options to initiate
   */
  constructor(options: TaskRouterCapability.TaskRouterCapabilityOptions);

  accountSid: string;
  authToken: string;
  workspaceSid: string;
  channelId: string;
  ttl: number;
  version: string;
  policies: TaskRouterCapability.Policy[];
  friendlyName?: string;

  addPolicy(policy: TaskRouterCapability.Policy): void;
  toJwt(): string;
}

declare namespace TaskRouterCapability {
  export interface TaskRouterCapabilityOptions {
    accountSid: string;
    authToken: string;
    workspaceSid: string;
    channelId: string;
    friendlyName?: string;
    ttl?: number;
    version?: string;
  }

  export interface PolicyOptions {
    /** Policy URL */
    url?: string;
    /** HTTP Method */
    method?: string;
    /** Request query filter allowances */
    queryFilter?: object;
    /** Request post filter allowances */
    postFilter?: object;
    /** Allow the policy */
    allowed?: boolean;
  }

  export interface PolicyPayload {
    url: string;
    method: string;
    query_filter: object;
    post_filter: object;
    allow: boolean;
  }

  export class Policy {
    /**
     * Create a new Policy
     * @param options Options to initiate policy
     */
    constructor(options: PolicyOptions);

    payload(): PolicyPayload;
  }
}

export = TaskRouterCapability;
