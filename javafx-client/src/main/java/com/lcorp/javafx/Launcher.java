package com.lcorp.javafx;

import javafx.application.Application;

public final class Launcher {
    private Launcher() {
    }

    // Обычная точка входа запускает JavaFX-приложение через Application.
    public static void main(String[] args) {
        Application.launch(Main.class, args);
    }
}
