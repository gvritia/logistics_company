package com.lcorp.javafx;

import com.lcorp.console.config.DatabaseConfig;
import com.lcorp.console.config.DatabaseConnectionFactory;
import com.lcorp.console.repository.jdbc.JdbcTransportationRequestQueryRepository;
import com.lcorp.console.service.TransportationRequestQueryService;
import com.lcorp.javafx.controller.MainController;
import com.lcorp.javafx.repository.JdbcRequestCreateDao;
import com.lcorp.javafx.repository.JdbcRequestDeleteDao;
import com.lcorp.javafx.repository.JdbcRequestUpdateDao;
import com.lcorp.javafx.service.RequestCreationService;
import com.lcorp.javafx.service.RequestDeletionService;
import com.lcorp.javafx.service.RequestEditingService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public final class LogisticsApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        Scene scene = new Scene(loader.load(), 1000, 560);
        stage.setTitle("Логистическая компания");
        stage.setScene(scene);
        stage.show();

        MainController controller = loader.getController();
        try {
            var connectionFactory = new DatabaseConnectionFactory(DatabaseConfig.load());
            var repository = new JdbcTransportationRequestQueryRepository(connectionFactory);
            controller.configure(
                new TransportationRequestQueryService(repository),
                new RequestCreationService(new JdbcRequestCreateDao(connectionFactory)),
                new RequestEditingService(new JdbcRequestUpdateDao(connectionFactory)),
                new RequestDeletionService(new JdbcRequestDeleteDao(connectionFactory))
            );
        } catch (IllegalStateException | NumberFormatException exception) {
            controller.showLoadError();
        }
    }
}
