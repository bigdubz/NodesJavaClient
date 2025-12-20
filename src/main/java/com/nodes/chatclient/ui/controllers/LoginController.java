package com.nodes.chatclient.ui.controllers;

import com.nodes.chatclient.AppContext;
import com.nodes.chatclient.ui.vm.LoginViewModel;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public final class LoginController {
    private final Parent root;
    private final LoginViewModel vm;

    public LoginController(AppContext ctx, Consumer<String> onLoginSuccess) {
        this.vm = new LoginViewModel(ctx);

        Label usernameLabel = new Label("Username");
        TextField username = new TextField();

        Label passwordLabel = new Label("Password");
        PasswordField password = new PasswordField();

        Button loginBtn = new Button("Login");

        Label error = new Label();

        ProgressIndicator loading = new ProgressIndicator();
        loading.setPrefSize(16, 16);

        username.textProperty().bindBidirectional(vm.usernameProperty());
        password.textProperty().bindBidirectional(vm.passwordProperty());

        loginBtn.disableProperty().bind(vm.loginInProgressProperty());

        loginBtn.setOnAction(e -> vm.loginAsync().thenAccept(res -> {
            if (res != null) {
                ctx.userId = res.userId;
                ctx.jwt = res.token;

                Platform.runLater(() -> onLoginSuccess.accept(res.userId));
            }
        }));


        usernameLabel.getStyleClass().add("label");
        username.getStyleClass().add("input-field");
        passwordLabel.getStyleClass().add("label");
        password.getStyleClass().add("input-field");
        loginBtn.getStyleClass().add("button");
        error.getStyleClass().add("error");
        loading.getStyleClass().add("login-loading");

        username.disableProperty().bind(vm.loginInProgressProperty());
        password.disableProperty().bind(vm.loginInProgressProperty());

        error.textProperty().bind(vm.errorMessageProperty());
        error.visibleProperty().bind(vm.errorMessageProperty().isNotNull());

        loading.visibleProperty().bind(vm.loginInProgressProperty());
        VBox usernameBox = new VBox(6, usernameLabel, username);
        VBox passwordBox = new VBox(6, passwordLabel, password);

        VBox box = new VBox(14, usernameBox, passwordBox, loginBtn, error, loading);
        box.setPadding(new Insets(32));
        box.getStyleClass().add("login-root");

        this.root = box;
    }

    public Parent getRoot() {
        return root;
    }

    public LoginViewModel getViewModel() {
        return vm;
    }
}
