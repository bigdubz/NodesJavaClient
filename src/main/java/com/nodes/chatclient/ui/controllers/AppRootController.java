package com.nodes.chatclient.ui.controllers;

import com.nodes.chatclient.AppContext;
import com.nodes.chatclient.store.ChatStore;
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
        
        ctx.wsService.setAuth(userId, ctx.jwt);
        ctx.wsService.connect();

        ctx.chatApi.getConversationsAsync(ctx.jwt)
                .thenAccept(store::mergeConversations);

        mainController = new MainController(
                ctx,
                store,
                this::logout
        );

        root.getChildren().setAll(mainController.getRoot());
        stage.setTitle("Nodes");
    }

    private void logout() {
        ctx.wsService.disconnect();
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
