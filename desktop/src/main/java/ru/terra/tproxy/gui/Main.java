package ru.terra.tproxy.gui;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.log4j.BasicConfigurator;

import java.io.IOException;
import java.net.URL;

/**
 * Date: 18.07.14
 * Time: 10:26
 */
public class Main extends Application {
    public static void main(String... args) {
        BasicConfigurator.configure();
        launch(args);


    }

    @Override
    public void start(Stage stage) throws Exception {
        String fxmlFile = "/fxml/w_main.fxml";
        URL location = this.getClass().getResource(fxmlFile);
        Parent root = null;
        FXMLLoader fxmlLoader = new FXMLLoader();
        try {
            root = fxmlLoader.load(location.openStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        stage.setScene(new Scene(root));
        stage.setTitle("TProxy desktop");
        stage.show();
        stage.setOnHidden(event -> System.exit(0));
    }
}
