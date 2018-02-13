package cn.zhougy0717.xposed.niwatori;

import android.app.Activity;
import android.app.Application;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.FrameLayout;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by zhougua on 1/13/2018.
 */

public class ActivityHandler extends XposedModule{
    private static final String FIELD_FLYING_HELPER = NFW.NAME + "_flyingHelper";

    private static FlyingHelper getHelper(@NonNull FrameLayout decorView) {
        return (FlyingHelper) XposedHelpers.getAdditionalInstanceField(
                decorView, FIELD_FLYING_HELPER);
    }

    private static class Handler{
        private FrameLayout mDecorView;
        private IReceiver mActionReceiver;
        private IReceiver mSettingsLoadedReceiver;
        private FlyingHelper mHelper;
        public Handler(FrameLayout decorView) {
            mDecorView = decorView;
            mActionReceiver = ActionReceiver.getInstance(mDecorView, NFW.FOCUSED_ACTIVITY_FILTER);
            mSettingsLoadedReceiver = SettingsLoadReceiver.getInstance(mDecorView, NFW.SETTINGS_CHANGED_FILTER);
        }

//        private static Handler getInstance(FrameLayout decorView){
//            Handler handler = (Handler) XposedHelpers.getAdditionalInstanceField(decorView, "HANDLER");
//            if (handler == null) {
//                handler = new Handler(decorView);
//                XposedHelpers.setAdditionalInstanceField(decorView, "HANDLER", handler);
//            }
//            return handler;
//        }

        public void registerReceiver(){
            mActionReceiver.register();
            mSettingsLoadedReceiver.register();
            mHelper = getHelper(mDecorView);
            if (mHelper != null && mHelper.getSettings().smallScreenPersistent) {
                NFW.requestResizedGlobal(mDecorView);
            }
        }

        public void unregisterReceiver(){
            mActionReceiver.unregister();
            mSettingsLoadedReceiver.unregister();
            mHelper = getHelper(mDecorView);
            if (mHelper!=null && mHelper.getSettings().autoReset) {
                // NOTE: When fire actions from shortcut (ActionActivity), it causes onPause and onResume events
                // because through an Activity. So shouldn't reset automatically.
                mHelper.resetState(true);
            }
        }
    }

    static class MyCallbacks implements Application.ActivityLifecycleCallbacks{
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
                Handler handler = new Handler(decorView);
                handler.registerReceiver();
                PopupWindowHandler.onResume(activity.getClass().getName());

                if (decorView.getBackground() == null) {
                    XposedHelpers.callMethod(decorView, "setWindowBackground", ModActivity.censorDrawable(decorView, null)/*new ColorDrawable(Color.WHITE)*/);
                }
            } catch (Throwable t) {
                logE(t);
            }
        }

        @Override
        public void onActivityPaused(Activity activity) {
            try {
                //                Activity activity = (Activity) param.thisObject;
                logD(activity + "#onPause");
                FrameLayout decorView = (FrameLayout) activity.getWindow().peekDecorView();
                Handler handler = new Handler(decorView);
                handler.unregisterReceiver();
                PopupWindowHandler.onPause(activity.getClass().getName());
            } catch (Throwable t) {
                logE(t);
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
    public static void install() {
        //
        // initialize addtional fields
        //
        XposedHelpers.findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Application application = (Application)param.thisObject;
                application.registerActivityLifecycleCallbacks(new MyCallbacks());
            }
        });
        //
        // screen rotation
        //
        XposedHelpers.findAndHookMethod(Activity.class, "onConfigurationChanged", Configuration.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            final Activity activity = (Activity) param.thisObject;
//                            final FlyingHelper helper = getHelper(activity);
                            final FlyingHelper helper = getHelper((FrameLayout) activity.getWindow().peekDecorView());
                            if (helper == null) {
                                logD("DecorView is null");
                                return;
                            }
                            final Configuration newConfig = (Configuration) param.args[0];
                            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                                helper.rotate();
                            } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                                helper.rotate();
                            }
                        } catch (Throwable t) {
                            logE(t);
                        }
                    }
                });
    }
}
