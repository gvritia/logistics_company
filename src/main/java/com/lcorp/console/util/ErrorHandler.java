package com.lcorp.console.util;

import com.lcorp.console.exception.LogisticsException;

public final class ErrorHandler {

    private ErrorHandler() {
    }

    public static boolean run(Runnable action) {
        try {
            action.run();
        } catch (InputCancelledException exception) {
            ConsoleWriter.printInfo("Операция отменена");
            return false;
        } catch (LogisticsException | IllegalArgumentException | IllegalStateException exception) {
            ConsoleWriter.printError(exception.getMessage());
        }
        return true;
    }
}
