package ru.terra.tproxy.gui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.ForwardingFilter;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.Session;
import org.apache.sshd.common.SshdSocketAddress;
import org.apache.sshd.common.io.IoSession;
import org.apache.sshd.common.session.AbstractSession;
import org.apache.sshd.server.UserAuth;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.SessionFactory;
import org.slf4j.LoggerFactory;
import ru.terra.tproxy.gui.sshd.MyPasswordAuthenticator;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Date: 09.04.15
 * Time: 11:05
 */
public class MainController implements Initializable {

    @FXML
    public Label lblStatus;
    @FXML
    public ListView<String> lvLog;
    @FXML
    public ListView<SessionItem> lvConnected;
    private SshServer sshd;
    private SSHService service;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        GuiNotifier.mainController = this;
    }

    public void start(ActionEvent actionEvent) {
        service = new SSHService();
        service.start();
        ContextMenu menu = new ContextMenu();
        MenuItem connect = new MenuItem("Подключить");
        connect.setOnAction((e) -> {
            SessionItem item = lvConnected.getSelectionModel().getSelectedItem();
            if (item != null) {
            }
        });
        menu.getItems().add(connect);
        lvConnected.setContextMenu(menu);
    }

    public void stop(ActionEvent actionEvent) {
        if (service != null && service.isRunning())
            service.reset();
        if (sshd != null)
            try {
                sshd.stop();
            } catch (InterruptedException e) {
                LoggerFactory.getLogger(MainController.class).error("Error while starting sshd", e);
            }
    }

    private class SSHService extends Service<Void> {
        @Override
        protected Task<Void> createTask() {
            return new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    sshd = SshServer.setUpDefaultServer();
                    sshd.setPort(44444);
                    List<NamedFactory<UserAuth>> userAuthFactories = new ArrayList<>();
                    sshd.setPasswordAuthenticator(new MyPasswordAuthenticator());
                    sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("hostkey.ser"));
                    sshd.setTcpipForwardingFilter(new ForwardingFilter() {
                        @Override
                        public boolean canForwardAgent(Session session) {
                            return true;
                        }

                        @Override
                        public boolean canForwardX11(Session session) {
                            return true;
                        }

                        @Override
                        public boolean canListen(SshdSocketAddress address, Session session) {
                            return true;
                        }

                        @Override
                        public boolean canConnect(SshdSocketAddress address, Session session) {
                            return true;
                        }
                    });
                    sshd.setSessionFactory(new TProxySessionFactory());
                    try {
                        sshd.start();
                    } catch (IOException e) {
                        LoggerFactory.getLogger(MainController.class).error("Error while starting sshd", e);
                    }

                    return null;
                }
            };
        }
    }

    private class TProxySessionFactory extends SessionFactory {
        @Override
        public void sessionCreated(IoSession ioSession) throws Exception {
            super.sessionCreated(ioSession);
            updateConnected();
        }

        @Override
        public void sessionClosed(IoSession ioSession) throws Exception {
            super.sessionClosed(ioSession);
            updateConnected();
        }

        @Override
        public void messageReceived(IoSession ioSession, org.apache.sshd.common.util.Readable message) throws Exception {
            super.messageReceived(ioSession, message);
        }
    }

    private void updateConnected() {
        Platform.runLater(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            lvConnected.getItems().clear();
            lvConnected.setItems(FXCollections.observableArrayList(sshd.getActiveSessions().stream().map(s -> new SessionItem(s)).collect(Collectors.toList())));
        });

    }

    private class SessionItem {
        private AbstractSession s;

        public SessionItem(AbstractSession s) {
            this.s = s;
        }

        @Override
        public String toString() {
            return s.getIoSession().getRemoteAddress().toString();
        }
    }
}
