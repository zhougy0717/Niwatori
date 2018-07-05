package cn.zhougy0717.xposed.niwatori;

import android.app.Activity;
import android.app.Dialog;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by zhougua on 1/11/2018.
 */

public class DialogHandler extends XposedModule{
    private static final String CLASS_SOFT_INPUT_WINDOW = "android.inputmethodservice.SoftInputWindow";
//    private static final String FIELD_DIALOG_ACTION_RECEIVER = NFW.NAME + "_dialogActionReceiver";
    private static final String FIELD_FLYING_HELPER = NFW.NAME + "_flyingHelper";
    private static Activity mCurrentActivity = null;

    private static class FlyingHandler {
        private Dialog mDialog;
        private FlyingHelper mHelper;
        private FrameLayout mDecorView;
        private IReceiver mActionReceiver;
        private IReceiver mSettingsLoadedReceiver;

        private GestureDetector mEdgeGesture;

        public static FlyingHandler create(Dialog dialog) {
            FlyingHandler handler = (FlyingHandler) XposedHelpers.getAdditionalInstanceField(dialog, FIELD_FLYING_HELPER);
            if (handler == null) {
                handler = new FlyingHandler(dialog);
                XposedHelpers.setAdditionalInstanceField(dialog, FIELD_FLYING_HELPER, handler);
            }
            return handler;
        }
        private FlyingHandler(Dialog dialog){
            mDialog = dialog;
            mDecorView = (FrameLayout) dialog.getWindow().peekDecorView();
            mHelper = ModActivity.createFlyingHelper(mDecorView);
            mActionReceiver = ActionReceiver.getInstance(mDecorView, NFW.FOCUSED_DIALOG_FILTER);
            mSettingsLoadedReceiver = SettingsLoadReceiver.getInstance(mDecorView, NFW.SETTINGS_CHANGED_FILTER);

            mEdgeGesture = new GestureDetector(mDecorView.getContext(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onFling(MotionEvent event1, MotionEvent event2,float velocityX, float velocityY) {
                    if (!mHelper.edgeDetected(event1)) {
                        return false;
                    }
                    dragDown();
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

        private void dragDown() {
            // Show Dialog on Bottom Left/Right.
            Window win = mDialog.getWindow();
            WindowManager.LayoutParams params = win.getAttributes();
            boolean left = mHelper.getSettings().getSmallScreenPivotX() < 0.5;
            params.gravity = (left?Gravity.LEFT:Gravity.RIGHT )| Gravity.BOTTOM;
            win.setAttributes(params);
            mDialog.show();
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
                FlyingHandler handler = FlyingHandler.create(dialog);
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
//                    registerReceiver(dialog);
                    Log.e("Ben", "onAttachedToWindow " + dialog);

                    FlyingHandler handler = FlyingHandler.create(dialog);
                    handler.registerReceiver();
                    handler.dealWithPersistentIn();
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
                    FlyingHandler handler = FlyingHandler.create(dialog);
                    if (hasFocus) {
                        handler.registerReceiver();
                        handler.dealWithPersistentIn();
                    } else {
                        handler.dealWithPersistentOut();
                        handler.unregisterReceiver();
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
                    FlyingHandler handler = FlyingHandler.create(dialog);
                    handler.dealWithPersistentOut();
                    handler.unregisterReceiver();
                } catch (Throwable t) {
                    logE(t);
                }
            }
        });
    }

    private static boolean isInputMethod(Dialog dialog) {
        return dialog.getClass().getName().equals(CLASS_SOFT_INPUT_WINDOW);
    }

//    private static void registerReceiver(final Dialog dialog) {
//        final BroadcastReceiver receiver = (BroadcastReceiver) XposedHelpers
//                .getAdditionalInstanceField(dialog, FIELD_DIALOG_ACTION_RECEIVER);
//        if (receiver != null) {
//            // already registered
//            return;
//        }
//        final BroadcastReceiver actionReceiver = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                try {
//                    final String action = intent.getAction();
//                    logD("activity broadcast receiver: " + action);
//                    FlyingHelper helper = getHelper(dialog);
//                    if (helper == null) {
//                        logD("DecorView is null.");
//                        return;
//                    }
//                    final String packageName = dialog.getContext().getPackageName();
//                    if (helper.getSettings().blackList.contains(packageName)) {
//                        if (helper.getSettings().logActions) {
//                            log(dialog.toString() + "is ignored");
//                        }
//                        return;
//                    }
//
//                    // Show Dialog on Bottom Left/Right.
//                    Window win = dialog.getWindow();
//                    WindowManager.LayoutParams params = win.getAttributes();
//                    boolean left = helper.getSettings().getSmallScreenPivotX() < 0.5;
//                    params.gravity = (left?Gravity.LEFT:Gravity.RIGHT )| Gravity.BOTTOM;
//                    win.setAttributes(params);
//                    dialog.show();
//
//                    helper.performAction(action);
//                    abortBroadcast();
//                    if (helper.getSettings().logActions) {
//                        log(packageName + " consumed: " + action);
//                    }
//                } catch (Throwable t) {
//                    logE(t);
//                }
//            }
//        };
//        XposedHelpers.setAdditionalInstanceField(dialog,
//                FIELD_DIALOG_ACTION_RECEIVER, actionReceiver);
//        dialog.getContext().registerReceiver(actionReceiver, NFW.FOCUSED_DIALOG_FILTER);
//
//    }

//    private static void unregisterReceiver(Dialog dialog) {
//        final BroadcastReceiver actionReceiver = (BroadcastReceiver) XposedHelpers
//                .getAdditionalInstanceField(dialog, FIELD_DIALOG_ACTION_RECEIVER);
//        if (actionReceiver != null) {
//            dialog.getContext().unregisterReceiver(actionReceiver);
//            XposedHelpers.setAdditionalInstanceField(
//                    dialog, FIELD_DIALOG_ACTION_RECEIVER, null);
//        }
//    }

//    private static void resetAutomatically(Dialog dialog) {
//        final FlyingHelper helper = getHelper(dialog);
//        if (helper == null) {
//            logD("DecorView is null");
//            return;
//        }
////        if (helper.getSettings().autoReset) {
//        if(!helper.getSettings().smallScreenPersistent) {
//            // When fire actions from shortcut (ActionActivity), it causes onPause and onResume events
//            // because through an Activity. So shouldn't reset automatically.
//            helper.resetState(true);
//        }
//    }


//    @Nullable
//    private static FlyingHelper getHelper(@NonNull Dialog dialog) {
//        final FrameLayout decorView = (FrameLayout) dialog.getWindow().peekDecorView();
//        if (decorView == null) {
//            return null;
//        }
//        return ModActivity.getHelper(decorView);
//    }

    public static void setCurrentActivity(Activity activity) {
        mCurrentActivity = activity;
    }
}
