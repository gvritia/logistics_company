package com.lcorp.console;

import com.lcorp.console.app.ApplicationContext;
import com.lcorp.console.app.ConsoleApp;
import com.lcorp.console.config.DatabaseConfig;
import com.lcorp.console.exception.DataAccessException;
import com.lcorp.console.util.ConsoleReader;
import com.lcorp.console.util.InputClosedException;
import jakarta.persistence.PersistenceException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.logging.LogManager;

public class Main {

    public static void main(String[] args) {
        configureLogging();

        try (ApplicationContext context = new ApplicationContext(DatabaseConfig.load());
             Scanner scanner = new Scanner(System.in)) {
            new ConsoleApp(context, new ConsoleReader(scanner)).run();
        } catch (InputClosedException exception) {
            System.out.println("Ввод завершён, работа прекращена");
        } catch (IllegalStateException | SQLException | PersistenceException
                 | DataAccessException exception) {
            System.err.println("Ошибка работы с базой данных: " + exception.getMessage());
            System.exit(1);
        }
    }

    private static void configureLogging() {
        try (InputStream settings =
                 Main.class.getResourceAsStream("/logging.properties")) {
            if (settings != null) {
                LogManager.getLogManager().readConfiguration(settings);
            }
        } catch (IOException exception) {
        }
    }
}
