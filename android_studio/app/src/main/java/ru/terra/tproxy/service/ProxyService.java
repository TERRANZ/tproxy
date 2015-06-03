package ru.terra.tproxy.service;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

import org.acra.ACRA;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import java.io.IOException;
import java.util.Random;

import ru.terra.tproxy.Config;
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
    //    private Channel chan;
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
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
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
                } else if (intent.getBooleanExtra("restart", false)) {
                    fullStop();
                }

            }
        };

        IntentFilter statusFilter = new IntentFilter(PROXY_RECEIVER);
        statusFilter.addCategory(Intent.CATEGORY_DEFAULT);
        lbm.registerReceiver(receiver, statusFilter);

        PowerManager mgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock");
        wakeLock.acquire();

        while (run) {
            try {
                Thread.sleep(1);
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
                .start();
    }

    private void fullStop() {
        if (proxy != null)
            proxy.stop();
//        if (chan != null)
//            chan.disconnect();
        if (session != null)
            session.disconnect();
        lbm.sendBroadcast(new Intent(MainActivity.STATUS_RECEIVER).putExtra("text", "Stopped"));
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

        String host = /*preferences.getString(getString(R.string.desktop_addr), "192.168.1.4");*/ Config.IP;
        String user = /*preferences.getString(getString(R.string.login_key), "");*/ Config.LOGIN;
        String password =/*preferences.getString(getString(R.string.pass_key), "");*/ Config.PASS;
        int port = Integer.parseInt(preferences.getString(getString(R.string.desktop_port_key), "22"));

        int tunnelLocalPort = Integer.parseInt(preferences.getString(getString(R.string.port_key), "55555"));
        String tunnelRemoteHost = "127.0.0.1";
        int tunnelRemotePort = randInt(10000, 50000);
        Log.d(TAG, "Remote port " + tunnelRemotePort);

        JSch jsch = new JSch();
        session = jsch.getSession(user, host, port);
        session.setPassword(password);
        localUserInfo lui = new localUserInfo(password);
        session.setUserInfo(lui);
        session.connect();

        if (session != null)
            session.setPortForwardingR("0.0.0.0", tunnelRemotePort, tunnelRemoteHost, tunnelLocalPort);
    }

    /**
     * Returns a pseudo-random number between min and max, inclusive.
     * The difference between min and max can be at most
     * <code>Integer.MAX_VALUE - 1</code>.
     *
     * @param min Minimum value
     * @param max Maximum value.  Must be greater than min.
     * @return Integer between min and max, inclusive.
     * @see java.util.Random#nextInt(int)
     */
    public static int randInt(int min, int max) {

        // NOTE: Usually this should be a field rather than a method
        // variable so that it is not re-seeded every call.
        Random rand = new Random();

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        int randomNum = rand.nextInt((max - min) + 1) + min;

        return randomNum;
    }
}
