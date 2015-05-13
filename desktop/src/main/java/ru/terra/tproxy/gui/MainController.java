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
import org.apache.sshd.common.*;
import org.apache.sshd.common.io.IoSession;
import org.apache.sshd.common.session.AbstractSession;
import org.apache.sshd.server.*;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.session.SessionFactory;
import org.slf4j.LoggerFactory;
import ru.terra.tproxy.gui.sshd.MyPasswordAuthenticator;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.*;
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
    private Map<IoSession, MyFilterOutputStream> sessionStreams = new HashMap<>();


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        GuiNotifier.mainController = this;
    }

    public void start(ActionEvent actionEvent) {
        service = new SSHService();
        service.start();
        addToLog("Service started");
        ContextMenu menu = new ContextMenu();
        MenuItem connect = new MenuItem("Connect this device");
        connect.setOnAction((e) -> {
            SessionItem item = lvConnected.getSelectionModel().getSelectedItem();
            if (item != null) {
                new Thread(() -> {
                    sessionStreams.forEach((s, a) -> {
                        if (item.s.getIoSession().equals(s)) {
                            if (a != null)
                                try {
                                    a.write("forward".getBytes());
                                    a.flush();
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                }
                        } else {
                            if (a != null)
                                try {
                                    a.write("restart".getBytes());
                                    a.flush();
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                }
                        }
                    });

                }).start();

            }
        });
        menu.getItems().add(connect);
        lvConnected.setContextMenu(menu);
    }

    public void stop(ActionEvent actionEvent) {
        addToLog("Service stop");
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
                    sshd.setShellFactory(new MyShellFactory());
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
            addToLog("Session created");
        }

        @Override
        public void sessionClosed(IoSession ioSession) throws Exception {
            super.sessionClosed(ioSession);
            updateConnected();
            addToLog("Session closed");
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
            if (sshd != null && sshd.getActiveSessions() != null && sshd.getActiveSessions().size() > 0)
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

    public class MyShellFactory implements Factory<Command> {
        @Override
        public Command create() {
            return new MyShellCommand();
        }
    }


    public class MyShellCommand implements Command, SessionAware {
        private InputStream in;
        private OutputStream out;
        private OutputStream err;
        private ServerSession session;

        @Override
        public void setInputStream(InputStream in) {
            this.in = in;
        }

        @Override
        public void setOutputStream(OutputStream out) {
            this.out = out;
        }

        @Override
        public void setErrorStream(OutputStream err) {
            this.err = err;
        }

        @Override
        public void setExitCallback(ExitCallback callback) {
        }

        @Override
        public void start(Environment env) throws IOException {
            MyFilterOutputStream fout = new MyFilterOutputStream(out);
            sessionStreams.put(session.getIoSession(), fout);
        }

        @Override
        public void destroy() {
            sessionStreams.remove(session.getIoSession());
        }

        @Override
        public void setSession(ServerSession session) {
            this.session = session;
        }
    }

    private class MyFilterOutputStream extends FilterOutputStream {
        public MyFilterOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void write(int c) throws IOException {
            if (c == '\n' || c == '\r') {
                super.write('\r');
                super.write('\n');
                return;
            }
            super.write(c);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            for (int i = off; i < len; i++) {
                write(b[i]);
            }
        }
    }

    private void addToLog(String msg) {
        Platform.runLater(() -> lvLog.getItems().add(msg));
    }
}
