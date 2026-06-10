package com.nodes.chatclient.ui.controllers;

import com.nodes.chatclient.AppContext;
import com.nodes.chatclient.e2ee.db.DatabaseManager;
import com.nodes.chatclient.e2ee.db.stores.ContactStore;
import com.nodes.chatclient.e2ee.db.stores.MessageStore;
import com.nodes.chatclient.e2ee.db.stores.OneTimePrekeyStore;
import com.nodes.chatclient.e2ee.db.stores.SessionStore;
import com.nodes.chatclient.e2ee.db.stores.SignedPrekeyStore;
import com.nodes.chatclient.e2ee.provisioning.*;
import com.nodes.chatclient.store.ChatStore;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.concurrent.CompletionException;

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
        ContactStore contactStore = new ContactStore(DatabaseManager.get());
        MessageStore messageStore = new MessageStore(DatabaseManager.get());
        SessionStore sessionStore = new SessionStore(DatabaseManager.get());
        SignedPrekeyStore signedPrekeyStore = new SignedPrekeyStore(DatabaseManager.get());
        OneTimePrekeyStore oneTimePrekeyStore = new OneTimePrekeyStore(DatabaseManager.get());

        ChatStore store = new ChatStore(
                userId,
                ctx.localIdentity.deviceId(),
                contactStore,
                messageStore,
                new MessageDecryptionService(
                        ctx.localIdentity,
                        contactStore,
                        sessionStore,
                        signedPrekeyStore,
                        oneTimePrekeyStore,
                        ctx.wsService
                )
        );
        store.loadLocalConversations();
        
        ctx.router.clearHandlers();
        ctx.router.registerServerHandlers(store, ctx.wsService);

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

        ctx.bundleProvisioningService = new BundleProvisioningService(
                ctx.bundlesApi,
                ctx.localIdentity,
                signedPrekeyStore,
                oneTimePrekeyStore
        );

        ctx.contactProvisioningService = new ContactProvisioningService(
                ctx.contactsApi,
                contactStore
        );

        ctx.sessionProvisioningService = new SessionProvisioningService(
                ctx.bundlesApi,
                ctx.localIdentity,
                contactStore,
                sessionStore
        );

        ctx.messageEncryptionService = new MessageEncryptionService(
                ctx.localIdentity,
                contactStore,
                sessionStore
        );

        ctx.wsService.connectThenWaitAuth()
                .thenCompose(v -> {
                    ctx.jwt = ctx.wsService.deviceToken();
                    return ctx.bundleProvisioningService.ensureBundleUploadedAsync(ctx.jwt);
                })
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
                    Platform.runLater(() -> System.err.println(
                            "Post-login startup failed: " + rootCauseMessage(err)
                    ));
                    return null;
                });
    }

    private String rootCauseMessage(Throwable err) {
        Throwable current = err;

        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }

        return current == null ? "unknown error" : current.getMessage();
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
