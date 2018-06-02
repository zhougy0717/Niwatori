package cn.zhougy0717.xposed.niwatori.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import cn.zhougy0717.xposed.niwatori.NFW;
import cn.zhougy0717.xposed.niwatori.R;

/**
 * Created by tkgktyk on 2015/03/29.
 */
public class ChangeSettingsActionReceiver extends BroadcastReceiver {
    private static final String TAG = ChangeSettingsActionReceiver.class.getSimpleName();

    private static final int INVALID_PERCENT = 999;

    @Override
    public void onReceive(Context context, Intent intent) {
        final SharedPreferences prefs = NFW.getSharedPreferences(context);
        Log.e("Ben", "received intent");
        if (intent.hasExtra("screen_resized")){
            prefs.edit().putBoolean("screen_resized", intent.getBooleanExtra("screen_resized", false))
                    .apply();
            Log.e("Ben", "screen_resized");
        }
        if (intent.hasExtra("key_small_screen_pivot_x")) {
            prefs.edit().putInt("key_small_screen_pivot_x", intent.getIntExtra("key_small_screen_pivot_x", 0))
                    .apply();
            Log.e("Ben", "key_small_screen_pivot_x");
        }
        if (intent.hasExtra("key_small_screen_size")) {
            prefs.edit().putInt("key_small_screen_size", intent.getIntExtra("key_small_screen_size", 0))
                    .apply();
            Log.e("Ben", "key_small_screen_size");
        }
    }
}
