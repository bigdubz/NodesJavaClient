CREATE TABLE IF NOT EXISTS schema_version (
    version INTEGER PRIMARY KEY
);


CREATE TABLE IF NOT EXISTS device_identity (
    userId TEXT NOT NULL,
    deviceId TEXT NOT NULL,
    registrationId INTEGER NOT NULL,

    signingPublicKey BLOB NOT NULL,
    signingPrivateKey BLOB NOT NULL,

    identityPublicKey BLOB NOT NULL,
    identityPrivateKey BLOB NOT NULL,

    createdAt INTEGER NOT NULL,

    PRIMARY KEY (userId, deviceId)
);


CREATE TABLE IF NOT EXISTS signed_prekeys (
    keyId INTEGER PRIMARY KEY,

    publicKey BLOB NOT NULL,
    privateKey BLOB NOT NULL,
    signature BLOB NOT NULL,

    createdAt INTEGER NOT NULL,
    isActive INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_spk_active ON signed_prekeys(isActive);


CREATE TABLE IF NOT EXISTS one_time_prekeys (
    keyId INTEGER PRIMARY KEY,

    publicKey BLOB NOT NULL,
    privateKey BLOB NOT NULL,

    isUsed INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_opk_unused ON one_time_prekeys(isUsed);


CREATE TABLE IF NOT EXISTS sessions (
    sessionId TEXT PRIMARY KEY,  -- this will be userId:deviceId

    remoteUserId TEXT NOT NULL,
    remoteDeviceId TEXT NOT NULL,

    sessionBlob BLOB NOT NULL,

    updatedAt INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_sessions_lookup ON sessions(remoteUserId, remoteDeviceId);


CREATE TABLE IF NOT EXISTS contacts (
    userId TEXT NOT NULL,
    deviceId TEXT NOT NULL,

    identityKey BLOB NOT NULL,
    signingKey BLOB NOT NULL,

    PRIMARY KEY (userId, deviceId)
);


CREATE TABLE IF NOT EXISTS messages (
    messageId TEXT PRIMARY KEY,
    conversationId TEXT NOT NULL,
    senderUserId TEXT NOT NULL,
    senderDeviceId TEXT NOT NULL,
    type INTEGER NOT NULL,  -- TEXT=0, REACTION=1, CONTROL=2
    createdAt INTEGER NOT NULL,
    body TEXT,
    status INTEGER NOT NULL, -- SENT=0, DELIVERED=1, READ=2
    referencedMessageId TEXT
);

CREATE INDEX IF NOT EXISTS idx_messages_conversation ON messages(conversationId, createdAt);
CREATE INDEX IF NOT EXISTS idx_messages_status ON messages(status);


CREATE TABLE IF NOT EXISTS outbound_queue (
    queueId INTEGER PRIMARY KEY AUTOINCREMENT,

    toUserId TEXT NOT NULL,
    toDeviceId TEXT NOT NULL,

    envelopeBlob BLOB NOT NULL,

    createdAt INTEGER NOT NULL,
    retryCount INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_outbound_lookup ON outbound_queue(toUserId, toDeviceId);


CREATE TABLE IF NOT EXISTS conversations (
    conversationId TEXT PRIMARY KEY,

    lastMessageId TEXT,
    lastUpdated INTEGER NOT NULL,

    unreadCount INTEGER NOT NULL DEFAULT 0
);
