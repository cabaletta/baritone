/**
 * Removes all undefined values of an object
 *
 * @param  obj object to filter
 * @return object with no undefined values
 */
export function of<TInput = {}, TOutput = {}>(obj: TInput): TOutput;