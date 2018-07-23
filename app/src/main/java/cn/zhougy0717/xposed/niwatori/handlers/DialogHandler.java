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

    final private IFloatingWindowHandler createFloatingWindowHandler(Dialog dialog) {
        return (IFloatingWindowHandler) createFlyingHandler(dialog);
    }
    private static class CustomizedHandler extends FloatingWindowHandler {
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
            mHelper.setForeground(mDecorView);
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
            if (mHelper.getSettings().triggeringGesture) {
                return mEdgeGesture.onTouchEvent(event);
            }
            else {
                return false;
            }
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
                    Log.e("Ben", "onAttachedToWindow: " + dialog);
                    if (isInputMethod(dialog)) {
                        return;
                    }

                    IFloatingWindowHandler handler = createFloatingWindowHandler(dialog);
                    handler.setSwitchFromOutside(false);
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
                    Log.e("Ben", "onDetachedFromWindow: " + dialog);
                    if (isInputMethod(dialog)) {
                        return;
                    }
                    IFlyingHandler handler = createFlyingHandler(dialog);
                    handler.dealWithPersistentOut();
                    handler.unregisterReceiver();
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
                    Log.e("Ben", "onWindowFocusChanged(" + hasFocus + "): " + dialog);
                    logD(dialog + "#onWindowFocusChanged: hasFocus=" + hasFocus);
                    IFloatingWindowHandler handler = createFloatingWindowHandler(dialog);
                    if (hasFocus) {
                        handler.registerReceiver();
                        if (handler.isSwitchFromOutside()) {
                            handler.switchFromOutside();
                        }
                        else {
                            handler.switchFromActivity();
                            handler.setSwitchFromOutside(true);
                        }
                    } else {
                        handler.dealWithPersistentOut();
                        handler.unregisterReceiver();
                    }
                } catch (Throwable t) {
                    logE(t);
                }
            }
        });
    }

    public static void setActivity(Activity activity) {
        mCurrentActivity = activity;
    }

    private static boolean isInputMethod(Dialog dialog) {
        return dialog.getClass().getName().equals(CLASS_SOFT_INPUT_WINDOW);
    }
}
