package com.lcorp.javafx;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MiniApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        TextField idField = new TextField();
        idField.setPromptText("Driver ID");
        Label result = new Label("Ready");

        Button deactivate = new Button("Deactivate");
        deactivate.setOnAction(event -> {
            try {
                long id = Long.parseLong(idField.getText().trim());
                if (id <= 0) {
                    result.setText("Invalid ID");
                    return;
                }
                result.setText(id == 1 ? "Deactivated" : "Driver not found");
            } catch (NumberFormatException e) {
                result.setText("Invalid ID");
            }
        });

        Button clear = new Button("Clear");
        clear.setOnAction(event -> {
            idField.clear();
            result.setText("Ready");
            idField.requestFocus();
        });

        VBox root = new VBox(10, idField, deactivate, clear, result);
        root.setPadding(new Insets(15));
        stage.setTitle("Driver demo");
        stage.setScene(new Scene(root, 320, 200));
        stage.show();
    }
}
