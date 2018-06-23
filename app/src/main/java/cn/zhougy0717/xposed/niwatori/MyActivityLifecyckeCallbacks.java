package cn.zhougy0717.xposed.niwatori;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.widget.FrameLayout;

import de.robv.android.xposed.XposedHelpers;

public class MyActivityLifecyckeCallbacks implements Application.ActivityLifecycleCallbacks{
    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
        try {
            FrameLayout decorView = (FrameLayout)activity.getWindow().peekDecorView();
            (new ReceiverManager(decorView)).registerReceiver();
            PopupWindowHandler.onResume(activity);

            final FlyingHelper helper = ModActivity.getHelper(decorView);
            if (helper != null) {
                /**
                 * Because onResume and onPause are running in parallel,
                 * the order of registerReceiver and sendBroadcast is not promised.
                 * This handler is handling the case of:
                 *      registerReceiver is invoked after sendBroadcast
                 * In this case, SettingsLoadRecever won't be triggered.
                 * Because of the latency between write and read consistently, the Runnable should be executed by 50ms delayed.
                 * Refer to comment in SettingsLoadReceiver for another case.
                 */
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (helper.getSettings().smallScreenPersistent) {
                            if (helper.getSettings().screenResized && !helper.isResized()) {
                                helper.performAction(NFW.ACTION_FORCE_SMALL_SCREEN);
                            } else if (helper.getSettings().screenResized && helper.isResized()) {
                                helper.performAction(NFW.ACTION_REFRESH_SMALL_SCREEN);
                            } else if (!helper.getSettings().screenResized && helper.isResized()) {
                                helper.performAction(NFW.ACTION_RESET);
                            }
                        }
                    }
                }, 50);
            }
            if (decorView.getBackground() == null) {
                XposedHelpers.callMethod(decorView, "setWindowBackground", ModActivity.censorDrawable(decorView, null)/*new ColorDrawable(Color.WHITE)*/);
            }
        } catch (Throwable t) {
            XposedModule.logE(t);
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        try {
            //                Activity activity = (Activity) param.thisObject;
            XposedModule.logD(activity + "#onPause");
            FrameLayout decorView = (FrameLayout) activity.getWindow().peekDecorView();
            ReceiverManager handler = new ReceiverManager(decorView);
            handler.unregisterReceiver();
            FlyingHelper helper = ModActivity.getHelper(decorView);
            if (helper!=null && !helper.getSettings().smallScreenPersistent) {
                // NOTE: When fire actions from shortcut (ActionActivity), it causes onPause and onResume events
                // because through an Activity. So shouldn't reset automatically.
                helper.resetState(true);
            }
            PopupWindowHandler.onPause(activity);
            helper.onExit();
        } catch (Throwable t) {
            XposedModule.logE(t);
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }
}
