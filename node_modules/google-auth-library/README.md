<img src="https://avatars0.githubusercontent.com/u/1342004?v=3&s=96" alt="Google Inc. logo" title="Google" align="right" height="96" width="96"/>

# Google Auth Library

[![Greenkeeper badge][greenkeeperimg]][greenkeeper]
[![npm version][npmimg]][npm]
[![CircleCI][circle-image]][circle-url]
[![codecov][codecov-image]][codecov-url]
[![Dependencies][david-dm-img]][david-dm]
[![Known Vulnerabilities][snyk-image]][snyk-url]

This is Google's officially supported [node.js][node] client library for using OAuth 2.0 authorization and authentication with Google APIs.

## Installation
This library is distributed on `npm`. To add it as a dependency, run the following command:

``` sh
$ npm install google-auth-library
```

## Upgrading to 1.x
The `1.x` release includes a variety of bug fixes, new features, and breaking changes. Please take care, and see [the release notes](https://github.com/google/google-auth-library-nodejs/releases/tag/v1.0.0) for a list of breaking changes, and the upgrade guide.

## Ways to authenticate
This library provides a variety of ways to authenticate to your Google services.
- [Application Default Credentials](#choosing-the-correct-credential-type-automatically) - Use Application Default Credentials when you use a single identity for all users in your application. Especially useful for applications running on Google Cloud.
- [OAuth 2](#oauth2) - Use OAuth2 when you need to perform actions on behalf of the end user.
- [JSON Web Tokens](#json-web-tokens) - Use JWT when you are using a single identity for all users. Especially useful for server->server or server->API communication.
- [Google Compute](#compute) - Directly use a service account on Google Cloud Platform. Useful for server->server or server->API communication.

## Application Default Credentials
This library provides an implementation of [Application Default Credentials][] for Node.js. The [Application Default Credentials][] provide a simple way to get authorization credentials for use in calling Google APIs.

They are best suited for cases when the call needs to have the same identity and authorization level for the application independent of the user. This is the recommended approach to authorize calls to Cloud APIs, particularly when you're building an application that uses Google Cloud Platform.

#### Download your Service Account Credentials JSON file

To use `Application Default Credentials`, You first need to download a set of JSON credentials for your project. Go to **APIs & Auth** > **Credentials** in the [Google Developers Console][devconsole] and select **Service account** from the **Add credentials** dropdown.

> This file is your *only copy* of these credentials. It should never be
> committed with your source code, and should be stored securely.

Once downloaded, store the path to this file in the `GOOGLE_APPLICATION_CREDENTIALS` environment variable.

#### Enable the API you want to use

Before making your API call, you must be sure the API you're calling has been enabled. Go to **APIs & Auth** > **APIs** in the [Google Developers Console][devconsole] and enable the APIs you'd like to call. For the example below, you must enable the `DNS API`.


#### Choosing the correct credential type automatically

Rather than manually creating an OAuth2 client, JWT client, or Compute client, the auth library can create the correct credential type for you, depending upon the environment your code is running under.

For example, a JWT auth client will be created when your code is running on your local developer machine, and a Compute client will be created when the same code is running on Google Cloud Platform. If you need a specific set of scopes, you can pass those in the form of a string or an array into the `auth.getClient` method.

The code below shows how to retrieve a default credential type, depending upon the runtime environment.

```js
const {auth} = require('google-auth-library');

/**
 * Acquire a client, and make a request to an API that's enabled by default.
 */
async function main() {
  const client = await auth.getClient({
    scopes: 'https://www.googleapis.com/auth/cloud-platform'
  });
  const projectId = await auth.getDefaultProjectId();
  const url = `https://www.googleapis.com/dns/v1/projects/${projectId}`;
  const res = await client.request({ url });
  console.log(res.data);
}

/**
 * Instead of specifying the type of client you'd like to use (JWT, OAuth2, etc)
 * this library will automatically choose the right client based on the environment.
 */
async function getADC() {
  // Acquire a client and the projectId based on the environment. This method looks
  // for the GCLOUD_PROJECT and GOOGLE_APPLICATION_CREDENTIALS environment variables.
  const res = await auth.getApplicationDefault();
  let client = res.credential;

  // The createScopedRequired method returns true when running on GAE or a local developer
  // machine. In that case, the desired scopes must be passed in manually. When the code is
  // running in GCE or a Managed VM, the scopes are pulled from the GCE metadata server.
  // See https://cloud.google.com/compute/docs/authentication for more information.
  if (client.createScopedRequired && client.createScopedRequired()) {
    // Scopes can be specified either as an array or as a single, space-delimited string.
    const scopes = ['https://www.googleapis.com/auth/cloud-platform'];
    client = client.createScoped(scopes);
  }
  return {
    client: client,
    projectId: res.projectId
  }
}

main().catch(console.error);
```

## OAuth2

This library comes with an [OAuth2][oauth] client that allows you to retrieve an access token and refreshes the token and retry the request seamlessly if you also provide an `expiry_date` and the token is expired. The basics of Google's OAuth2 implementation is explained on [Google Authorization and Authentication documentation][authdocs].

In the following examples, you may need a `CLIENT_ID`, `CLIENT_SECRET` and `REDIRECT_URL`. You can find these pieces of information by going to the [Developer Console][devconsole], clicking your project > APIs & auth > credentials.

For more information about OAuth2 and how it works, [see here][oauth].

#### A complete OAuth2 example

Let's take a look at a complete example.

``` js
const {OAuth2Client} = require('google-auth-library');
const http = require('http');
const url = require('url');
const querystring = require('querystring');
const opn = require('opn');

// Download your OAuth2 configuration from the Google
const keys = require('./keys.json');

/**
 * Start by acquiring a pre-authenticated oAuth2 client.
 */
async function main() {
  try {
    const oAuth2Client = await getAuthenticatedClient();
    // Make a simple request to the Google Plus API using our pre-authenticated client. The `request()` method
    // takes an AxiosRequestConfig object.  Visit https://github.com/axios/axios#request-config.
    const url = 'https://www.googleapis.com/plus/v1/people?query=pizza';
    const res = await oAuth2Client.request({url})
    console.log(res.data);
  } catch (e) {
    console.error(e);
  }
  process.exit();
}

/**
 * Create a new OAuth2Client, and go through the OAuth2 content
 * workflow.  Return the full client to the callback.
 */
function getAuthenticatedClient() {
  return new Promise((resolve, reject) => {
    // create an oAuth client to authorize the API call.  Secrets are kept in a `keys.json` file,
    // which should be downloaded from the Google Developers Console.
    const oAuth2Client = new OAuth2Client(
      keys.web.client_id,
      keys.web.client_secret,
      keys.web.redirect_uris[0]
    );

    // Generate the url that will be used for the consent dialog.
    const authorizeUrl = oAuth2Client.generateAuthUrl({
      access_type: 'offline',
      scope: 'https://www.googleapis.com/auth/plus.me'
    });

    // Open an http server to accept the oauth callback. In this simple example, the
    // only request to our webserver is to /oauth2callback?code=<code>
    const server = http.createServer(async (req, res) => {
      if (req.url.indexOf('/oauth2callback') > -1) {
        // acquire the code from the querystring, and close the web server.
        const qs = querystring.parse(url.parse(req.url).query);
        console.log(`Code is ${qs.code}`);
        res.end('Authentication successful! Please return to the console.');
        server.close();

        // Now that we have the code, use that to acquire tokens.
        const r = await oAuth2Client.getToken(qs.code)
        // Make sure to set the credentials on the OAuth2 client.
        oAuth2Client.setCredentials(r.tokens);
        console.info('Tokens acquired.');
        resolve(oAuth2Client);
      }
    }).listen(3000, () => {
      // open the browser to the authorize url to start the workflow
      opn(authorizeUrl);
    });
  });
}

main();
```

#### Handling token events
This library will automatically obtain an `access_token`, and automatically refresh the `access_token` if a `refresh_token` is present.  The `refresh_token` is only returned on the [first authorization]((https://github.com/google/google-api-nodejs-client/issues/750#issuecomment-304521450), so if you want to make sure you store it safely. An easy way to make sure you always store the most recent tokens is to use the `tokens` event:

```js
const client = await auth.getClient();

client.on('tokens', (tokens) => {
  if (tokens.refresh_token) {
    // store the refresh_token in my database!
    console.log(tokens.refresh_token);
  }
  console.log(tokens.access_token);
});

const url = `https://www.googleapis.com/dns/v1/projects/${projectId}`;
const res = await client.request({ url });
// The `tokens` event would now be raised if this was the first request
```

#### Retrieve access token
With the code returned, you can ask for an access token as shown below:

``` js
const tokens = await oauth2Client.getToken(code);
// Now tokens contains an access_token and an optional refresh_token. Save them.
oauth2Client.setCredentials(tokens);
```

#### Manually refreshing access token
If you need to manually refresh the `access_token` associated with your OAuth2 client, ensure the call to `generateAuthUrl` sets the `access_type` to `offline`.  The refresh token will only be returned for the first authorization by the user.  To force consent, set the `prompt` property to `consent`:

```js
// Generate the url that will be used for the consent dialog.
const authorizeUrl = oAuth2Client.generateAuthUrl({
  // To get a refresh token, you MUST set access_type to `offline`.
  access_type: 'offline',
  // set the appropriate scopes
  scope: 'https://www.googleapis.com/auth/plus.me',
  // A refresh token is only returned the first time the user
  // consents to providing access.  For illustration purposes,
  // setting the prompt to 'consent' will force this consent
  // every time, forcing a refresh_token to be returned.
  prompt: 'consent'
});
```

If a refresh_token is set again on `OAuth2Client.credentials.refresh_token`, you can can `refreshAccessToken()`:

``` js
const tokens = await oauth2Client.refreshAccessToken();
// your access_token is now refreshed and stored in oauth2Client
// store these new tokens in a safe place (e.g. database)
```

#### Checking `access_token` information
After obtaining and storing an `access_token`, at a later time you may want to go check the expiration date,
original scopes, or audience for the token.  To get the token info, you can use the `getTokenInfo` method:

```js
// after acquiring an oAuth2Client...
const tokenInfo = await oAuth2client.getTokenInfo('my-access-token');

// take a look at the scopes originally provisioned for the access token
console.log(tokenInfo.scopes);
```

This method will throw if the token is invalid.

#### OAuth2 with Installed Apps (Electron)
If you're authenticating with OAuth2 from an installed application (like Electron), you may not want to embed your `client_secret` inside of the application sources. To work around this restriction, you can choose the `iOS` application type when creating your OAuth2 credentials in the [Google Developers console][devconsole]:

![application type][apptype]

If using the `iOS` type, when creating the OAuth2 client you won't need to pass a `client_secret` into the constructor:
```js
const oAuth2Client = new OAuth2Client({
  clientId: <your_client_id>,
  redirectUri: <your_redirect_uri>
});
```

## JSON Web Tokens
The Google Developers Console provides a `.json` file that you can use to configure a JWT auth client and authenticate your requests, for example when using a service account.

``` js
const {JWT} = require('google-auth-library');
const keys = require('./jwt.keys.json');

async function main() {
  const client = new JWT(
    keys.client_email,
    null,
    keys.private_key,
    ['https://www.googleapis.com/auth/cloud-platform'],
  );
  await client.authorize();
  const url = `https://www.googleapis.com/dns/v1/projects/${keys.project_id}`;
  const res = await client.request({url});
  console.log(res.data);
}

main().catch(console.error);

```

The parameters for the JWT auth client including how to use it with a `.pem` file are explained in [examples/jwt.js](examples/jwt.js).

#### Loading credentials from environment variables
Instead of loading credentials from a key file, you can also provide them using an environment variable and the `GoogleAuth.fromJSON()` method.  This is particularly convenient for systems that deploy directly from source control (Heroku, App Engine, etc).

Start by exporting your credentials:

```
$ export CREDS='{
  "type": "service_account",
  "project_id": "your-project-id",
  "private_key_id": "your-private-key-id",
  "private_key": "your-private-key",
  "client_email": "your-client-email",
  "client_id": "your-client-id",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://accounts.google.com/o/oauth2/token",
  "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
  "client_x509_cert_url": "your-cert-url"
}'
```
Now you can create a new client from the credentials:

```js
const {auth} = require('google-auth-library');

// load the environment variable with our keys
const keysEnvVar = process.env['CREDS'];
if (!keysEnvVar) {
  throw new Error('The $CREDS environment variable was not found!');
}
const keys = JSON.parse(keysEnvVar);

async function main() {
  // load the JWT or UserRefreshClient from the keys
  const client = auth.fromJSON(keys);
  client.scopes = ['https://www.googleapis.com/auth/cloud-platform'];
  await client.authorize();
  const url = `https://www.googleapis.com/dns/v1/projects/${keys.project_id}`;
  const res = await client.request({url});
  console.log(res.data);
}

main().catch(console.error);
```
#### Using a Proxy
You can use the following environment variables to proxy HTTP and HTTPS requests:

- `HTTP_PROXY` / `http_proxy`
- `HTTPS_PROXY` / `https_proxy`

When HTTP_PROXY / http_proxy are set, they will be used to proxy non-SSL requests that do not have an explicit proxy configuration option present. Similarly, HTTPS_PROXY / https_proxy will be respected for SSL requests that do not have an explicit proxy configuration option. It is valid to define a proxy in one of the environment variables, but then override it for a specific request, using the proxy configuration option.


## Compute
If your application is running on Google Cloud Platform, you can authenticate using the default service account or by specifying a specific service account.

**Note**: In most cases, you will want to use [Application Default Credentials](choosing-the-correct-credential-type-automatically).  Direct use of the `Compute` class is for very specific scenarios.

``` js
const {Compute} = require('google-auth-library');

async function main() {
  const client = new Compute({
    // Specifying the service account email is optional.
    serviceAccountEmail: 'my-service-account@example.com'
  });
  const projectId = 'your-project-id';
  const url = `https://www.googleapis.com/dns/v1/projects/${project_id}`;
  const res = await client.request({url});
  console.log(res.data);
}

main().catch(console.error);

```

## Questions/problems?

* Ask your development related questions on [Stack Overflow][stackoverflow].
* If you've found an bug/issue, please [file it on GitHub][bugs].

## Contributing

See [CONTRIBUTING][contributing].

## License

This library is licensed under Apache 2.0. Full license text is available in [LICENSE][copying].

[apiexplorer]: https://developers.google.com/apis-explorer
[Application Default Credentials]: https://developers.google.com/identity/protocols/application-default-credentials#callingnode
[apptype]: https://user-images.githubusercontent.com/534619/36553844-3f9a863c-17b2-11e8-904a-29f6cd5f807a.png
[authdocs]: https://developers.google.com/accounts/docs/OAuth2Login
[axios]: https://github.com/axios/axios
[axiosOpts]: https://github.com/axios/axios#request-config
[bugs]: https://github.com/google/google-auth-library-nodejs/issues
[circle-image]: https://circleci.com/gh/google/google-auth-library-nodejs.svg?style=svg
[circle-url]: https://circleci.com/gh/google/google-auth-library-nodejs
[codecov-image]: https://codecov.io/gh/google/google-auth-library-nodejs/branch/master/graph/badge.svg
[codecov-url]: https://codecov.io/gh/google/google-auth-library-nodejs
[contributing]: https://github.com/google/google-auth-library-nodejs/blob/master/.github/CONTRIBUTING.md
[copying]: https://github.com/google/google-auth-library-nodejs/tree/master/LICENSE
[david-dm-img]: https://david-dm.org/google/google-auth-library-nodejs/status.svg
[david-dm]: https://david-dm.org/google/google-auth-library-nodejs
[greenkeeperimg]: https://badges.greenkeeper.io/google/google-auth-library-nodejs.svg
[greenkeeper]: https://greenkeeper.io/
[node]: http://nodejs.org/
[npmimg]: https://img.shields.io/npm/v/google-auth-library.svg
[npm]: https://www.npmjs.org/package/google-auth-library
[oauth]: https://developers.google.com/identity/protocols/OAuth2
[snyk-image]: https://snyk.io/test/github/google/google-auth-library-nodejs/badge.svg
[snyk-url]: https://snyk.io/test/github/google/google-auth-library-nodejs
[stability]: http://nodejs.org/api/stream.html#stream_stream
[stackoverflow]: http://stackoverflow.com/questions/tagged/google-auth-library-nodejs
[stream]: http://nodejs.org/api/stream.html#stream_class_stream_readable
[urlshort]: https://developers.google.com/url-shortener/
[usingkeys]: https://developers.google.com/console/help/#UsingKeys
[devconsole]: https://console.developer.google.com
[oauth]: https://developers.google.com/accounts/docs/OAuth2
[options]: https://github.com/google/google-auth-library-nodejs/tree/master#options
[gcloud]: https://github.com/GoogleCloudPlatform/gcloud-node
[cloudplatform]: https://developers.google.com/cloud/

