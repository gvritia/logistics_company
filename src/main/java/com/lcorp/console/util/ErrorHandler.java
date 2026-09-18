package com.lcorp.console.util;

import com.lcorp.console.exception.LogisticsException;

public final class ErrorHandler {

    private ErrorHandler() {
    }

    public static void run(Runnable action) {
        try {
            action.run();
        } catch (LogisticsException | IllegalArgumentException | IllegalStateException exception) {
            ConsoleWriter.printError(exception.getMessage());
        }
    }
}
