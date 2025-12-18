package com.nodes.chatclient.ui.controllers;

import com.nodes.chatclient.AppContext;
import com.nodes.chatclient.ui.vm.ChatViewModel;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

public class ChatController {

    private final Parent root;

    public ChatController(AppContext ctx, ChatViewModel vm) {

        Label label = new Label("Chat with " + vm.getPeerId());

        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(15));
        pane.setTop(label);

        this.root = pane;
    }

    public Parent getRoot() {
        return root;
    }
}
