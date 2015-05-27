package ru.terra.tproxy.service;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

import org.acra.ACRA;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import java.io.IOException;
import java.io.InputStream;

import ru.terra.tproxy.MainActivity;
import ru.terra.tproxy.R;

/**
 * Date: 02.04.15
 * Time: 17:16
 */
public class ProxyService extends IntentService {
    public static final String PROXY_RECEIVER = "ru.terra.tproxy.service.ProxyService.receiver";
    private static final String TAG = ProxyService.class.getName();
    private volatile boolean run;
    private Session session;
    private LocalBroadcastManager lbm;
    private Channel chan;
    private HttpProxyServer proxy;
    private boolean forward = true;
    private SharedPreferences preferences;

    public ProxyService() {
        super("Proxy intent service");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        lbm = LocalBroadcastManager.getInstance(this);
        run = true;
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        startProxy();

        SharedPreferences.OnSharedPreferenceChangeListener prefsListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                lbm.sendBroadcast(new Intent(ProxyService.PROXY_RECEIVER).putExtra("restart", true));
            }
        };

        preferences.registerOnSharedPreferenceChangeListener(prefsListener);

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (run) {
                    if (session != null)
                        if (!session.isConnected()) {
                            try {
                                fullStop();
                            } catch (Exception e) {
                                Log.e(TAG, "Unable to full stop", e);
                            }
                            try {
                                Thread.sleep(10000);
                                connectSSH(forward);
                                startProxy();
                            } catch (Exception e) {
                                Log.e(TAG, "Unable to connect ssh", e);
                            }
                        }
                }
            }
        }).start();

        try {
            connectSSH(forward);
        } catch (Exception e) {
            ACRA.getErrorReporter().handleSilentException(e);
        }

        lbm.sendBroadcast(new Intent(MainActivity.STATUS_RECEIVER).putExtra("text", "Started"));
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getBooleanExtra("stop", false)) {
                    fullStop();
                }
//                else if (intent.getBooleanExtra("forward", false)) {
//                    chan.disconnect();
//                    session.disconnect();
//
//                    new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            try {
//                                Thread.sleep(3000);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                            lbm.sendBroadcast(new Intent(MainActivity.STATUS_RECEIVER).putExtra("text", "Started as proxy"));
//                            startProxy();
//                        }
//                    }).start();
//                }
                else if (intent.getBooleanExtra("restart", false)) {
//                    forward = false;
                    fullStop();
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
                .withPort(Integer.parseInt(preferences.getString(getString(R.string.port_key), "55555")))
                .withAllowLocalOnly(false)
                .withListenOnAllAddresses(true)
                .withTransparent(true)
//                .plusActivityTracker(new ActivityTracker() {
//                    @Override
//                    public void clientConnected(InetSocketAddress clientAddress) {
//
//                    }
//
//                    @Override
//                    public void clientSSLHandshakeSucceeded(InetSocketAddress clientAddress, SSLSession sslSession) {
//
//                    }
//
//                    @Override
//                    public void clientDisconnected(InetSocketAddress clientAddress, SSLSession sslSession) {
//
//                    }
//
//                    @Override
//                    public void bytesReceivedFromClient(FlowContext flowContext, int numberOfBytes) {
//
//                    }
//
//                    @Override
//                    public void requestReceivedFromClient(FlowContext flowContext, HttpRequest httpRequest) {
//
//                    }
//
//                    @Override
//                    public void bytesSentToServer(FullFlowContext flowContext, int numberOfBytes) {
//
//                    }
//
//                    @Override
//                    public void requestSentToServer(FullFlowContext flowContext, HttpRequest httpRequest) {
//
//                    }
//
//                    @Override
//                    public void bytesReceivedFromServer(FullFlowContext flowContext, int numberOfBytes) {
//
//                    }
//
//                    @Override
//                    public void responseReceivedFromServer(FullFlowContext flowContext, HttpResponse httpResponse) {
////                        Updater.getUpdater().update(flowContext.getServerHostAndPort(), 0l, "");
//                    }
//
//                    @Override
//                    public void bytesSentToClient(FlowContext flowContext, int numberOfBytes) {
//
//                    }
//
//                    @Override
//                    public void responseSentToClient(FlowContext flowContext, HttpResponse httpResponse) {
//
//                    }
//                })
                .start();
    }

    private void fullStop() {
        if (proxy != null)
            proxy.stop();
        if (chan != null)
            chan.disconnect();
        if (session != null)
            session.disconnect();
        lbm.sendBroadcast(new Intent(MainActivity.STATUS_RECEIVER).putExtra("text", "Stopped"));
//        run = false;
    }

    class localUserInfo implements UserInfo {
        String passwd;

        localUserInfo(String passwd) {
            this.passwd = passwd;
        }

        public String getPassword() {
            return passwd;
        }

        public boolean promptYesNo(String str) {
            return true;
        }

        public String getPassphrase() {
            return passwd;
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

    private void connectSSH(boolean f) throws InterruptedException, JSchException, IOException {

        String host = preferences.getString(getString(R.string.desktop_addr), "192.168.1.4");
        String user = preferences.getString(getString(R.string.login_key), "");
        String password = preferences.getString(getString(R.string.pass_key), "");
        int port = Integer.parseInt(preferences.getString(getString(R.string.desktop_port_key), "23"));

        int tunnelLocalPort = Integer.parseInt(preferences.getString(getString(R.string.port_key), "55555"));
        String tunnelRemoteHost = "127.0.0.1";
        int tunnelRemotePort = Integer.parseInt(preferences.getString(getString(R.string.port_key), "55555"));

        JSch jsch = new JSch();
        session = jsch.getSession(user, host, port);
        session.setPassword(password);
        localUserInfo lui = new localUserInfo(password);
        session.setUserInfo(lui);

        session.connect();

        if (session != null) {
            if (f)
                session.setPortForwardingR("0.0.0.0", tunnelLocalPort, tunnelRemoteHost, tunnelRemotePort);
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
                                String res = new String(tmp, 0, i);
                                Log.i(TAG, "Received " + res);
                                if ("forward".equalsIgnoreCase(res)) {
                                    forward = true;
                                    lbm.sendBroadcast(new Intent(ProxyService.PROXY_RECEIVER).putExtra("forward", true));
                                } else if ("restart".equalsIgnoreCase(res)) {
                                    forward = false;
                                    lbm.sendBroadcast(new Intent(ProxyService.PROXY_RECEIVER).putExtra("restart", true));
                                }
                                break;
                            }
                            if (chan.isClosed()) {
                                if (in.available() > 0) continue;
                                Log.i(TAG, "exit-status: " + chan.getExitStatus());
                                System.out.println("exit-status: " + chan.getExitStatus());
//                                    fullStop();
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
