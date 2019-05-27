export type HttpMethod = 'get'|'post'|'put'|'patch'|'delete';

export type Url = string;

export type PhoneNumber = string;

export type Sid = string;

export interface ListEachOptions<TInstance> {
  /**
   * Upper limit for the number of records to return.
   * each() guarantees never to return more than limit.
   * Default is no limit
   */
  limit?: number;
  /**
   * Number of records to fetch per request,
   * when not set will use the default value of 50 records.
   * If no pageSize is defined but a limit is defined,
   * each() will attempt to read the limit with the most efficient
   * page size, i.e. min(limit, 1000)
   */
  pageSize?: number;
  /**
   * Function to process each record. If this and a positional
   * callback are passed, this one will be used
   */
  callback?: (item: TInstance, done: (err?: Error) => void) => void;
  /**
   * Function to be called upon completion of streaming
   */
  done?: (err?: Error) => void;
}

export interface ListOptions<TInstance> {
  /**
   * Upper limit for the number of records to return.
   * each() guarantees never to return more than limit.
   * Default is no limit
   */
  limit?: number;
  /**
   * Number of records to fetch per request,
   * when not set will use the default value of 50 records.
   * If no pageSize is defined but a limit is defined,
   * each() will attempt to read the limit with the most efficient
   * page size, i.e. min(limit, 1000)
   */
  pageSize?: number;
  /**
   * Callback to handle list of records
   */
  callback?: (items: TInstance[]) => void;
}

export interface PageOptions<TPage> {
  /**
   * PageToken provided by the API
   */
  pageToken?: string;
  /**
   * Page Number, this value is simply for client state
   */
  pageNumber?: number;
  /**
   * Number of records to return, defaults to 50
   */
  pageSize?: number;
  /**
   * Callback to handle list of records
   */
  callback?: (page: TPage) => void;
}

/**
 * A generic type that returns all property names of a class whose type is not "function"
 */
export type NonFunctionPropertyNames<T> = {
  [K in keyof T]: T[K] extends Function ? never : K;
}[keyof T];

/**
 * A generic type that returns only properties of a class that are not functions
 */
export type NonFunctionProperties<T> = Pick<T, NonFunctionPropertyNames<T>>;

/**
 * A type alias for better readibility
 */
export type Json<T> = NonFunctionProperties<T>;

export declare class SerializableClass {
  /**
   * Converts the current instance in a regular JSON. 
   * It will be automatically called by JSON.stringify()
   */
  toJSON(): Json<this>;
}
