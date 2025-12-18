package com.nodes.chatclient;

import com.nodes.chatclient.config.ClientConfig;
import com.nodes.chatclient.http.AuthApi;
import com.nodes.chatclient.http.ChatApi;
import com.nodes.chatclient.http.HttpClientFactory;
import com.nodes.chatclient.ws.WsMessageRouter;
import com.nodes.chatclient.ws.WsService;

public final class AppContext {
    public final ClientConfig config;
    public final HttpClientFactory httpFactory;
    public final AuthApi authApi;
    public final ChatApi chatApi;
    public final WsService wsService;
    public final WsMessageRouter router;

    public String userId;
    public String jwt;

    public AppContext(
            ClientConfig config,
            HttpClientFactory httpFactory,
            AuthApi authApi,
            ChatApi chatApi,
            WsService wsService,
            WsMessageRouter router
    ) {
        this.config = config;
        this.httpFactory = httpFactory;
        this.authApi = authApi;
        this.chatApi = chatApi;
        this.wsService = wsService;
        this.router = router;
    }
}
