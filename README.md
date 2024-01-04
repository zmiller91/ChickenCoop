# Premium Functionality

* Update Pi remotely
    * Connects to AWS IoT


# Free Functionality

* Update locally
* Add components
* 7 days of historical data

# Implementation

- Interfaces are built to describe basic functionality
- Separate interface implementations are written for local vs remote interaction
- Two artifacts are built, one with remote classes and the other with local classes
- Tables between remote and local need to by synced, don't know how to do this yet.

# Remote vs Local Classes

- SerialRepository needs to add entries into the serial table in order to attach components
- CoopStateProvider needs to fetch from a local source instead of AWS IoT

Database updates dont remotely are sent in a topic and the same change is mirrored locally. All config is derived via
the database, both remotely and locally. When opting into a premium subscription, local data can be synced to the remote
server through the usage of presigned s3 urls: https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-presigned-url.html

Premium Data Flow

1. User creates an entity remotely (e.g. component, configuration, etc)
2. Remote server saves it to MySQL
3. Remote server pushes it to Pi
4. Pi saves it to MySQl

1. Pi collects metric data
2. Pi saves it to MySQL
3. Pi checks it if has a connection to remote
4. Pi pushes it remotely
5. Remote saves it to MySQL

Free Data Flow

1. User creates an entity locally (e.g. component, configuration, etc)
2. Pi saves it to MySQL

1. Pi collects metric data
2. Pi saves it to MySQL
3. Pi checks if it has a connection to remote
4. Pi does nothing

Free to Premium Data Flow

1. User opts into premium service
2. Pi is granted access to subscribe to MQTT
3. Pi asks for an updates
4. Pi is given a presigned S3 URL
5. Pi dumps all local information to the presigned S3 URL
6. Pi tells remote that it's finished syncing
7. Remote synces the presigned S3 url to MySQL
8. Sync is finished