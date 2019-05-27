import { HttpMethod } from '../interfaces';
import Response = require('../http/response');

declare class RequestClient {
  constructor();
  /**
   * Make an HTTP request
   * @param opts The request options
   */
  request<TData>(opts: RequestClient.RequestOptions<TData>): Promise<Response<TData>>;
}

declare namespace RequestClient {
  export interface RequestOptions<TData = any, TParams = object> {
    /**
     * The HTTP method
     */
    method: HttpMethod;
    /**
     * The request URI
     */
    uri: string;
    /**
     * The username used for auth
     */
    username?: string;
    /**
     * The password used for auth
     */
    password?: string;
    /**
     * The request headers
     */
    headers?: Headers;
    /**
     * The object of params added as query string to the request
     */
    params?: TParams;
    /**
     * The form data that should be submitted
     */
    data?: TData;
    /**
     * The request timeout in milliseconds
     */
    timeout?: number;
    /**
     * Should the client follow redirects
     */
    allowRedirects?: boolean;
    /**
     * Set to true to use the forever-agent
     */
    forever?: boolean;
  }

  export interface Headers {
    [header: string]: string;
  }
}

export = RequestClient;