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
        private IReceiver mSettingsLoadedReceiver;
        private FlyingHelper mHelper;
        public Handler(FrameLayout decorView) {
            mDecorView = decorView;
            mActionReceiver = ActionReceiver.getInstance(mDecorView, NFW.FOCUSED_ACTIVITY_FILTER);
            mSettingsLoadedReceiver = SettingsLoadReceiver.getInstance(mDecorView, NFW.SETTINGS_CHANGED_FILTER);
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
    public static void install() {
        //
        // initialize addtional fields
        //
        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    final Activity activity = (Activity) param.thisObject;
                    FrameLayout decorView = (FrameLayout)activity.getWindow().peekDecorView();
                    Handler handler = new Handler(decorView);
                    handler.registerReceiver();
                    PopupWindowHandler.onResume(activity.getClass().getName());
                } catch (Throwable t) {
                    logE(t);
                }
            }
        });

        XposedHelpers.findAndHookMethod(Activity.class, "onPause", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    Activity activity = (Activity) param.thisObject;
                    logD(activity + "#onPause");
                    FrameLayout decorView = (FrameLayout) activity.getWindow().peekDecorView();
                    Handler handler = new Handler(decorView);
                    handler.unregisterReceiver();
                    PopupWindowHandler.onPause(activity.getClass().getName());
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
