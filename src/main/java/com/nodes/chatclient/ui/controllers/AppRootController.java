package com.nodes.chatclient.ui.controllers;

import com.nodes.chatclient.AppContext;
import com.nodes.chatclient.store.ChatStore;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public final class AppRootController {
    private final StackPane root = new StackPane();
    private final LoginController loginController;
    private MainController mainController;
    
    private final AppContext ctx;
    
    public AppRootController(AppContext ctx, Stage stage) {
        this.ctx = ctx;
        loginController = new LoginController(
                ctx, 
                userId -> showMain(stage, userId)
        );
        
        root.getChildren().add(loginController.getRoot());
    }
    
    public Parent getRoot() {
        return root;
    }
    
    private void showMain(Stage stage, String userId) {
        ChatStore store = new ChatStore(userId);
        
        ctx.router.clearHandlers();
        ctx.router.registerServerHandlers(store);

        String deviceId;
        try {
            deviceId = ctx.localIdentity.deviceId();
            if (deviceId == null || deviceId.isBlank()) {
                throw new IllegalStateException("Local identity has no device id");
            }
            ctx.wsService.setAuth(userId, deviceId, ctx.jwt);
        } catch (Exception e) {
            System.err.println("Failed to load local identity: " + e.getMessage());
            return;
        }

        ctx.wsService.connectThenWaitAuth()
                .thenCompose(v -> {
                    ctx.jwt = ctx.wsService.deviceToken();
                    return ctx.chatApi.getConversationsAsync(ctx.jwt);
                })
                .thenAccept(store::mergeConversations)
                .thenRun(() -> {
                    ctx.wsService.enableRoutingAndFlush();
                    Platform.runLater(() -> {
                        mainController = new MainController(
                                ctx,
                                store,
                                this::logout
                        );

                        root.getChildren().setAll(mainController.getRoot());
                        stage.setTitle("Nodes");
                    });
                })
                .exceptionally(err -> {
                    Platform.runLater(() -> System.err.println("WS auth failed: " + err.getMessage()));
                    return null;
                });
    }

    private void logout() {
        ctx.wsService.disconnect();
        ctx.wsService.disableRouting();
        ctx.jwt = null;
        ctx.userId = null;

        if (mainController != null) {
            mainController.reset();
            mainController = null;
        }

        loginController.reset();
        root.getChildren().setAll(loginController.getRoot());
    }
}
