#SCIM Configuration

The following properties are used to configure the I2 SCIM server:

##Basic

The I2 SCIM server implements a dynamically configured schema system. That is to say, there are no *hard-coded* resource types and new ones may be added on demand. In the current release, the system requires a reboot to load new definitions.

**scim.schema.path** - The path to an initial JSON schema file to load into the server. The default loads the default from [RFC7643](https://tools.ietf.org/html/rfc7643).

**scim.resourcetype.path** - The path to an initial JSON file to load resource type definitions into the server. The file defines endpoints defined in [RFC7643](https://tools.ietf.org/html/rfc7643).

**scim.coreSchema.path** - The path to an initial JSON file that defines common SCIM attributes such as id, externalId, meta that are found on all SCIM resources. Normally this file cannot be changed, however, some attribute qualities such as return-ability can be altered.

**scim.thread.count** - The maximum number of *worker* threads per server. While an I2 server may serve thousands of simultaneous requests, requests are processed through a set of worker threads to optimize throughput to backend persistence services. This number should be tuned based on processor and database characteristics.

**scim.json.pretty** - When true, JSON results are formatted with spacing and line-returns for easy reading.  (DEFAULT: false)

**logging.level.com.independentid.scim** - The console logging level desired. (DEFAULT: info)


##Persistence
As currently implemented, I2 SCIM supports the MongoDB as its persistence database due to it document centric architecture. 

**scim.provider.bean** - Indicates a named bean that implements the IScimProvider interface. (Default: MongoDao / I2 SCIM MongoProvider).

**Mongo Configuration**

**scim.mongodb.uri** - The URI of the Mongo DB service. (Default: mongodb://localhost:27017)

**scim.mongodb.dbname** - The name of the database to use in Mongo. (Default: SCIM)

**scim.mongodb.indexes** - SCIM attributes to index. (Default: User:userName,User:emails.value,Group:displayName)

**scim.mongodb.test** - When enabled, the I2 SCIM Server will re-initialize the database including re-loading the default schema from json files. CAUTION: This will destroy all data identified by scim.mongodb.dbname. (Default: false) 

##Security
**scim.security.enable** - This parameter be used to disable authentication and authorization in the server. This is most often used for protocol testing and in certain deployment configurations where security is applied by another component. (DEFAULT: true)

**scim.security.authen.jwt** - This parameter is used to turn on support for JWT Bearer tokens. See spring.security.oauth2.jwt

**spring.security.oauth2.resourceserver.jwt.issuer-uri** - The URI used for JWT Bearer tokens.

**spring.security.oauth2.resourceserver.jwt.jwk-set-uri** - The URI used to locate the JWKS public key set for the token issuer.  This method is preferred as the server can load new keys automatically should the issuer change keys.


##SCIM Protocol
**scim.query.max.resultsize** - The maximum number of resources returned in a query (DEFAULT: 1000).

**scim.bulk.max.ops** - The maximum number of operations that can be submitted in a single bulk request (DEFAULT: 1000).

**scim.bulk.max.errors** - The maximum number of errors that can be tolerated in a single bulk request (DEFAULT: 5). Note that the result returned indicates which operations succeeded, which one failed, and those aborted.
