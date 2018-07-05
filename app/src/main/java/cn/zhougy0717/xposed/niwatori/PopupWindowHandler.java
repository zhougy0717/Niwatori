package cn.zhougy0717.xposed.niwatori;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.PopupWindow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by zhougua on 1/11/2018.
 */

public class PopupWindowHandler extends XposedModule{
    private static final String FIELD_FLYING_HELPER = NFW.NAME + "_flyingHelper";

    private static Handler mActiveHandler = null;
    private static Activity mCurrentActivity = null;
    private final static String FIELD_ACTIVITY_HANDLER = "activity_handler";

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
            mActionReceiver = ActionReceiver.getInstance(mDecorView, NFW.FOCUSED_DIALOG_FILTER);
            mSettingsLoadedReceiver = SettingsLoadReceiver.getInstance(mDecorView, NFW.SETTINGS_CHANGED_FILTER);
        }

        public void registerReceiver(){
            mActionReceiver.register();
            mSettingsLoadedReceiver.register();
            View v = (View) XposedHelpers.getObjectField(mPopupWindow, "mContentView");
//            mHelper.setForeground(v);
        }

        public void unregisterReceiver(){
            mActionReceiver.unregister();
            mSettingsLoadedReceiver.unregister();
        }

        public void dealWithPersistentIn(){
            FlyingHelper helper = ModActivity.getHelper((FrameLayout) mCurrentActivity.getWindow().peekDecorView());
            if (mHelper.getSettings().smallScreenPersistent) {
                // In persistent small screen mode, sync up with parent activity and the popup window.
                if (helper.isResized() && !mHelper.isResized()) {
                    mHelper.performAction(NFW.ACTION_FORCE_SMALL_SCREEN);
                } else if (!helper.isResized() && mHelper.isResized()) {
                    mHelper.performAction(NFW.ACTION_RESET);
                }
            }
        }
        
        public void dealWithPersistentOut() {
            if (mHelper!=null && !mHelper.getSettings().smallScreenPersistent) {
                // NOTE: When fire actions from shortcut (ActionActivity), it causes onPause and onResume events
                // because through an Activity. So shouldn't reset automatically.
                mHelper.resetState(true);
            }
            FlyingHelper helper = ModActivity.getHelper((FrameLayout) mCurrentActivity.getWindow().peekDecorView());
            if (helper.getSettings().smallScreenPersistent) {
                // In persistent small screen mode, sync up with parent activity and the popup window.
                if (mHelper.isResized() && !helper.isResized()) {
                    helper.performAction(NFW.ACTION_FORCE_SMALL_SCREEN);
                } else if (mHelper.isResized() && helper.isResized()) {
                    helper.performAction(NFW.ACTION_REFRESH_SMALL_SCREEN);
                } else if (!mHelper.isResized() && helper.isResized()) {
                    helper.performAction(NFW.ACTION_RESET);
                }
            }
        }
    }

    public void install() {
        //
        // register receiver
        //
        XposedBridge.hookAllMethods(PopupWindow.class, "invokePopup", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                final PopupWindow pw = (PopupWindow) param.thisObject;
                final FrameLayout decor = (FrameLayout) XposedHelpers.getObjectField(pw, "mDecorView");

                decor.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        int width = decor.getWidth();
                        int height = decor.getHeight();

                        Rect r = new Rect();
                        decor.getWindowVisibleDisplayFrame(r);
                        if (((double)width/(r.right - r.left) <= 0.5) && ((double)height/(r.bottom - r.top) <= 0.5)) {
                            return;
                        }

                        if (mActiveHandler != null){
                            return;
                        }
                        try {
                            mActiveHandler = new PopupWindowHandler.Handler(pw);
                            mActiveHandler.registerReceiver();
                            XposedHelpers.setAdditionalInstanceField(mCurrentActivity, FIELD_ACTIVITY_HANDLER, mActiveHandler);
                            mActiveHandler.dealWithPersistentIn();
                            final PopupWindow.OnDismissListener dismissListener =
                                    (PopupWindow.OnDismissListener) XposedHelpers.getObjectField(pw, "mOnDismissListener");
                            pw.setOnDismissListener(new PopupWindow.OnDismissListener() {
                                @Override
                                public void onDismiss() {
                                    if (mActiveHandler != null) {
                                        mActiveHandler.dealWithPersistentOut();
                                        mActiveHandler.unregisterReceiver();
                                        mActiveHandler = null;
                                    }
                                    if (dismissListener != null) {
                                        dismissListener.onDismiss();
                                    }
                                }
                            });
                        } catch (Throwable t) {
                            logE(t);
                        }
                    }
                });
            }
        });

        final Class<?> classPopupDecorView = XposedHelpers.findClass("android.widget.PopupWindow$PopupDecorView", null);
        XposedHelpers.findAndHookMethod(classPopupDecorView, "onTouchEvent", MotionEvent.class,
                new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                        try {
                            final FrameLayout decorView = (FrameLayout) methodHookParam.thisObject;
                                final MotionEvent event = (MotionEvent) methodHookParam.args[0];
//                                final FlyingHelper helper = getHelper((FrameLayout) decorView);
                                final FlyingHelper helper = ModActivity.getHelper(decorView);
                                if (helper != null && helper.onTouchEvent(event)) {
                                    return true;
                                }
                        } catch (Throwable t) {
                            logE(t);
                        }
                        return invokeOriginalMethod(methodHookParam);
                    }
                });

        XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                // TODO: clear the existing handler under this activity's name
                Activity activity = (Activity) param.thisObject;
            }
        });
    }

//    private static FlyingHelper getHelper(@NonNull FrameLayout decorView) {
//        return (FlyingHelper) XposedHelpers.getAdditionalInstanceField(
//                decorView, FIELD_FLYING_HELPER);
//    }

    public static void onPause(Activity activity){
        // TODO: save the handler under the activity's name
        if(mActiveHandler != null){
            mActiveHandler.unregisterReceiver();
            mActiveHandler.dealWithPersistentOut();
        }
    }

    public static void onResume(Activity activity){
        // TODO: check the existing handler using the activity's class name
        mCurrentActivity = activity;
        Handler handler= (Handler) XposedHelpers.getAdditionalInstanceField(mCurrentActivity, FIELD_ACTIVITY_HANDLER);
        if (handler != null){
            handler.registerReceiver();
            handler.dealWithPersistentIn();
        }
    }
}
