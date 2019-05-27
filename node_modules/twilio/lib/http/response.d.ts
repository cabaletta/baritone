declare class Response<TPayload> {
  constructor(statusCode: number, body: TPayload);
  toString(): string;
}

export = Response;
