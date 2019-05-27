# gcp-metadata
> Get the metadata from a Google Cloud Platform environment

[![codecov][codecov-image]][codecov-url]

```sh
$ npm install --save gcp-metadata
```
```js
const gcpMetadata = require('gcp-metadata');
```

#### Access all metadata
```js
const res = await gcpMetadata.instance();
console.log(res.data); // ... All metadata properties
```

#### Access specific properties
```js
const res = await gcpMetadata.instance('hostname');
console.log(res.data) // ...All metadata properties
```

#### Access specific properties with query parameters
```js
const res = await gcpMetadata.instance({
  property: 'tags',
  params: { alt: 'text' }
});
console.log(res.data) // ...Tags as newline-delimited list
```

[codecov-image]: https://codecov.io/gh/stephenplusplus/gcp-metadata/branch/master/graph/badge.svg
[codecov-url]: https://codecov.io/gh/stephenplusplus/gcp-metadata