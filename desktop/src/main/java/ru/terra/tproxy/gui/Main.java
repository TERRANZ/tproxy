package ru.terra.tproxy.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.log4j.BasicConfigurator;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.ForwardingFilter;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.Session;
import org.apache.sshd.common.SshdSocketAddress;
import org.apache.sshd.server.UserAuth;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.slf4j.LoggerFactory;
import ru.terra.tproxy.gui.sshd.MyPasswordAuthenticator;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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
    }
}
