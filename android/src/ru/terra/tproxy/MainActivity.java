package ru.terra.tproxy;

import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import ru.terra.tproxy.proxy.UpdateNotifier;
import ru.terra.tproxy.proxy.Updater;
import ru.terra.tproxy.service.ProxyService;

import java.util.ArrayList;
import java.util.List;

/**
 * Date: 24.06.14
 * Time: 18:08
 */
public class MainActivity extends Activity {
    public static final String STATUS_RECEIVER = "ru.terra.tproxy.MainActivity.receiver";
    private LocalBroadcastManager lbm;
    private SharedPreferences prefs;
    private Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_main);

        lbm = LocalBroadcastManager.getInstance(this);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final ListView lvLog = (ListView) findViewById(R.id.lvLog);
        final List<String> log = new ArrayList<>();
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, log);
        final TextView tvStatus = (TextView) findViewById(R.id.tvStatus);
        lvLog.setAdapter(adapter);

        Updater.getUpdater().setNotifier(new UpdateNotifier() {
            @Override
            public void start(final String url, final Long size, String cache) {
                lvLog.post(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (log) {
                            log.add(url + ":" + size);
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        });

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, final Intent intent) {
                tvStatus.post(new Runnable() {
                    @Override
                    public void run() {
                        tvStatus.setText(intent.getStringExtra("text"));
                    }
                });
            }
        };
        IntentFilter statusFilter = new IntentFilter(STATUS_RECEIVER);
        statusFilter.addCategory(Intent.CATEGORY_DEFAULT);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, statusFilter);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.m_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mi_start: {
                startService(new Intent(this, ProxyService.class));
                item.setEnabled(false);
                menu.findItem(R.id.mi_stop).setEnabled(true);
                return true;
            }
            case R.id.mi_stop: {
                lbm.sendBroadcast(new Intent(ProxyService.PROXY_RECEIVER).putExtra("stop", true));
                menu.findItem(R.id.mi_start).setEnabled(true);
                item.setEnabled(false);
                return true;
            }
            case R.id.mi_prefs: {
                startActivity(new Intent(this, PrefsActivity.class));
                return true;
            }
            default:
                return false;
        }
    }

}
