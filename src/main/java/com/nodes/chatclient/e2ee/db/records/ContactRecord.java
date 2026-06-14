package com.nodes.chatclient.e2ee.db.records;

import com.nodes.chatclient.http.dto.Contact;

public record ContactRecord(
        String userId,
        String deviceId,
        byte[] identityKey,
        byte[] signingKey
) {

    public static ContactRecord from(Contact contact) {
        return new ContactRecord(contact.userId(), contact.deviceId(), contact.ik(), contact.sk());
    }
}
