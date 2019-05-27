import Version = require('./Version');
import Response = require('../http/response');

interface Solution {

}

/**
 * Base page object to maintain request state.
 */
declare class Page<TVersion extends Version, TPayload extends Page.TwilioResponsePayload, TResource, TInstance> {
  /**
   * Base page object to maintain request state.
   *
   * @param version - A twilio version instance
   * @param response - The http response
   * @param solution - path solution
   */
  constructor(version: TVersion, response: Response<string>, solution: Solution);

  /**
   * Get the url of the previous page of records
   *
   * @return url of the previous page
   */
  getPreviousPageUrl(): string;
  /**w
   * Get the url of the next page of records
   *
   * @return url of the next page
   */
  getNextPageUrl(): string;
  /**
   * Load a list of records
   *
   * @param  resources json payload of records
   * @return list of resources
   */
  loadInstance(resources: TResource[]): TInstance[];
  /**
   * Fetch the next page of records
   *
   * @return promise that resolves to next page of results
   */
  nextPage(): Promise<Page<TVersion, TPayload, TResource, TInstance>>;
  /**
   * Fetch the previous page of records
   *
   * @return promise that resolves to previous page of results
   */
  previousPage(): Promise<Page<TVersion, TPayload, TResource, TInstance>>;
  /**
   * Parse json response from API
   * @throws If non 200 status code is returned
   *
   * @param  response API response
   * @return json parsed response
   */
  processResponse(response: Response<string>): TPayload;
  /**
   * Load a page of records
   * @throws {Error} If records cannot be deserialized
   *
   * @param  payload json payload
   * @return the page of records
   */
  loadPage(payload: TPayload): TResource[];

  /**
   * @constant META_KEYS
   * @description meta keys returned in a list request
   */
  static META_KEYS: [
    'end',
    'first_page_uri',
    'last_page_uri',
    'next_page_uri',
    'num_pages',
    'page',
    'page_size',
    'previous_page_uri',
    'start',
    'total',
    'uri'
  ];
}

declare namespace Page {
  export interface TwilioResponsePayload {
    // DEPRECTATED: end: any;
    first_page_uri: string;
    // DEPRECTATED: last_page_uri: string;
    next_page_uri: string;
    // DEPRECTATED: num_pages: number;
    page: number;
    page_size: number;
    previous_page_uri: string;
    // DEPRECTATED: start: number;
    // DEPRECTATED: total: number;
    uri: string;
    meta?: {
      key?: string;
    }
  }
}

export = Page;