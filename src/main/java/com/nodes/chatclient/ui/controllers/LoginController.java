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

        TextField username = new TextField();
        PasswordField password = new PasswordField();
        Button loginBtn = new Button("Login");
        Label error = new Label();
        ProgressIndicator loading = new ProgressIndicator();
        loading.setVisible(false);
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

        VBox box = new VBox(10, username, password, loginBtn, error, loading);
        box.setPadding(new Insets(20));

        this.root = box;
    }

    public Parent getRoot() {
        return root;
    }

    public LoginViewModel getViewModel() {
        return vm;
    }
}
