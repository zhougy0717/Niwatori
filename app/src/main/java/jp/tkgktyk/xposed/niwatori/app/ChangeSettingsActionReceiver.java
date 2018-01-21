package jp.tkgktyk.xposed.niwatori.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import jp.tkgktyk.xposed.niwatori.NFW;
import jp.tkgktyk.xposed.niwatori.R;

/**
 * Created by tkgktyk on 2015/03/29.
 */
public class ChangeSettingsActionReceiver extends BroadcastReceiver {
    private static final String TAG = ChangeSettingsActionReceiver.class.getSimpleName();

    private static final int INVALID_PERCENT = 999;

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean save = true;
        final SharedPreferences prefs = NFW.getSharedPreferences(context);

        final String action = intent.getAction();
        Log.d(TAG, action);
        if (action.equals(NFW.ACTION_CS_SWAP_LEFT_RIGHT)) {
            final String keyInitX = context.getString(R.string.key_initial_x_percent);
            final int initX = prefs.getInt(keyInitX, INVALID_PERCENT);
            if (initX == INVALID_PERCENT) {
                Log.d(TAG, "invalid init x");
                save = false;
            }

            final String keyPivotX = context.getString(R.string.key_small_screen_pivot_x);
            final int pivotX = prefs.getInt(keyPivotX, INVALID_PERCENT);
            if (pivotX == INVALID_PERCENT) {
                Log.d(TAG, "invalid pivot x");
                save = false;
            }

            if (save) {
                prefs.edit()
                        .putInt(keyInitX, -initX)
                        .putInt(keyPivotX, 100 - pivotX)
                        .apply();
            }
        }

        if (save) {
            NFW.sendSettingsChanged(context, prefs);
            Toast.makeText(context, R.string.action_cs_swap_left_right, Toast.LENGTH_SHORT).show();
        }
    }
}
