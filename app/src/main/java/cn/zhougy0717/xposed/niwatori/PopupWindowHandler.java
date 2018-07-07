package cn.zhougy0717.xposed.niwatori;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.PopupWindow;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by zhougua on 1/11/2018.
 */

public class PopupWindowHandler extends XposedModule{
    private static final String FIELD_FLYING_HANDLER = NFW.NAME + "_flyingHandler";

    private static FlyingHandler mActiveHandler = null;
    private static Activity mCurrentActivity = null;
    private final static String FIELD_ACTIVITY_HANDLER = "activity_handler";

    private static class FlyingHandler {
        private PopupWindow mPopupWindow = null;
        private FrameLayout mDecorView;
        private IReceiver mActionReceiver;
        private FlyingHelper mHelper;
        private IReceiver mSettingsLoadedReceiver;
        private GestureDetector mEdgeGesture;
        public static FlyingHandler create(FrameLayout decorView) {
            FlyingHandler handler = (FlyingHandler) XposedHelpers.getAdditionalInstanceField(decorView, FIELD_FLYING_HANDLER);
            if (handler == null) {
                handler = new FlyingHandler(decorView);
                XposedHelpers.setAdditionalInstanceField(decorView, FIELD_FLYING_HANDLER, handler);
            }
            return handler;
        }
        public static FlyingHandler create(PopupWindow pw) {
            FrameLayout decorView = (FrameLayout) XposedHelpers.getObjectField(pw, "mDecorView");
            return create(decorView);
        }

        private FlyingHandler(FrameLayout decorView){
            mDecorView = decorView;
            mHelper = ModActivity.createFlyingHelper(mDecorView);
            mActionReceiver = ActionReceiver.getInstance(mDecorView, NFW.FOCUSED_DIALOG_FILTER);
            mSettingsLoadedReceiver = SettingsLoadReceiver.getInstance(mDecorView, NFW.SETTINGS_CHANGED_FILTER);

            mEdgeGesture = new GestureDetector(mDecorView.getContext(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onFling(MotionEvent event1, MotionEvent event2,float velocityX, float velocityY) {
                    if (!mHelper.edgeDetected(event1)) {
                        return false;
                    }
                    NFW.performAction(mDecorView.getContext(), NFW.ACTION_SMALL_SCREEN);
                    return true;
                }

                @Override
                public boolean onDown(MotionEvent event){
                    if (!edgeDetected(event)) {
                        return false;
                    }
                    return true;
                }
            });
        }

        public void registerReceiver(){
            mActionReceiver.register();
            mSettingsLoadedReceiver.register();
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

        public boolean edgeDetected(MotionEvent event){
            return mHelper.edgeDetected(event);
        }

        public boolean onTouchEvent(MotionEvent event){
            if(mEdgeGesture.onTouchEvent(event)) {
                return true;
            }
            return mHelper.onTouchEvent(event);
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
                            mActiveHandler = FlyingHandler.create(pw);
                            mActiveHandler.registerReceiver();
                            mActiveHandler.dealWithPersistentIn();
                        } catch (Throwable t) {
                            logE(t);
                        }
                    }
                });
            }
        });

        XposedBridge.hookAllMethods(PopupWindow.class, "dismiss", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (mActiveHandler != null) {
                    mActiveHandler.dealWithPersistentOut();
                    mActiveHandler.unregisterReceiver();
                    mActiveHandler = null;
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
                            FlyingHandler handler = FlyingHandler.create(decorView);
                            if (handler.onTouchEvent(event)) {
                                return true;
                            }
                        } catch (Throwable t) {
                            logE(t);
                        }
                        return invokeOriginalMethod(methodHookParam);
                    }
                });
    }

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
        if(mActiveHandler != null){
            mActiveHandler.registerReceiver();
            mActiveHandler.dealWithPersistentIn();
        }
    }
}
