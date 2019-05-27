/**
 * Turns a Date object into a string if parameter is a Date
 * otherwise returns the parameter
 *
 * @param  date date object to format
 * @return date formatted in YYYY-MM-DD form
 */
export function iso8601Date(date: Date): string;
export function iso8601Date<T>(data: T): T;

/**
 * Turns a Date object into a string if parameter is a Date
 * otherwise returns the parameter
 *
 * @param  date date object to format
 * @return date formatted in YYYY-MM-DD[T]HH:mm:ss[Z] form
 */
export function iso8601DateTime(date: Date): string;
export function iso8601DateTime<T>(data: T): T;

/**
 * Turns a map of params int oa flattened map separated by dots
 * if the parameter is an object, otherwise returns an empty map
 * 
 * @param m map to transform
 * @param prefix to append to each flattened value
 * @returns flattened map
 */
export function prefixedCollapsibleMap<T extends {}>(m: T, prefix?: string): T;
export function prefixedCollapsibleMap<T>(m: T, prefix?: string): {};

/**
 * Turns an object into a JSON string if the parameter
 * is an object, otherwise returns the passed in object
 * 
 * @param o json object or array
 * @returns stringified object
 */
export function object(o: object | Array<any>): string;
export function object<T>(o: T): T;

/**
 * Coerces a boolean literal into a string
 * 
 * @param input boolean or string to be coerced
 * @returns a string 'true' or 'false' if passed a boolean, else the value
 */
export function bool(input: boolean): 'true' | 'false';
export function bool(input: string): string;

/**
 * Maps transform over each element in input if input is an array
 *
 * @param input array to map transform over, if not an array then it is
 * returned as is.
 * @returns new array with transform applied to each element.
 */
type MapFunction<TInput, TOutput> = (input: TInput) => TOutput;
export function map<TInput, TOutput>(input: Array<TInput>, transform: MapFunction<TInput, TOutput>): Array<TOutput>
export function map<T>(input: T, transform?: any): T;