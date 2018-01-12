package jp.tkgktyk.xposed.niwatori.app;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.PopupWindow;

import java.text.DecimalFormat;
import java.util.List;

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
    private static final String FIELD_DECOR_VIEW = NFW.NAME + "_decorView";
    private static final String FIELD_DIALOG_ACTION_RECEIVER = NFW.NAME + "_dialogActionReceiver";
    private static final String FIELD_FLYING_HELPER = NFW.NAME + "_flyingHelper";

    private class Handler{
        private PopupWindow mPopupWindow;
        private FrameLayout mDecorView;
        private BroadcastReceiver mReceiver;
        private FlyingHelper mHelper;
        public Handler(PopupWindow pw){
            mPopupWindow = pw;

//            mDecorView = (FrameLayout) XposedHelpers.getObjectField(pw, "mBackgroundView");
            mDecorView = (FrameLayout) XposedHelpers.getObjectField(pw, "mDecorView");

            mHelper = ModActivity.createFlyingHelper(mDecorView, 1);
            mReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    try {
                        final String action = intent.getAction();
                        logD("activity broadcast receiver: " + action);

                        final String packageName = mDecorView.getContext().getPackageName();
                        if (mHelper.getSettings().blackList.contains(packageName)) {
                            if (mHelper.getSettings().logActions) {
                                log(mDecorView.toString() + "is ignored");
                            }
                            return;
                        }
                        mHelper.performAction(action);
                        abortBroadcast();
                        if (mHelper.getSettings().logActions) {
                            log(packageName + " consumed: " + action);
                        }
                    } catch (Throwable t) {
                        logE(t);
                    }
                }
            };
        }

        public void registerReceiver(){
            final BroadcastReceiver existingReceiver = (BroadcastReceiver) XposedHelpers
                    .getAdditionalInstanceField(mDecorView, FIELD_DIALOG_ACTION_RECEIVER);
            if (existingReceiver != null) {
                // already registered
                return;
            }

            XposedHelpers.setAdditionalInstanceField(mDecorView, FIELD_DIALOG_ACTION_RECEIVER, mReceiver);
            mDecorView.getContext().registerReceiver(mReceiver, NFW.FOCUSED_DIALOG_FILTER);
        }

        public void unregisterReceiver(){
            final BroadcastReceiver actionReceiver = (BroadcastReceiver) XposedHelpers
                .getAdditionalInstanceField(mDecorView, FIELD_DIALOG_ACTION_RECEIVER);
            if (actionReceiver != null) {
                mDecorView.getContext().unregisterReceiver(mReceiver);
                mHelper.resetState(true);
                XposedHelpers.setAdditionalInstanceField(
                        mDecorView, FIELD_DIALOG_ACTION_RECEIVER, null);
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
//                ViewGroup decor = (ViewGroup) XposedBridge.invokeOriginalMethod(param.method,
//                        param.thisObject, param.args);
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
                    final Handler h = new Handler(pw);
                    h.registerReceiver();
                    final PopupWindow.OnDismissListener dismissListener =
                            (PopupWindow.OnDismissListener) XposedHelpers.getObjectField(pw, "mOnDismissListener");
                    pw.setOnDismissListener(new PopupWindow.OnDismissListener() {
                        @Override
                        public void onDismiss() {
                            h.unregisterReceiver();
                            dismissListener.onDismiss();
                        }
                    });
                } catch (Throwable t) {
                    logE(t);
                }

//                return decor;
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

//    @Nullable
//    private static FrameLayout getDecorView(PopupWindow pw) {
//        return (FrameLayout) XposedHelpers.getAdditionalInstanceField(
//                pw, FIELD_DECOR_VIEW);
//    }
}
