package com.nodes.chatclient.ui.vm;

import com.nodes.chatclient.AppContext;
import com.nodes.chatclient.http.api.LoginApi;
import com.nodes.chatclient.http.dto.LoginResponse;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

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
        if (loginInProgress.get()) {
            return CompletableFuture.completedFuture(null);
        }

        loginInProgress.set(true);
        errorMessage.set(null);

        LoginApi loginApi = ctx.loginApi;

        return loginApi
                .loginAsync(username.get(), password.get())
                .whenCompleteAsync((res, err) -> {
                    Platform.runLater(() -> loginInProgress.set(false));

                    if (err != null) {
                        String message = rootCauseMessage(err);
                        System.err.println("Login request failed: " + message);
                        Platform.runLater(() -> errorMessage.set(message));
                    }
                });
    }

    private String rootCauseMessage(Throwable err) {
        Throwable current = err;

        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }

        return current == null ? "unknown login error" : current.getMessage();
    }
}
