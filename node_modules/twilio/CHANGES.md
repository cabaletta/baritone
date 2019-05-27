twilio-node changelog
=====================

[2018-08-23] Version 3.19.2
----------------------------
**Library**
- PR #372: Tests for typescript. Thanks to @ekarson!

**Api**
- Add Proxy Active Sessions category to usage records

**Chat**
- Add User Channel instance resource

**Preview**
- Add `Actions` endpoints and remove `ResponseUrl` from assistants on the Understand api

**Pricing**
- add voice_country resource (v2)


[2018-08-09] Version 3.19.1
----------------------------
**Preview**
- Add new Intent Statistics endpoint
- Remove `ttl` from Assistants

**Studio**
- Studio is now GA


[2018-08-03] Version 3.19.0
----------------------------
**Library**
- PR #371: Add test for attributes with dashes. Thanks to @ekarson!
- PR #366: Tag and push Docker latest image when deploying with TravisCI. Thanks to @jonatasbaldin!

**Api**
- Add support for sip domains to map credential lists for registrations

**Chat**
- Make message From field updatable
- Add REST API webhooks

**Notify**
- Removing deprecated `segments`, `users`, `segment_memberships`, `user_bindings` classes from helper libraries. **(breaking change)**

**Preview**
- Remove `ttl` from Assistants

**Proxy**
- Enable setting a proxy number as reserved

**Video**
- Add `group-small` room type

**Twiml**
- Add `Connect` and `Room` for Programmable Video Rooms
- Add support for SSML lang tag on Say verb


[2018-07-16] Version 3.18.0
----------------------------
**Library**
- PR #365: Add a request body validator. Thanks to @cjcodes!

**Twiml**
- Add support for SSML on Say verb, the message body is changed to be optional **(breaking change)**


[2018-07-11] Version 3.17.6
----------------------------
**Library**
- PR #362: Remove old Precise env and sudo flag on TravisCI. Thanks to @jonatasbaldin!

**Api**
- Add `cidr_prefix_length` param to SIP IpAddresses API

**Studio**
- Add new /Execution endpoints to begin Engagement -> Execution migration

**Video**
- [Rooms] Allow deletion of individual recordings from a room


[2018-07-05] Version 3.17.5
----------------------------
**Library**
- PR #358: Add Dockerfile and related changes to build the Docker image. Thanks to @jonatasbaldin!
- PR #361: Regenerate with structured params/properties. Thanks to @ekarson!

**Api**
- Release `Call Recording Controls` feature support in helper libraries
- Add Voice Insights sub-category keys to usage records


[2018-06-21] Version 3.17.4
----------------------------
**Library**
- PR #359: Add test for mixed content. Thanks to @ekarson!
- PR #357: Allow creating generic twiml nodes. Thanks to @cjcodes!

**Api**
- Add Fraud Lookups category to usage records

**Video**
- Allow user to set `ContentDisposition` when obtaining media URLs for Room Recordings and Compositions
- Add Composition Settings resource


[2018-06-15] Version 3.17.3
----------------------------
**Library**
- PR #354: Add validateSslCertificate to node client. Thanks to @mbichoffe!
- PR #355: Add addText method to TwiML classes. Thanks to @ekarson!
- PR #356: Update nsp to latest. Thanks to @ekarson!

**Twiml**
- Add methods to helper libraries to inject arbitrary text under a TwiML node


[2018-06-05] Version 3.17.2
----------------------------
**Library**
- PR #353: Update package-lock.json. Thanks to @cjcodes!

**Chat**
- Add Binding and UserBinding documentation

**Lookups**
- Add back support for `fraud` lookup type


[2018-05-25] Version 3.17.1
----------------------------
**Library**
- PR #349: Update Contributing section in README.md. Thanks to @dkundel!
- PR #347: Update dependencies to fix vulnerabilities (#343). Thanks to @dkundel!
- PR #351: Update request dependency. Thanks to @cjcodes!

**Api**
- Add more programmable video categories to usage records
- Add 'include_subaccounts' parameter to all variation of usage_record fetch

**Studio**
- Add endpoint to delete engagements

**Trunking**
- Added cnam_lookup_enabled parameter to Trunk resource.
- Added case-insensitivity for recording parameter to Trunk resource.


[2018-05-11] Version 3.17.0
----------------------------
**Library**
- PR #340: Update request version RE sec. advisory #606. Thanks to @cjcodes!

**Chat**
- Add Channel Webhooks resource

**Monitor**
- Update event filtering to support date/time **(breaking change)**

**Wireless**
- Updated `maturity` to `ga` for all wireless apis


[2018-04-28] Version 3.16.0
----------------------------
**Library**
- PR #337: Upgrade lodash. Thanks to @YasharF!

**Video**
- Redesign API by adding custom `VideoLayout` object. **(breaking change)**


[2018-04-20] Version 3.15.1
----------------------------
**Twiml**
- Gather input Enum: remove unnecessary "dtmf speech" value as you can now specify multiple enum values for this parameter and both "dtmf" and "speech" are already available.


[2018-04-13] Version 3.15.0
----------------------------
**Library**
- PR #334: Add incoming.allow to AccessToken VoiceGrant. Thanks to @ryan-rowland!
- PR #335: use _.isDate to check if object is a Date object. Thanks to @joliveros!

**Preview**
- Support for Understand V2 APIs - renames various resources and adds new fields

**Studio**
- Change parameters type from string to object in engagement resource

**Video**
- [Recordings] Change `size` type to `long`. **(breaking change)**


[2018-03-22] Version 3.14.0
----------------------------
**Lookups**
- Disable support for `fraud` lookups *(breaking change)*

**Preview**
- Add `BuildDuration` and `ErrorCode` to Understand ModelBuild

**Studio**
- Add new /Context endpoint for step and engagement resources.


[2018-03-12] Version 3.13.1
----------------------------
**Api**
- Add `caller_id` param to Outbound Calls API
- Release `trim` recording Outbound Calls API functionality in helper libraries

**Video**
- [composer] Add `room_sid` to Composition resource.

**Twiml**
- Adds support for passing in multiple input type enums when setting `input` on `Gather`


[2018-03-02] Version 3.13.0
----------------------------
**Library**
- Add `toJSON` methods on all instance objects to allow serialization to json and remove circular references.

**Studio**
- Add new /Context endpoint for step and engagement resources. Removes the context property from existing step and engagement resources. *(breaking change)*


[2018-02-26] Version 3.12.0
----------------------------
**Important Notice**
- Node v0.12 is no longer supported.

**Api**
- Add `trim` param to Outbound Calls API

**Lookups**
- Add support for `fraud` lookup type

**Numbers**
- Initial Release

**Video**
- [composer] Add `SEQUENCE` value to available layouts, and `trim` and `reuse` params.


[2018-02-09] Version 3.11.3
----------------------------
**Api**
- Add `AnnounceUrl` and `AnnounceMethod` params for conference announce

**Chat**
- Add support to looking up user channels by identity in v1


[2018-01-30] Version 3.11.2
----------------------------
**Api**
- Add `studio-engagements` usage key

**Preview**
- Remove Studio Engagement Deletion

**Studio**
- Initial Release

**Video**
- [omit] Beta: Allow updates to `SubscribedTracks`.
- Add `SubscribedTracks`.
- Add track name to Video Recording resource
- Add Composition and Composition Media resources


[2018-01-22] Version 3.11.1
----------------------------
**Library**
- PR #315: Add 'forever' as an option to RequestClient request method. Thanks @vzhidal!
- PR #311: Fix X-Twilio-Signature validation when URL has '?'. Thanks @alexcchan!
- PR #305: Update momentjs to address NSP 532 ReDoS advisory. Thanks @jhdielman!

**Api**
- Add `conference_sid` property on Recordings
- Add proxy and sms usage key

**Chat**
- Make user channels accessible by identity
- Add notifications logs flag parameter

**Fax**
- Added `ttl` parameter
  `ttl` is the number of minutes a fax is considered valid.

**Preview**
- Add `call_delay`, `extension`, `verification_code`, and `verification_call_sids`.
- Add `failure_reason` to HostedNumberOrders.
- Add DependentHostedNumberOrders endpoint for AuthorizationDocuments preview API.


[2017-12-15] Version 3.11.0
----------------------------
**Api**
- Add `voip`, `national`, `shared_cost`, and `machine_to_machine` sub-resources to `/2010-04-01/Accounts/{AccountSid}/AvailablePhoneNumbers/{IsoCountryCode}/`
- Add programmable video keys

**Preview**
- Add `verification_type` and `verification_document_sid` to HostedNumberOrders.

**Proxy**
- Fixed typo in session status enum value

**Twiml**
- Fix Dial record property incorrectly typed as accepting TrimEnum values when it actually has its own enum of values. *(breaking change)*
- Add `priority` and `timeout` properties to Task TwiML.
- Add support for `recording_status_callback_event` for Dial verb and for Conference


[2017-12-01] Version 3.10.1
----------------------------
**Api**
- Use the correct properties for Dependent Phone Numbers of an Address *(breaking change)*
- Update Call Recordings with the correct properties

**Preview**
- Add `status` and `email` query param filters for AuthorizationDocument list endpoint

**Proxy**
- Added DELETE support to Interaction
- Standardized enum values to dash-case
- Rename Service#friendly_name to Service#unique_name

**Video**
- Remove beta flag from `media_region` and `video_codecs`

**Wireless**
- Bug fix: Changed `operator_mcc` and `operator_mnc` in `DataSessions` subresource from `integer` to `string`


[2017-11-17] Version 3.10.0
----------------------------
**Sync**
- Add TTL support for Sync objects *(breaking change)*
  - The required `data` parameter on the following actions is now optional: "Update Document", "Update Map Item", "Update List Item"
  - New actions available for updating TTL of Sync objects: "Update List", "Update Map", "Update Stream"

**Video**
- [bi] Rename `RoomParticipant` to `Participant`
- Add Recording Settings resource
- Expose EncryptionKey and MediaExternalLocation properties in Recording resource


[2017-11-13] Version 3.9.3
---------------------------
**Accounts**
- Add AWS credential type

**Preview**
- Removed `iso_country` as required field for creating a HostedNumberOrder.

**Proxy**
- Added new fields to Service: geo_match_level, number_selection_behavior, intercept_callback_url, out_of_session_callback_url


[2017-11-03] Version 3.9.2
---------------------------
**Api**
- Add programmable video keys

**Video**
- Add `Participants`


[2017-10-27] Version 3.9.1
---------------------------
**Chat**
- Add Binding resource
- Add UserBinding resource


[2017-10-20] Version 3.9.0
---------------------------
**TwiML**
- Update all TwiML Resources with latest parameters
- Autogenerate TwiML resources for faster updates

**Api**
- Add `address_sid` param to IncomingPhoneNumbers create and update
- Add 'fax_enabled' option for Phone Number Search


[2017-10-13] Version 3.8.1
---------------------------
**Api**
- Add `smart_encoded` param for Messages
- Add `identity_sid` param to IncomingPhoneNumbers create and update

**Preview**
- Make 'address_sid' and 'email' optional fields when creating a HostedNumberOrder
- Add AuthorizationDocuments preview API.

**Proxy**
- Initial Release

**Wireless**
- Added `ip_address` to sim resource


[2017-10-06] Version 3.8.0
---------------------------
**Preview**
- Add `acc_security` (authy-phone-verification) initial api-definitions

**Taskrouter**
- [bi] Less verbose naming of cumulative and real time statistics


[2017-09-28] Version 3.7.0
---------------------------
**Chat**
- Make member accessible through identity.
- Make channel subresources accessible by channel unique name.
- Set get list 'max_page_size' parameter to 100.
- Add service instance webhook retry configuration.
- Add media message capability.
- Make body an optional parameter on Message creation.

**Notify**
- `data`, `apn`, `gcm`, `fcm`, `sms` parameters in `Notifications` create resource are objects instead of strings. *(breaking change)*

**Taskrouter**
- Add new query ability by TaskChannelSid or TaskChannelUniqueName.
- Move Events, Worker, Workers endpoint over to CPR.
- Add new RealTime and Cumulative Statistics endpoints.

**Video**
- Create should allow an array of video_codecs.
- Add video_codecs as a property of room to make it externally visible.


[2017-09-15] Version 3.6.7
---------------------------
**Api**
- Add `sip_registration` property on SIP Domains
- Add new video and market usage category keys
- Support transferring IncomingPhoneNumbers between accounts.


[2017-09-01] Version 3.6.6
---------------------------
- Add lastResponse and lastRequest to Http::Client

[2017-09-01] Version 3.6.5
---------------------------
**Sync**
- Add support for Streams

**Wireless**
- Added DataSessions sub-resource to Sims.


[2017-08-25] Version 3.6.4
---------------------------
**Api**
- Update `status` enum for Recordings to include 'failed'
- Add `error_code` property on Recordings

**Chat**
- Add mutable parameters for channel, members and messages

**Video**
- New `media_region` parameter when creating a room, which controls which region media will be served out of.
- Add `video_codec` enum and `video_codecs` parameter, which can be set to either `VP8` or `H264` during room creation.


[2017-08-18] Version 3.6.3
---------------------------
**Api**
- Add VoiceReceiveMode {'voice', 'fax'} option to IncomingPhoneNumber UPDATE requests

**Chat**
- Add channel message media information
- Add service instance message media information

**Preview**
- Removed 'email' from bulk_exports configuration api [bi]. No migration plan needed because api has not been used yet.
- Add AvailableNumbers resource.
- Add DeployedDevices.

**Sync**
- Add support for Service Instance unique names


[2017-08-10] Version 3.6.2
---------------------------
**Api**
- Add New wireless usage keys added
- Add `auto_correct_address` param for Addresses create and update

**Video**
- Add `video_codec` enum and `video_codecs` parameter, which can be set to either `VP8` or `H264` during room creation.
- Restrict recordings page size to 100


[2017-07-27] Version 3.6.1
---------------------------

- Support SSL connection/session reuse.

[2017-07-27] Version 3.6.0
---------------------------
This release adds Beta and Preview products to main artifact.

Previously, Beta and Preview products were only included in the alpha artifact.
They are now being included in the main artifact to ease product
discoverability and the collective operational overhead of maintaining multiple
artifacts per library.

**Api**
- Remove unused `encryption_type` property on Recordings *(breaking change)*
- Update `status` enum for Messages to include 'accepted'

**Messaging**
- Fix incorrectly typed capabilities property for PhoneNumbers.

**Notify**
- Add `ToBinding` optional parameter on Notifications resource creation. Accepted values are json strings.

**Preview**
- Add `sms_application_sid` to HostedNumberOrders.

**Taskrouter**
- Fully support conference functionality in reservations.


[2017-07-13] Version 3.5.0
---------------------------

- Bump `jsonwebtoken` from 5.4.x to 7.4.1.
- Bump `xmlbuilder` from 8.2.2 to 9.0.1.
- Detect and fail install when node not present.

**Api**
- Update `AnnounceMethod` parameter naming for consistency

**Notify**
- Add `ToBinding` optional parameter on Notifications resource creation. Accepted values are json strings.

**Preview**
- Add `verification_attempts` to HostedNumberOrders.
- Add `status_callback_url` and `status_callback_method` to HostedNumberOrders.

**Video**
- Filter recordings by date using the parameters `DateCreatedAfter` and `DateCreatedBefore`.
- Override the default time-to-live of a recording's media URL through the `Ttl` parameter (in seconds, default value is 3600).
- Add query parameters `SourceSid`, `Status`, `DateCreatedAfter` and `DateCreatedBefore` to the convenience method for retrieving Room recordings.

**Wireless**
- Added national and international data limits to the RatePlans resource.


[2017-06-16] Version 3.4.0
--------------------------

- Remove client-side max page size validation.
- Bump moment to 2.18.1 to fix security vulnerability.
- Fix Node 0.12 tests and test against Node 8.
- Add `<Sim>` to TwiML.
- Add `locality` field to `AvailablePhoneNumbers`.
- Add `origin` field to `IncomingPhoneNumbers`.
- Add `inLocality` parameter to `AvailablePhoneNumbers`.
- Add `origin` parameter to `IncomingPhoneNumbers`.
- Add `getPage` method for reentrant paging to all list resources.

[2017-05-24] Version 3.3.0
--------------------------

- Document new TwiML parameters.

[2017-05-22] Version 3.2.0
--------------------------

- Rename room `Recordings` resource to `RoomRecordings` to avoid class name conflict (backwards incompatible).

[2017-05-19] Version 3.1.0
--------------------------

- Add video domain.


[2017-05-03] Version 3.0.0
--------------------------------
**New Major Version**

The newest version of the `twilio-node` helper library!

This version brings a host of changes to update and modernize the `twilio-node` helper library. It is auto-generated to produce a more consistent and correct product.

- [Migration Guide](https://www.twilio.com/docs/libraries/node/migration-guide)
- [Full API Documentation](https://twilio.github.io/twilio-node/)
- [General Documentation](https://www.twilio.com/docs/libraries/node)
