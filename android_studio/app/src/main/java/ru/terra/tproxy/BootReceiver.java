package ru.terra.tproxy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import ru.terra.tproxy.service.ProxyService;

/**
 * Created by terranz on 13.05.15.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean(context.getString(R.string.autostart), false))
            context.startService(new Intent(context, ProxyService.class));
    }
}
