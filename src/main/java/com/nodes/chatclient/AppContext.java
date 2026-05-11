package com.nodes.chatclient;

import com.nodes.chatclient.config.ClientConfig;
import com.nodes.chatclient.e2ee.identity.LocalIdentityService;
import com.nodes.chatclient.e2ee.types.LocalIdentity;
import com.nodes.chatclient.e2ee.utils.BundleProvisioningService;
import com.nodes.chatclient.e2ee.utils.ContactProvisioningService;
import com.nodes.chatclient.http.api.ContactsApi;
import com.nodes.chatclient.http.api.LoginApi;
import com.nodes.chatclient.http.api.BundlesApi;
import com.nodes.chatclient.http.HttpClientFactory;
import com.nodes.chatclient.ws.WsMessageRouter;
import com.nodes.chatclient.ws.WsService;

public final class AppContext {
    public final ClientConfig config;
    public final HttpClientFactory httpFactory;
    public final LoginApi loginApi;
    public final BundlesApi bundlesApi;
    public final ContactsApi contactsApi;
    public final LocalIdentityService localIdentityService;
    public final WsService wsService;
    public final WsMessageRouter router;

    public String userId;
    public String deviceId;
    public String jwt;
    public LocalIdentity localIdentity;
    public BundleProvisioningService bundleProvisioningService;
    public ContactProvisioningService contactProvisioningService;

    public AppContext(
            ClientConfig config,
            HttpClientFactory httpFactory,
            LoginApi loginApi,
            BundlesApi bundlesApi,
            ContactsApi contactsApi,
            LocalIdentityService localIdentityService,
            WsService wsService,
            WsMessageRouter router
    ) {
        this.config = config;
        this.httpFactory = httpFactory;
        this.loginApi = loginApi;
        this.bundlesApi = bundlesApi;
        this.contactsApi = contactsApi;
        this.localIdentityService = localIdentityService;
        this.wsService = wsService;
        this.router = router;
    }
}
