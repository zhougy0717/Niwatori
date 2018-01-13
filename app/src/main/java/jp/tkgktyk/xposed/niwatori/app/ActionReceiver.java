package jp.tkgktyk.xposed.niwatori.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.FrameLayout;

import de.robv.android.xposed.XposedHelpers;
import jp.tkgktyk.xposed.niwatori.FlyingHelper;
import jp.tkgktyk.xposed.niwatori.NFW;

/**
 * Created by zhougua on 1/12/2018.
 */

public class ActionReceiver extends Receiver implements IReceiver{
    private static final String FIELD_DIALOG_ACTION_RECEIVER = NFW.NAME + "_dialogActionReceiver";

    public ActionReceiver (FrameLayout decorView) {
        super(decorView);
        create();
    }

    private BroadcastReceiver mReceiver;
    public final BroadcastReceiver create() {
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                try {
                    handler(action);
                    abortBroadcast();
                } catch (Throwable t) {
                    logE(t);
                }
            }
        };
        return mReceiver;
    }

    private void handler (String action){
        FlyingHelper helper = getHelper(mDecorView);
        final String packageName = mDecorView.getContext().getPackageName();
        if (helper.getSettings().blackList.contains(packageName)) {
            if (helper.getSettings().logActions) {
                log(mDecorView.toString() + "is ignored");
            }
            return;
        }
        helper.performAction(action);
        if (helper.getSettings().logActions) {
            log(packageName + " consumed: " + action);
        }
    }

    public void register(){
        final BroadcastReceiver existingReceiver = (BroadcastReceiver) XposedHelpers
                .getAdditionalInstanceField(mDecorView, FIELD_DIALOG_ACTION_RECEIVER);
        if (existingReceiver == null) {
            XposedHelpers.setAdditionalInstanceField(mDecorView, FIELD_DIALOG_ACTION_RECEIVER, mReceiver);
            mDecorView.getContext().registerReceiver(mReceiver, NFW.FOCUSED_DIALOG_FILTER);
            Log.e("Ben", "register action receiver");
//            XposedHelpers.setAdditionalInstanceField("CURRENT DECOR VIEW", "VIEW", mDecorView);
        }
//        super.register();
    }

    public void unregister(){
        final BroadcastReceiver actionReceiver = (BroadcastReceiver) XposedHelpers
                .getAdditionalInstanceField(mDecorView, FIELD_DIALOG_ACTION_RECEIVER);
        if (actionReceiver != null) {
            mDecorView.getContext().unregisterReceiver(mReceiver);
            getHelper(mDecorView).resetState(true);
            XposedHelpers.setAdditionalInstanceField(
                    mDecorView, FIELD_DIALOG_ACTION_RECEIVER, null);
        }
        Log.e("Ben", "action receiver is removed " + mDecorView);
    }
}
