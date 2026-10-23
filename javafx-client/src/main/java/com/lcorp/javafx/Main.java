package com.lcorp.javafx;

import com.lcorp.javafx.controller.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public final class Main extends Application {
    // @Override: JavaFX вызывает переопределённый start после запуска приложения.
    @Override
    public void start(Stage stage) {
        try {
            DesktopContext context = new DesktopContext();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Scene scene = new Scene(loader.load(), 1180, 740);
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            MainController controller = loader.getController();
            controller.setContext(context);
            stage.setTitle("Логистическая компания — заявки (КР2)");
            stage.setMinWidth(900);
            stage.setMinHeight(580);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка запуска");
            alert.setHeaderText("Не удалось открыть приложение");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}
