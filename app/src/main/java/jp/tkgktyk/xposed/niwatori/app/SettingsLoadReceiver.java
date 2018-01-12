package jp.tkgktyk.xposed.niwatori.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.FrameLayout;

import de.robv.android.xposed.XposedHelpers;
import jp.tkgktyk.xposed.niwatori.FlyingHelper;
import jp.tkgktyk.xposed.niwatori.NFW;

/**
 * Created by zhougua on 1/12/2018.
 */

public class SettingsLoadReceiver extends Receiver implements IReceiver {
    private static final String FIELD_SETTINGS_CHANGED_RECEIVER = NFW.NAME + "_settingsChangedReceiver";
    private BroadcastReceiver mReceiver;

    public SettingsLoadReceiver (FrameLayout decorView) {
        super(decorView);
        create();
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

    public void register(){
        final BroadcastReceiver settingsLoadReceiver = (BroadcastReceiver) XposedHelpers
                .getAdditionalInstanceField(mDecorView, FIELD_SETTINGS_CHANGED_RECEIVER);
        if(settingsLoadReceiver == null) {
            XposedHelpers.setAdditionalInstanceField(mDecorView,
                    FIELD_SETTINGS_CHANGED_RECEIVER, mReceiver);
            mDecorView.getContext().registerReceiver(mReceiver,
                    NFW.SETTINGS_CHANGED_FILTER);
        }
    }

    public void unregister(){
        final BroadcastReceiver settingsLoadReceiver = (BroadcastReceiver) XposedHelpers
                .getAdditionalInstanceField(mDecorView, FIELD_SETTINGS_CHANGED_RECEIVER);
        if(settingsLoadReceiver != null) {
            mDecorView.getContext().registerReceiver(mReceiver,
                    NFW.SETTINGS_CHANGED_FILTER);
        }
    }
}
