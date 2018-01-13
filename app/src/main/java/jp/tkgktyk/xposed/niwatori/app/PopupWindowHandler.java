package jp.tkgktyk.xposed.niwatori.app;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.PopupWindow;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import jp.tkgktyk.xposed.niwatori.FlyingHelper;
import jp.tkgktyk.xposed.niwatori.ModActivity;
import jp.tkgktyk.xposed.niwatori.NFW;
import jp.tkgktyk.xposed.niwatori.XposedModule;

/**
 * Created by zhougua on 1/11/2018.
 */

public class PopupWindowHandler extends XposedModule{
    private static final String FIELD_FLYING_HELPER = NFW.NAME + "_flyingHelper";

    private static Handler mHandler = null;
    private List<Handler> mActiveHandlers = new ArrayList<Handler>();

    private List<IReceiver> mActiveReceivers = new ArrayList<IReceiver>();
//    public PopupWindowHandler(){
//        XposedHelpers.setAdditionalInstanceField("ACTIVE RECEIVERS", "ACTIVE RECEIVERS", mActiveReceivers);
//    }
    private class Handler{
        private PopupWindow mPopupWindow;
        private FrameLayout mDecorView;
        private IReceiver mActionReceiver;
        private FlyingHelper mHelper;
        private IReceiver mSettingsLoadedReceiver;
        public Handler(PopupWindow pw){
            mPopupWindow = pw;
            mDecorView = (FrameLayout) XposedHelpers.getObjectField(pw, "mDecorView");

            mHelper = ModActivity.createFlyingHelper(mDecorView);
//            mSettingsLoadedReceiver = new SettingsLoadReceiver(mDecorView);
//            mActionReceiver = new ActionReceiver(mDecorView);
            mActionReceiver = ActionReceiver.getInstance(mDecorView, NFW.FOCUSED_DIALOG_FILTER);
            mSettingsLoadedReceiver = SettingsLoadReceiver.getInstance(mDecorView, NFW.SETTINGS_CHANGED_FILTER);
        }

        public void registerReceiver(){
            mActionReceiver.register();
            mSettingsLoadedReceiver.register();
            View v = (View) XposedHelpers.getObjectField(mPopupWindow, "mContentView");
            mHelper.setForeground(v);
        }

        public void unregisterReceiver(){
            mActionReceiver.unregister();
            mSettingsLoadedReceiver.unregister();
        }
    }

    public void install() {
        //
        // register receiver
        //
//        XposedBridge.hookAllConstructors(PopupWindow.class, new XC_MethodHook() {
//            @Override
//            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                Log.e("Ben", "Pop up window is contructed." + (PopupWindow)param.thisObject);
//            }
//        });
//        XposedBridge.hookAllMethods(PopupWindow.class, "showAsDropDown", new XC_MethodHook() {
//            @Override
//            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                Log.e("Ben", "Pop up window show as drop down." + (PopupWindow)param.thisObject);
//            }
//        });
//        XposedBridge.hookAllMethods(PopupWindow.class, "showAtLocation", new XC_MethodHook() {
//            @Override
//            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                Log.e("Ben", "Pop up window show at location." + (PopupWindow)param.thisObject);
//            }
//        });
        XposedBridge.hookAllMethods(PopupWindow.class, "invokePopup", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                final PopupWindow pw = (PopupWindow) param.thisObject;
                final FrameLayout decor = (FrameLayout) XposedHelpers.getObjectField(pw, "mDecorView");

                Rect r = new Rect();
                decor.getWindowVisibleDisplayFrame(r);

                decor.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                int width = decor.getMeasuredWidth();
                int height = decor.getMeasuredHeight();

                if (((double)width/(r.right - r.left) <= 0.5) || ((double)height/(r.bottom - r.top) <= 0.5)) {
                    return;
                }

                try {
                    Log.e("Ben", "New pop up window created");
                    mHandler = new PopupWindowHandler.Handler(pw);
                    mHandler.registerReceiver();
                    final PopupWindow.OnDismissListener dismissListener =
                            (PopupWindow.OnDismissListener) XposedHelpers.getObjectField(pw, "mOnDismissListener");
                    pw.setOnDismissListener(new PopupWindow.OnDismissListener() {
                        @Override
                        public void onDismiss() {
                            mHandler.unregisterReceiver();
                            dismissListener.onDismiss();
                        }
                    });
                } catch (Throwable t) {
                    logE(t);
                }
            }
        });

        final Class<?> classPopupDecorView = XposedHelpers.findClass("android.widget.PopupWindow$PopupDecorView", null);
        XposedHelpers.findAndHookMethod(classPopupDecorView, "dispatchTouchEvent", MotionEvent.class,
                new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                        try {
                            final FrameLayout decorView = (FrameLayout) methodHookParam.thisObject;
                                final MotionEvent event = (MotionEvent) methodHookParam.args[0];
                                final FlyingHelper helper = getHelper((FrameLayout) decorView);
                                if (helper != null && helper.onTouchEvent(event)) {
                                    return true;
                                }
                        } catch (Throwable t) {
                            logE(t);
                        }
                        return invokeOriginalMethod(methodHookParam);
                    }
                });
    }

    private static FlyingHelper getHelper(@NonNull FrameLayout decorView) {
        return (FlyingHelper) XposedHelpers.getAdditionalInstanceField(
                decorView, FIELD_FLYING_HELPER);
    }

    public static void onPause(){
//        if (mHandler != null) {
//            mHandler.unregisterReceiver();
//            mHandler = null;
//        }
        Log.e("Ben", "popup window onPause: mHandler " + mHandler);
    }

    public static void onResume(){
//        if (mHandler != null) {
//            mHandler.registerReceiver();
//        }
        Log.e("Ben", "popup window onResume: mHandler " + mHandler);
    }
}
