package com.nodes.chatclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nodes.chatclient.config.ClientConfig;
import com.nodes.chatclient.e2ee.db.DatabaseManager;
import com.nodes.chatclient.e2ee.identity.LocalIdentityService;
import com.nodes.chatclient.http.api.LoginApi;
import com.nodes.chatclient.http.api.BundlesApi;
import com.nodes.chatclient.http.api.ChatApi;
import com.nodes.chatclient.http.HttpClientFactory;
import com.nodes.chatclient.ui.controllers.AppRootController;
import com.nodes.chatclient.ws.WsMessageRouter;
import com.nodes.chatclient.ws.WsService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.http.HttpClient;
import java.util.Objects;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        DatabaseManager.init();
//        System.setProperty("jdk.httpclient.HttpClient.log", "all");
        System.setProperty("java.net.preferIPv6Addresses", "true");

        ClientConfig config = ClientConfig.localDev();
        AppContext ctx = getAppContext(config);

        AppRootController app = new AppRootController(ctx, stage);

        int DEFAULT_SIZE_H = 1000;
        int DEFAULT_SIZE_W = 1200;
        Scene scene = new Scene(app.getRoot(), DEFAULT_SIZE_W, DEFAULT_SIZE_H);
        scene.getStylesheets().addAll(
                Objects.requireNonNull(getClass().getResource("/styles/global.css")).toExternalForm(),
                Objects.requireNonNull(getClass().getResource("/styles/login.css")).toExternalForm(),
                Objects.requireNonNull(getClass().getResource("/styles/chat.css")).toExternalForm(),
                Objects.requireNonNull(getClass().getResource("/styles/conversations.css")).toExternalForm()
        );

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

        LoginApi loginApi = new LoginApi(config, httpClient, mapper);
        ChatApi chatApi = new ChatApi(config, httpClient, mapper);
        BundlesApi bundlesApi = new BundlesApi(config, httpClient, mapper);
        LocalIdentityService localIdentityService = LocalIdentityService.from(DatabaseManager.get());

        WsMessageRouter router = new WsMessageRouter(mapper);
        WsService wsService = new WsService(config, httpClient, mapper, router);

        return new AppContext(
                config,
                httpFactory,
                loginApi,
                chatApi,
                bundlesApi,
                localIdentityService,
                wsService,
                router
        );
    }
}
