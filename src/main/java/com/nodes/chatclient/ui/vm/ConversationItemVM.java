package com.nodes.chatclient.ui.vm;

import com.nodes.chatclient.store.model.Conversation;
import javafx.beans.property.*;

public final class ConversationItemVM {
    private final String peerId;
    private final StringProperty lastMessage = new SimpleStringProperty("");
    private final LongProperty lastTimestamp = new SimpleLongProperty(0);
    private final IntegerProperty unreadCount = new SimpleIntegerProperty(0);
    private final BooleanProperty online = new SimpleBooleanProperty(false);

    public ConversationItemVM(String peerId) {
        this.peerId = peerId;
    }

    public void updateFrom(Conversation convo) {
        lastMessage.set(convo.lastMessage);
        lastTimestamp.set(convo.lastTimestamp);
        unreadCount.set(convo.unreadCount);
    }

    public String getPeerId() {
        return peerId;
    }

    public long getLastTimestamp() {
        return lastTimestamp.get();
    }

    public StringProperty lastMessageProperty() {
        return lastMessage;
    }

    public IntegerProperty unreadCountProperty() {
        return unreadCount;
    }

    public BooleanProperty onlineProperty() {
        return online;
    }
}
