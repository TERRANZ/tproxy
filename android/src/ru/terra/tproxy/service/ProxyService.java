package ru.terra.tproxy.service;

import android.app.IntentService;
import android.content.*;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import org.littleshoot.proxy.ActivityTracker;
import org.littleshoot.proxy.FlowContext;
import org.littleshoot.proxy.FullFlowContext;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import ru.terra.tproxy.MainActivity;
import ru.terra.tproxy.R;
import ru.terra.tproxy.proxy.Updater;

import javax.net.ssl.SSLSession;
import java.net.InetSocketAddress;

/**
 * Date: 02.04.15
 * Time: 17:16
 */
public class ProxyService extends IntentService {
    public static final String PROXY_RECEIVER = "ru.terra.tproxy.service.ProxyService.receiver";
    private volatile boolean run;
    private Session session;

    public ProxyService() {
        super("Proxy intent service");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        run = true;

        final HttpProxyServer proxy = DefaultHttpProxyServer.bootstrap()
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

        try {
            connectSSH();
        } catch (JSchException e) {
            Log.e("ProxyService", "Unable to start ssh connection", e);
        }

        lbm.sendBroadcast(new Intent(MainActivity.STATUS_RECEIVER).putExtra("text", "Started"));
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getBooleanExtra("stop", false)) {
                    proxy.stop();
                    session.disconnect();
                    lbm.sendBroadcast(new Intent(MainActivity.STATUS_RECEIVER).putExtra("text", "Stopped"));
                    run = false;
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

    private void connectSSH() throws JSchException {
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
        session.connect();
        session.setPortForwardingR(tunnelLocalPort, tunnelRemoteHost, tunnelRemotePort);
    }
}
