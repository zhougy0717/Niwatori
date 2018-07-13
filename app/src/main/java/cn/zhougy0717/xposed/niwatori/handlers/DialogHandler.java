package cn.zhougy0717.xposed.niwatori.handlers;

import android.app.Activity;
import android.app.Dialog;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import cn.zhougy0717.xposed.niwatori.NFW;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by zhougua on 1/11/2018.
 */

public class DialogHandler extends BaseHandler {
    private static final String CLASS_SOFT_INPUT_WINDOW = "android.inputmethodservice.SoftInputWindow";
    private static IFlyingHandler mActiveHandler = null;

    @Override
    final protected IFlyingHandler allocateHandler(FrameLayout decorView) {
        return new CustomizedHandler(decorView);
    }

    @Override
    final protected IFlyingHandler allocateHandler(Object obj){
        return new CustomizedHandler((Dialog) obj);
    }

    @Override
    final protected FrameLayout getDecorView(Object obj){
        return (FrameLayout)((Dialog)obj).getWindow().peekDecorView();
    }

    private static class CustomizedHandler extends BaseHandler.FlyingHandler {
        private Dialog mDialog;

        @Override
        protected void actionOnFling() {
            dragDown();
        }

        private CustomizedHandler(FrameLayout decorView){
            super(decorView);
        }

        private CustomizedHandler(Dialog dialog) {
            this((FrameLayout)dialog.getWindow().peekDecorView());
            mDialog = dialog;
        }

        private void dragDown() {
            // Show Dialog on Bottom Left/Right.
            Window win = mDialog.getWindow();
            WindowManager.LayoutParams params = win.getAttributes();
            boolean left = mHelper.getSettings().getSmallScreenPivotX() < 0.5;
            params.gravity = (left?Gravity.LEFT:Gravity.RIGHT )| Gravity.BOTTOM;
            win.setAttributes(params);
            mDialog.show();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event){
            return mEdgeGesture.onTouchEvent(event);
        }
    }
    public void install() {
        //
        // register receiver
        //
        XposedBridge.hookAllMethods(Dialog.class, "onTouchEvent", new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                Dialog dialog = (Dialog) param.thisObject;
                MotionEvent event = (MotionEvent) param.args[0];
                IFlyingHandler handler = createFlyingHandler(dialog);
                if (handler.onTouchEvent(event)) {
                    return true;
                }
                if ((event.getAction() == MotionEvent.ACTION_DOWN) && handler.edgeDetected(event)) {
                    // We want to hijack ACTION_DOWN on edge. Beucase ACTION_DOWN will dismiss the dialog.
                    return false;
                }

                return invokeOriginalMethod(param);
            }
        });

        /**
         * onAttachedToWindow: dialog is contructed and show up
         */
        XposedHelpers.findAndHookMethod(Dialog.class, "onAttachedToWindow", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                logD("onAttachedToWindow");
                try {
                    final Dialog dialog = (Dialog) param.thisObject;
                    if (isInputMethod(dialog)) {
                        return;
                    }

                    IFlyingHandler handler = createFlyingHandler(dialog);
                    handler.registerReceiver();
                    handler.dealWithPersistentIn();
                    mActiveHandler = handler;
                } catch (Throwable t) {
                    logE(t);
                }
            }
        });

        /**
         * onWindowFocusChanged(true): when switch from other window directly to dialog window
         * onWindowFocusChanged(false): hit home button to exit
         */
        XposedHelpers.findAndHookMethod(Dialog.class, "onWindowFocusChanged", boolean.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    final Dialog dialog = (Dialog) param.thisObject;
                    if (isInputMethod(dialog)) {
                        return;
                    }
                    final boolean hasFocus = (Boolean) param.args[0];
                    logD(dialog + "#onWindowFocusChanged: hasFocus=" + hasFocus);
                    IFlyingHandler handler = createFlyingHandler(dialog);
                    if (hasFocus) {
                        handler.registerReceiver();
                        handler.dealWithPersistentIn();
                        mActiveHandler = handler;
                    } else {
                        handler.dealWithPersistentOut();
                        handler.unregisterReceiver();
                        mActiveHandler = null;
                    }
                } catch (Throwable t) {
                    logE(t);
                }
            }
        });

        /**
         * onDetachedFromWindow: hit back button to destroy dialog
         */
        XposedHelpers.findAndHookMethod(Dialog.class, "onDetachedFromWindow", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                logD("onDetachedFromWindow");
                try {
                    final Dialog dialog = (Dialog) param.thisObject;
                    if (isInputMethod(dialog)) {
                        return;
                    }
                    IFlyingHandler handler = createFlyingHandler(dialog);
                    handler.dealWithPersistentOut();
                    handler.unregisterReceiver();
                    mActiveHandler = null;
                } catch (Throwable t) {
                    logE(t);
                }
            }
        });
    }

    private static boolean isInputMethod(Dialog dialog) {
        return dialog.getClass().getName().equals(CLASS_SOFT_INPUT_WINDOW);
    }

    public static void onActivityResume(Activity activity) {
        mCurrentActivity = activity;
        if(mActiveHandler != null){
            mActiveHandler.registerReceiver();
            mActiveHandler.dealWithPersistentIn();
        }
    }

    public static void onActivityPause(){
        if (mActiveHandler != null) {
            mActiveHandler.registerReceiver();
            mActiveHandler.dealWithPersistentIn();
        }
    }
}
