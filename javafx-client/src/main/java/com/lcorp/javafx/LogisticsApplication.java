package com.lcorp.javafx;

import com.lcorp.console.config.DatabaseConfig;
import com.lcorp.console.config.DatabaseConnectionFactory;
import com.lcorp.console.repository.jdbc.JdbcTransportationRequestQueryRepository;
import com.lcorp.console.service.TransportationRequestQueryService;
import com.lcorp.javafx.controller.MainController;
import com.lcorp.javafx.repository.JdbcRequestCreateDao;
import com.lcorp.javafx.service.RequestCreationService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public final class LogisticsApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        Scene scene = new Scene(loader.load(), 800, 500);
        stage.setTitle("Логистическая компания");
        stage.setScene(scene);
        stage.show();

        MainController controller = loader.getController();
        try {
            var connectionFactory = new DatabaseConnectionFactory(DatabaseConfig.load());
            var repository = new JdbcTransportationRequestQueryRepository(connectionFactory);
            controller.configure(
                new TransportationRequestQueryService(repository),
                new RequestCreationService(new JdbcRequestCreateDao(connectionFactory))
            );
        } catch (IllegalStateException | NumberFormatException exception) {
            controller.showLoadError();
        }
    }
}
