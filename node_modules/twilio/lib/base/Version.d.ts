import Domain = require('./Domain');
import TwilioClient = require('../rest/Twilio');

declare class Version {
  constructor(solutelydomain: Domain, version: string);
  /**
   * Generate absolute url from a uri
   *
   * @param  uri uri to transform
   * @return transformed url
   */
  absoluteUrl(uri: string): string;
  /**
   * Generate relative url from a uri
   *
   * @param  uri uri to transform
   * @return transformed url
   */
  relativeUrl(uri: string): string;
  /**
   * Make a request against the domain
   *
   * @param  opts request options
   * @return promise that resolves to request response
   */
  request(opts: TwilioClient.RequestOptions): Promise<any>;
  /**
   * Fetch a instance of a record
   * @throws {Error} If response returns non 2xx status code
   *
   * @param  opts request options
   * @return promise that resolves to fetched result
   */
  fetch(opts: TwilioClient.RequestOptions): Promise<any>;
  /**
   * Update a record
   * @throws {Error} If response returns non 2xx status code
   *
   * @param  opts request options
   * @return promise that resolves to updated result
   */
  update(opts: TwilioClient.RequestOptions): Promise<any>;
  /**
   * Delete a record
   * @throws {Error} If response returns a 5xx status
   *
   * @param  opts request options
   * @return promise that resolves to true if record was deleted
   */
  remove(opts: TwilioClient.RequestOptions): Promise<boolean>;
  /**
   * Create a new record
   * @throws {Error} If response returns non 2xx or 201 status code
   *
   * @param  opts request options
   * @return promise that resolves to created record
   */
  create(opts: TwilioClient.RequestOptions): Promise<any>;
  /**
   * Fetch a page of records
   *
   * @param  opts request options
   * @return promise that resolves to page of records
   */
  page(opts: TwilioClient.RequestOptions): Promise<any>;
  /**
   * Process limits for list requests
   *
   * @param opts Page limit options passed to the request
   */
  readLimits(opts: Version.PageLimitOptions): Version.PageLimit;
}

declare namespace Version {
  export interface PageLimitOptions { 
    /**
     * The maximum number of items to fetch
     */
    limit: number; 
    /**
     * The maximum number of items to return with every request
     */
    pageSize: number;
  }

  export interface PageLimit {
    limit: number;
    pageSize: number;
    pageLimit: number;
  }
}

export = Version;