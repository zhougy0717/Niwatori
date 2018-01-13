package jp.tkgktyk.xposed.niwatori;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import jp.tkgktyk.xposed.niwatori.app.ActionReceiver;
import jp.tkgktyk.xposed.niwatori.app.IReceiver;
import jp.tkgktyk.xposed.niwatori.app.PopupWindowHandler;
import jp.tkgktyk.xposed.niwatori.app.SettingsLoadReceiver;

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
        private FlyingHelper mHelper;
        public Handler(FrameLayout decorView) {
            mDecorView = decorView;
//            mActionReceiver = new ActionReceiver(mDecorView);
            mActionReceiver = ActionReceiver.getInstance(mDecorView, NFW.FOCUSED_ACTIVITY_FILTER);
//            mHelper = getHelper(mDecorView);
        }

        private static Handler getInstance(FrameLayout decorView){
            Handler handler = (Handler) XposedHelpers.getAdditionalInstanceField(decorView, "HANDLER");
            if (handler == null) {
                handler = new Handler(decorView);
                XposedHelpers.setAdditionalInstanceField(decorView, "HANDLER", handler);
            }
            return handler;
        }

        public void registerReceiver(){
            mActionReceiver.register();
            mHelper = getHelper(mDecorView);
            Log.e("Ben", "Activity handler register: helper " + mHelper);
            if (mHelper != null && mHelper.getSettings().smallScreenPersistent) {
                NFW.requestResizedGlobal(mDecorView);
            }
        }

        public void unregisterReceiver(){
            mActionReceiver.unregister();
            mHelper = getHelper(mDecorView);
            Log.e("Ben", "Activity handler unregister: helper " + mHelper);
            if (mHelper!=null && mHelper.getSettings().autoReset) {
                // When fire actions from shortcut (ActionActivity), it causes onPause and onResume events
                // because through an Activity. So shouldn't reset automatically.
                mHelper.resetState(true);
            }
        }
    }
    public static void install() {
        //
        // initialize addtional fields
        //
//        XposedBridge.hookAllConstructors(Activity.class, new XC_MethodHook() {
//            @Override
//            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                try {
//                    final Activity activity = (Activity) param.thisObject;
//                    XposedHelpers.setAdditionalInstanceField(activity, FIELD_RECEIVER_REGISTERED, false);
//                    XposedHelpers.setAdditionalInstanceField(activity, FIELD_HAS_FOCUS, false);
//                } catch (Throwable t) {
//                    logE(t);
//                }
//            }
//        });
        //
        // register broadcast receiver
        //
//        XposedHelpers.findAndHookMethod(Activity.class, "onWindowFocusChanged", boolean.class
//                , new XC_MethodHook() {
//                    @Override
//                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                        try {
//                            final Activity activity = (Activity) param.thisObject;
//                            final boolean hasFocus = (Boolean) param.args[0];
//                            logD(activity + "#onWindowFocusChanged: hasFocus=" + hasFocus);
//                            registerReceiver(activity, hasFocus);
//                        } catch (Throwable t) {
//                            logE(t);
//                        }
//                    }
//                });
        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    final Activity activity = (Activity) param.thisObject;
//                    final boolean hasFocus = activity.hasWindowFocus();
//                    logD(activity + "#onResume: hasFocus=" + hasFocus);
//                    registerReceiver(activity, hasFocus);
                    Log.e("Ben", "activity onResume" + activity);
                    FrameLayout decorView = (FrameLayout)activity.getWindow().peekDecorView();
//                    Handler handler = Handler.getInstance(decorView);
                    Handler handler = new Handler(decorView);
                    handler.registerReceiver();
                    PopupWindowHandler.onResume();

//                    final FlyingHelper helper = getHelper(activity);
//                    if (helper != null && helper.getSettings().smallScreenPersistent) {
//                        NFW.requestResizedGlobal(activity);
//                    }
                } catch (Throwable t) {
                    logE(t);
                }
            }
        });
        XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
//                if (activity.getPackageName().startsWith("com.smzdm.client.android")){
                    Log.e("Ben", "activity onCreate:" + activity);
//                }
            }
        });
        XposedHelpers.findAndHookMethod(Activity.class, "onStart", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
//                if (activity.getPackageName().startsWith("com.smzdm.client.android")){
                    Log.e("Ben", "activity onStart:" + activity);
                }
//            }
        });
        XposedHelpers.findAndHookMethod(Activity.class, "onStop", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
//                if (activity.getPackageName().startsWith("com.smzdm.client.android")){
                    Log.e("Ben", "activity onStop:" + activity);
//                }
            }
        });
        XposedHelpers.findAndHookMethod(Activity.class, "onDestroy", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
//                if (activity.getPackageName().startsWith("com.smzdm.client.android")){
                    Log.e("Ben", "activity onDestroy:" + activity);
//                }
            }
        });
        XposedHelpers.findAndHookMethod(Activity.class, "onPause", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    Activity activity = (Activity) param.thisObject;
                    logD(activity + "#onPause");
                    Log.e("Ben", "activity onPause" + activity);
                    FrameLayout decorView = (FrameLayout) activity.getWindow().peekDecorView();
//                    Handler handler = Handler.getInstance(decorView);
                    Handler handler = new Handler(decorView);
                    handler.unregisterReceiver();
//                    unregisterReceiver(activity);
//                    resetAutomatically(activity);
                    PopupWindowHandler.onPause();
                } catch (Throwable t) {
                    logE(t);
                }
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
