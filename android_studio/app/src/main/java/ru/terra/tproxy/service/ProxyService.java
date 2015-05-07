package ru.terra.tproxy.service;

import android.app.IntentService;
import android.content.*;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

import org.acra.ACRA;
import org.littleshoot.proxy.ActivityTracker;
import org.littleshoot.proxy.FlowContext;
import org.littleshoot.proxy.FullFlowContext;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import ru.terra.tproxy.MainActivity;
import ru.terra.tproxy.R;
import ru.terra.tproxy.proxy.Updater;

import javax.net.ssl.SSLSession;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringBufferInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.InetSocketAddress;

/**
 * Date: 02.04.15
 * Time: 17:16
 */
public class ProxyService extends IntentService {
    public static final String PROXY_RECEIVER = "ru.terra.tproxy.service.ProxyService.receiver";
    private volatile boolean run;
    private Session session;
    private LocalBroadcastManager lbm;
    private Channel chan;
    private HttpProxyServer proxy;

    public ProxyService() {
        super("Proxy intent service");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        lbm = LocalBroadcastManager.getInstance(this);
        run = true;

//        startProxy();

        try {
            connectSSH(false);
        } catch (Exception e) {
            ACRA.getErrorReporter().handleException(e);
            Log.e("ProxyService", "Unable to start ssh connection", e);
        }

        lbm.sendBroadcast(new Intent(MainActivity.STATUS_RECEIVER).putExtra("text", "Started"));
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getBooleanExtra("stop", false)) {
                    fullStop();
                } else if (intent.getBooleanExtra("forward", false)) {
                    chan.disconnect();
                    session.disconnect();

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(3000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            lbm.sendBroadcast(new Intent(MainActivity.STATUS_RECEIVER).putExtra("text", "Started as proxy"));
                            startProxy();
                            try {
                                connectSSH(true);
                            } catch (Exception e) {
                                ACRA.getErrorReporter().handleException(e);
                                Log.e("ProxyService", "Unable to start ssh connection", e);
                            }
                        }
                    }).start();

                }
            }
        };

        IntentFilter statusFilter = new IntentFilter(PROXY_RECEIVER);
        statusFilter.addCategory(Intent.CATEGORY_DEFAULT);
        lbm.registerReceiver(receiver, statusFilter);

        while (run) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Log.e("ProxyService", "Error while sleep loop", e);
            }
        }
    }

    private void startProxy() {
        proxy = DefaultHttpProxyServer.bootstrap()
                .withPort(55555)
                .withAllowLocalOnly(false)
                .withListenOnAllAddresses(true)
                .withTransparent(true)
                .plusActivityTracker(new ActivityTracker() {
                    @Override
                    public void clientConnected(InetSocketAddress clientAddress) {

                    }

                    @Override
                    public void clientSSLHandshakeSucceeded(InetSocketAddress clientAddress, SSLSession sslSession) {

                    }

                    @Override
                    public void clientDisconnected(InetSocketAddress clientAddress, SSLSession sslSession) {

                    }

                    @Override
                    public void bytesReceivedFromClient(FlowContext flowContext, int numberOfBytes) {

                    }

                    @Override
                    public void requestReceivedFromClient(FlowContext flowContext, HttpRequest httpRequest) {

                    }

                    @Override
                    public void bytesSentToServer(FullFlowContext flowContext, int numberOfBytes) {

                    }

                    @Override
                    public void requestSentToServer(FullFlowContext flowContext, HttpRequest httpRequest) {

                    }

                    @Override
                    public void bytesReceivedFromServer(FullFlowContext flowContext, int numberOfBytes) {

                    }

                    @Override
                    public void responseReceivedFromServer(FullFlowContext flowContext, HttpResponse httpResponse) {
                        Updater.getUpdater().update(flowContext.getServerHostAndPort(), 0l, "");
                    }

                    @Override
                    public void bytesSentToClient(FlowContext flowContext, int numberOfBytes) {

                    }

                    @Override
                    public void responseSentToClient(FlowContext flowContext, HttpResponse httpResponse) {

                    }
                })
                .start();
    }

    private void fullStop() {
        proxy.stop();
        chan.disconnect();
        session.disconnect();
        lbm.sendBroadcast(new Intent(MainActivity.STATUS_RECEIVER).putExtra("text", "Stopped"));
        run = false;
    }

    class localUserInfo implements UserInfo {
        String passwd;

        public String getPassword() {
            return passwd;
        }

        public boolean promptYesNo(String str) {
            return true;
        }

        public String getPassphrase() {
            return null;
        }

        public boolean promptPassphrase(String message) {
            return true;
        }

        public boolean promptPassword(String message) {
            return true;
        }

        public void showMessage(String message) {
        }
    }

    private void connectSSH(boolean forward) throws JSchException, IOException {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String host = prefs.getString(getString(R.string.desktop_addr), "192.168.1.4");
        String user = "username";
        String password = "password";
        int port = 44444;

        int tunnelLocalPort = 55555;
        String tunnelRemoteHost = "127.0.0.1";
        int tunnelRemotePort = 55555;

        JSch jsch = new JSch();
        session = jsch.getSession(user, host, port);
        session.setPassword(password);
        localUserInfo lui = new localUserInfo();
        session.setUserInfo(lui);
        try {
            session.connect();
        } catch (JSchException e) {
            Updater.getUpdater().update("Unable to connect to desktop: " + e.getMessage(), 0l, "");
            ACRA.getErrorReporter().handleException(e);
            return;
        }
        if (session != null) {
            if (forward)
                session.setPortForwardingR(tunnelLocalPort, tunnelRemoteHost, tunnelRemotePort);
            chan = session.openChannel("shell");
            chan.connect();
            final InputStream in = chan.getInputStream();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        byte[] tmp = new byte[1024];
                        while (true) {
                            while (in.available() > 0) {
                                int i = in.read(tmp, 0, 1024);
                                if (i < 0) break;
                                Updater.getUpdater().update(new String(tmp, 0, i), 0l, "");
                                lbm.sendBroadcast(new Intent(ProxyService.PROXY_RECEIVER).putExtra("forward", true));
                                break;
                            }
                            if (chan.isClosed()) {
                                if (in.available() > 0) continue;
                                Updater.getUpdater().update("exit-status: " + chan.getExitStatus(), 0l, "");
                                System.out.println("exit-status: " + chan.getExitStatus());
                                fullStop();
                                break;
                            }
                            try {
                                Thread.sleep(1000);
                            } catch (Exception ee) {
                            }
                        }
                    } catch (Exception e) {
                        Log.e("ProxyService", e.getMessage(), e);
                    }
                }
            }).start();
        }
    }


}
