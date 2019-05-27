import TwilioClient = require('../rest/Twilio');

declare class Domain {
  constructor(twilio: TwilioClient, baseUrl: string);

  /**
   * Turn a uri into an absolute url
   *
   * @param  uri uri to transform
   * @return absolute url
   */
  absoluteUrl(uri: string): string;

  /**
   * Make request to this domain
   *
   * @param opts request options
   * @return request promise
   */
  request(opts?: TwilioClient.RequestOptions): Promise<any>;
}

export = Domain;
