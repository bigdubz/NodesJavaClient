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
    createdAt INTEGER NOT NULL,
    receivedAt INTEGER NOT NULL,
    isOutgoing INTEGER NOT NULL CHECK (isOutgoing IN (0,1)),
    type INTEGER NOT NULL CHECK (type IN (0,1,2)),  -- TEXT=0, REACTION=1, CONTROL=2
    deliveryStatus INTEGER NOT NULL CHECK (deliveryStatus IN (0,1,2)), -- SENT=0, DELIVERED=1, READ=2

    -- CONTROL:
    controlType INTEGER CHECK (controlType IN (0,1,2)), -- ACK=0, TYPING=1, READ_RECEIPT=2

    -- TEXT:
    body TEXT,

    -- REACTION:
    reaction TEXT,
    reactionIsRemoved INTEGER CHECK (reactionIsRemoved IN (0,1)),

    referencedMessageId TEXT,

    CHECK (
        -- TEXT
        (type = 0 AND body IS NOT NULL AND controlType IS NULL AND reaction IS NULL AND reactionIsRemoved IS NULL)

            OR -- REACTION
        (type = 1 AND reaction IS NOT NULL AND referencedMessageId IS NOT NULL AND reactionIsRemoved IS NOT NULL
            AND body IS NULL AND controlType IS NULL)

            OR -- CONTROL: ACK or READ_RECEIPT require reference
        (type = 2 AND controlType IS NOT NULL AND controlType IN (0,2) AND referencedMessageId IS NOT NULL
            AND body IS NULL AND reaction IS NULL AND reactionIsRemoved IS NULL)

            OR -- CONTROL: TYPING must NOT reference
        (type = 2 AND controlType IS NOT NULL AND controlType = 1 AND referencedMessageId IS NULL
            AND body IS NULL AND reaction IS NULL AND reactionIsRemoved IS NULL)
    ),
    CHECK (referencedMessageId IS NULL OR referencedMessageId != messageId),
    FOREIGN KEY (referencedMessageId) REFERENCES messages(messageId),
    FOREIGN KEY (conversationId) REFERENCES conversations(conversationId)
);

CREATE INDEX IF NOT EXISTS idx_messages_conversation ON messages(conversationId, createdAt);
CREATE INDEX IF NOT EXISTS idx_messages_delivery_status ON messages(conversationId, deliveryStatus);
CREATE INDEX IF NOT EXISTS idx_messages_reference ON messages(referencedMessageId);


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
