package com.nodes.chatclient.ui.vm;

import com.nodes.chatclient.AppContext;
import com.nodes.chatclient.http.AuthApi;
import com.nodes.chatclient.http.dto.LoginResponse;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.concurrent.CompletableFuture;

public final class LoginViewModel {
    private final AppContext ctx;
    private final StringProperty username = new SimpleStringProperty();
    private final StringProperty password = new SimpleStringProperty();

    private final BooleanProperty loginInProgress = new SimpleBooleanProperty(false);
    private final StringProperty errorMessage = new SimpleStringProperty();

    public LoginViewModel(AppContext ctx) {
        this.ctx = ctx;
    }

    public StringProperty usernameProperty() {
        return username;
    }

    public StringProperty passwordProperty() {
        return password;
    }

    public BooleanProperty loginInProgressProperty() {
        return loginInProgress;
    }

    public StringProperty errorMessageProperty() {
        return errorMessage;
    }

    public CompletableFuture<LoginResponse> loginAsync() {
        loginInProgress.set(true);
        errorMessage.set(null);

        AuthApi authApi = ctx.authApi;

        return authApi
                .loginAsync(username.get(), password.get())
                .whenCompleteAsync((res, err) -> {
                    Platform.runLater(() -> loginInProgress.set(false));

                    if (err != null) {
                        Platform.runLater(() -> errorMessage.set(err.getMessage()));
                    }
                });
    }
}
