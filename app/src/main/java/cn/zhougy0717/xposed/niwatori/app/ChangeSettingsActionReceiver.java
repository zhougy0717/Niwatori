package cn.zhougy0717.xposed.niwatori.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import cn.zhougy0717.xposed.niwatori.NFW;
import cn.zhougy0717.xposed.niwatori.R;
import cn.zhougy0717.xposed.niwatori.Settings;

/**
 * Created by tkgktyk on 2015/03/29.
 */
public class ChangeSettingsActionReceiver extends BroadcastReceiver {
    private static final String TAG = ChangeSettingsActionReceiver.class.getSimpleName();

    private static final int INVALID_PERCENT = 999;

    @Override
    public void onReceive(Context context, Intent intent) {
        final SharedPreferences prefs = NFW.getSharedPreferences(context);
        Settings settings = new Settings(prefs);
        String actionIntentConsume = prefs.getString("key_action_intent_consumer", "NA");
        if (intent.hasExtra("screen_resized")){
            settings.screenResized = intent.getBooleanExtra("screen_resized", false);
        }
        if (intent.hasExtra("key_small_screen_pivot_x")) {
            settings.smallScreenPivotX = ((float)intent.getIntExtra("key_small_screen_pivot_x", 0))/100;
        }
        if (intent.hasExtra("key_small_screen_size")) {
            settings.smallScreenSize = ((float)intent.getIntExtra("key_small_screen_size", 0))/100;
        }
        if (intent.hasExtra("key_action_intent_consumer")) {
            actionIntentConsume = intent.getStringExtra("key_action_intent_consumer");
        }
        prefs.edit().putBoolean("screen_resized", settings.screenResized)
                .putInt("key_small_screen_pivot_x", (int)(100*settings.smallScreenPivotX))
                .putInt("key_small_screen_size", (int)(100*settings.smallScreenSize))
                .putString("key_action_intent_consumer", actionIntentConsume)
                .apply();
    }
}
