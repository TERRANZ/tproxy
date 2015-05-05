package ru.terra.tproxy.gui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
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
import java.util.ResourceBundle;

/**
 * Date: 09.04.15
 * Time: 11:05
 */
public class MainController implements Initializable {

    @FXML
    public Label lblStatus;
    @FXML
    public ListView<String> lvLog;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        GuiNotifier.mainController = this;

    }
}
