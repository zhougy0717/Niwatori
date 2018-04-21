package cn.zhougy0717.xposed.niwatori;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.FrameLayout;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import cn.zhougy0717.xposed.niwatori.FlyingHelper;
import cn.zhougy0717.xposed.niwatori.ModActivity;
import cn.zhougy0717.xposed.niwatori.NFW;
import cn.zhougy0717.xposed.niwatori.XposedModule;

/**
 * Created by zhougua on 1/11/2018.
 */

public class DialogHandler extends XposedModule{
    private static final String CLASS_SOFT_INPUT_WINDOW = "android.inputmethodservice.SoftInputWindow";
    private static final String FIELD_DIALOG_ACTION_RECEIVER = NFW.NAME + "_dialogActionReceiver";
    private static final String FIELD_FLYING_HELPER = NFW.NAME + "_flyingHelper";

    public void install() {
        //
        // register receiver
        //
        XposedHelpers.findAndHookMethod(Dialog.class, "onAttachedToWindow", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                logD("onAttachedToWindow");
                try {
                    final Dialog dialog = (Dialog) param.thisObject;
                    if (isInputMethod(dialog)) {
                        return;
                    }
                    registerReceiver(dialog);
                } catch (Throwable t) {
                    logE(t);
                }
            }
        });
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
                    if (hasFocus) {
                        registerReceiver(dialog);
                    } else {
                        unregisterReceiver(dialog);
                        resetAutomatically(dialog);
                    }
                } catch (Throwable t) {
                    logE(t);
                }
            }
        });
        XposedHelpers.findAndHookMethod(Dialog.class, "onDetachedFromWindow", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                logD("onDetachedFromWindow");
                try {
                    final Dialog dialog = (Dialog) param.thisObject;
                    if (isInputMethod(dialog)) {
                        return;
                    }
                    unregisterReceiver(dialog);
                } catch (Throwable t) {
                    logE(t);
                }
            }
        });
    }

    private static boolean isInputMethod(Dialog dialog) {
        return dialog.getClass().getName().equals(CLASS_SOFT_INPUT_WINDOW);
    }

    private static void registerReceiver(final Dialog dialog) {
        final BroadcastReceiver receiver = (BroadcastReceiver) XposedHelpers
                .getAdditionalInstanceField(dialog, FIELD_DIALOG_ACTION_RECEIVER);
        if (receiver != null) {
            // already registered
            return;
        }
        final BroadcastReceiver actionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    final String action = intent.getAction();
                    logD("activity broadcast receiver: " + action);
                    FlyingHelper helper = getHelper(dialog);
                    if (helper == null) {
                        logD("DecorView is null.");
                        return;
                    }
                    final String packageName = dialog.getContext().getPackageName();
                    if (helper.getSettings().blackList.contains(packageName)) {
                        if (helper.getSettings().logActions) {
                            log(dialog.toString() + "is ignored");
                        }
                        return;
                    }
                    helper.performAction(action);
                    abortBroadcast();
                    if (helper.getSettings().logActions) {
                        log(packageName + " consumed: " + action);
                    }
                } catch (Throwable t) {
                    logE(t);
                }
            }
        };
        XposedHelpers.setAdditionalInstanceField(dialog,
                FIELD_DIALOG_ACTION_RECEIVER, actionReceiver);
        dialog.getContext().registerReceiver(actionReceiver, NFW.FOCUSED_DIALOG_FILTER);

    }

    private static void unregisterReceiver(Dialog dialog) {
        final BroadcastReceiver actionReceiver = (BroadcastReceiver) XposedHelpers
                .getAdditionalInstanceField(dialog, FIELD_DIALOG_ACTION_RECEIVER);
        if (actionReceiver != null) {
            dialog.getContext().unregisterReceiver(actionReceiver);
            XposedHelpers.setAdditionalInstanceField(
                    dialog, FIELD_DIALOG_ACTION_RECEIVER, null);
        }
    }

    private static void resetAutomatically(Dialog dialog) {
        final FlyingHelper helper = getHelper(dialog);
        if (helper == null) {
            logD("DecorView is null");
            return;
        }
//        if (helper.getSettings().autoReset) {
        if(!helper.getSettings().smallScreenPersistent) {
            // When fire actions from shortcut (ActionActivity), it causes onPause and onResume events
            // because through an Activity. So shouldn't reset automatically.
            helper.resetState(true);
        }
    }


    @Nullable
    private static FlyingHelper getHelper(@NonNull Dialog dialog) {
        final FrameLayout decorView = getDecorView(dialog);
        if (decorView == null) {
            return null;
        }
        return ModActivity.getHelper(decorView);
    }

//    private static FlyingHelper getHelper(@NonNull FrameLayout decorView) {
//        return (FlyingHelper) XposedHelpers.getAdditionalInstanceField(
//                decorView, FIELD_FLYING_HELPER);
//    }

    @Nullable
    private static FrameLayout getDecorView(@NonNull Dialog dialog) {
        return (FrameLayout) dialog.getWindow().peekDecorView();
    }
}
