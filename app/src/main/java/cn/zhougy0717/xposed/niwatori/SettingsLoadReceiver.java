package cn.zhougy0717.xposed.niwatori;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.FrameLayout;

import de.robv.android.xposed.XposedHelpers;

/**
 * Created by zhougua on 1/12/2018.
 */

public class SettingsLoadReceiver extends Receiver implements IReceiver {
    private static final String FIELD_SETTINGS_CHANGED_RECEIVER = NFW.NAME + "_settingsChangedReceiver";
    private BroadcastReceiver mReceiver;
    private boolean mRegistered = false;

    public static IReceiver getInstance(FrameLayout decorView, IntentFilter filter) {
        IReceiver recv = (IReceiver) XposedHelpers.getAdditionalInstanceField(decorView, "SETTINGS_LOAD_RECEIVER");
        if (recv == null) {
            recv = new SettingsLoadReceiver(decorView, filter);
            XposedHelpers.setAdditionalInstanceField(decorView, "SETTINGS_LOAD_RECEIVER", recv);
        }
        return recv;
    }

    public SettingsLoadReceiver (FrameLayout decorView, IntentFilter filter){
        super(decorView, filter);
        create();
    }
    public SettingsLoadReceiver (FrameLayout decorView) {
        super(decorView);
        create();
    }

    public void register(){
        if(!mRegistered){
            mDecorView.getContext().registerReceiver(mReceiver,mFilter);
            mRegistered = true;
        }
    }

    public void unregister(){
        if(mRegistered) {
            mDecorView.getContext().registerReceiver(mReceiver, mFilter);
        }
    }
    public final BroadcastReceiver create() {
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                NFW.Settings settings = (NFW.Settings) intent.getSerializableExtra(NFW.EXTRA_SETTINGS);
                handler(settings);
            }
        };
        return mReceiver;
    }

    private void handler(NFW.Settings settings){
        logD(mDecorView.getContext().getPackageName() + ": reload settings");
        // need to reload on each package?
        final FlyingHelper helper = getHelper(mDecorView);
        if (helper != null) {
            getHelper(mDecorView).onSettingsLoaded(settings);
        }
    }
}
