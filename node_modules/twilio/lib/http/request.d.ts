import { HttpMethod } from '../interfaces';

declare class Request<TData> {
  constructor(opts: RequestOptions<TData>);
  attributeEqual(lhs: any, rhs: any): boolean;
  isEqual(other: Request<any>): boolean;
  toString(): string;
}

declare namespace Request {
  export interface RequestOptions<TData> {
    method?: HttpMethod | '*';
    url?: string;
    auth?: string;
    params?: string;
    data?: TData | '*';
    headers?: object | '*';
  }
}

export = Request;