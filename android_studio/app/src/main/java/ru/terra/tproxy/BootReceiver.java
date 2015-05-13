package ru.terra.tproxy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import ru.terra.tproxy.service.ProxyService;

/**
 * Created by terranz on 13.05.15.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, ProxyService.class));
    }
}
