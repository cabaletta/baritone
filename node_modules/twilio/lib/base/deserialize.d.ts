/**
 * Parse a string into a Date object
 *
 * @param  s date string in YYYY-MM-DD format
 * @return Date object
 */
export function iso8601Date(s: string): Date;

/**
 * Parse a string into a Date object
 *
 * @param  s date string in YYYY-MM-DD[T]HH:mm:ss[Z] format
 * @return Date object
 */
export function iso8601DateTime(s: string): Date;

/**
 * Parse a string into a Date object
 *
 * @param  s date string in ddd, DD MMM YYYY HH:mm:ss [+0000] format
 * @return Date object
 */
export function rfc2822DateTime(s: string): Date;

/**
 * parse a string into a decimal
 *
 * @param  d decimal value as string
 * @return number object
 */
export function decimal(d: string): number;

/**
 * Parse a string into a integer
 *
 * @param  i integer value as string
 * @return number object
 */
export function integer(i: string): number;