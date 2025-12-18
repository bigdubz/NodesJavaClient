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
        ObjectMapper mapper = new ObjectMapper();

        HttpClientFactory httpFactory = new HttpClientFactory(config);
        HttpClient httpClient = httpFactory.create();

        AuthApi authApi = new AuthApi(config, httpClient, mapper);
        ChatApi chatApi = new ChatApi(config, httpClient, mapper);

        ChatStore store = null;
        WsMessageRouter router = new WsMessageRouter(mapper);
        WsService wsService = new WsService(config, httpClient, mapper, router);

        AppContext ctx = new AppContext(
                config,
                httpFactory,
                authApi,
                chatApi,
                wsService,
                router
        );

        LoginController login = new LoginController(ctx, (userId) -> showConversationScene(stage, ctx, userId));

        Scene scene = new Scene(login.getRoot());

        stage.setScene(scene);
        stage.setTitle("Nodes");
        stage.show();
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

        Platform.runLater(() -> {
            Scene scene = new Scene(controller.getRoot(), 500, 700);
            stage.setScene(scene);
            stage.setTitle("Chat: " + peerId);
        });
    }
}
