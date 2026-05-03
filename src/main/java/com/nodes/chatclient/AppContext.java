package com.nodes.chatclient;

import com.nodes.chatclient.config.ClientConfig;
import com.nodes.chatclient.e2ee.identity.LocalIdentityService;
import com.nodes.chatclient.e2ee.types.LocalIdentity;
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
    public final LocalIdentityService localIdentityService;
    public final WsService wsService;
    public final WsMessageRouter router;

    public String userId;
    public String deviceId;
    public String jwt;
    public LocalIdentity localIdentity;

    public AppContext(
            ClientConfig config,
            HttpClientFactory httpFactory,
            AuthApi authApi,
            ChatApi chatApi,
            LocalIdentityService localIdentityService,
            WsService wsService,
            WsMessageRouter router
    ) {
        this.config = config;
        this.httpFactory = httpFactory;
        this.authApi = authApi;
        this.chatApi = chatApi;
        this.localIdentityService = localIdentityService;
        this.wsService = wsService;
        this.router = router;
    }
}
