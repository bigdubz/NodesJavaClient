package com.nodes.chatclient.ui.fx;

import javafx.application.Platform;

public final class FxDispatcher {

    private FxDispatcher() {}

    public static void run(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }
}
