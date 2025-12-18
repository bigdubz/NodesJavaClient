package com.nodes.chatclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nodes.chatclient.config.ClientConfig;
import com.nodes.chatclient.http.AuthApi;
import com.nodes.chatclient.http.ChatApi;
import com.nodes.chatclient.http.HttpClientFactory;
import com.nodes.chatclient.store.ChatStore;
import com.nodes.chatclient.ui.controllers.ChatController;
import com.nodes.chatclient.ui.controllers.ConversationsController;
import com.nodes.chatclient.ui.controllers.LoginController;
import com.nodes.chatclient.ui.vm.ChatViewModel;
import com.nodes.chatclient.ui.vm.ConversationsViewModel;
import com.nodes.chatclient.ws.WsMessageRouter;
import com.nodes.chatclient.ws.WsService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.http.HttpClient;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        ClientConfig config = ClientConfig.localDev();
        AppContext ctx = getAppContext(config);

        LoginController login = new LoginController(ctx, (userId) -> showConversationScene(stage, ctx, userId));

        Scene scene = new Scene(login.getRoot());

        stage.setScene(scene);
        stage.setTitle("Nodes");
        stage.show();
        stage.setOnCloseRequest(event -> {
            System.out.println("Closing application...");
            ctx.wsService.disconnect();
            Platform.exit();
            System.exit(0);
        });
    }

    private static AppContext getAppContext(ClientConfig config) {
        ObjectMapper mapper = new ObjectMapper();

        HttpClientFactory httpFactory = new HttpClientFactory(config);
        HttpClient httpClient = httpFactory.create();

        AuthApi authApi = new AuthApi(config, httpClient, mapper);
        ChatApi chatApi = new ChatApi(config, httpClient, mapper);

        WsMessageRouter router = new WsMessageRouter(mapper);
        WsService wsService = new WsService(config, httpClient, mapper, router);

        return new AppContext(
                config,
                httpFactory,
                authApi,
                chatApi,
                wsService,
                router
        );
    }

    private void showConversationScene(Stage stage, AppContext ctx, String userId) {
        ChatStore store = new ChatStore(userId);
        ctx.router.registerServerHandlers(store);
        ctx.wsService.setAuth(userId, ctx.jwt);
        ctx.wsService.connect();

        ctx.chatApi.getConversationsAsync(ctx.jwt)
                .thenAccept(store::mergeConversations);

        ConversationsViewModel cvm = new ConversationsViewModel(store);
        ConversationsController cc = new ConversationsController(
                cvm,
                peerId -> showChatScene(stage, ctx, store, peerId)
        );

        Platform.runLater(() ->
                stage.setScene(
                        new Scene(cc.getRoot(), 500, 700)
                )
        );
    }

    private void showChatScene(
            Stage stage,
            AppContext ctx,
            ChatStore store,
            String peerId
    ) {
        ChatViewModel vm = new ChatViewModel(store, peerId);
        ChatController controller = new ChatController(ctx, vm);
        long cursor = store.getConversation(peerId)
                .map(c -> c.lastTimestamp)
                .orElse(Long.MAX_VALUE);

        ctx.chatApi.getHistoryAsync(ctx.jwt, peerId, cursor, 50)
                .thenAccept(rows -> store.mergeHistory(peerId, rows));

        Platform.runLater(() -> {
            Scene scene = new Scene(controller.getRoot(), 500, 700);
            stage.setScene(scene);
            stage.setTitle("Chat: " + peerId);
        });
    }
}
