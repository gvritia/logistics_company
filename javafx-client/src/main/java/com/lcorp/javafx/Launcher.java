package com.lcorp.javafx;

import javafx.application.Application;

/** Entry point for IDE and classpath launches. */
public final class Launcher {
    private Launcher() {
    }

    public static void main(String[] args) {
        Application.launch(Main.class, args);
    }
}
