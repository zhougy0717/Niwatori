package cn.zhougy0717.xposed.niwatori;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
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
            //                final Activity activity = (Activity) param.thisObject;
            FrameLayout decorView = (FrameLayout)activity.getWindow().peekDecorView();
            ReceiverManager handler = new ReceiverManager(decorView);
            handler.registerReceiver();
            PopupWindowHandler.onResume(activity.getClass().getName());

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
            PopupWindowHandler.onPause(activity.getClass().getName());
            ModActivity.getHelper(decorView).onExit();
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
