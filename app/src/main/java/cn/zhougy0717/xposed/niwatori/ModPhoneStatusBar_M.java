package cn.zhougy0717.xposed.niwatori;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by zhougua on 1/8/2018.
 */

public class ModPhoneStatusBar_M extends ModPhoneStatusBar {
    protected String getPanelHolderName(){
        return "com.android.systemui.statusbar.phone.PanelHolder";
    }

    protected void hookPanelHolderOnTouch(ClassLoader classLoader){
//        final Class<?> classPanelHolder = XposedHelpers.findClass(getPanelHolderName(), classLoader);
//        XposedHelpers.findAndHookMethod(classPanelHolder, "onTouchEvent", MotionEvent.class,
//                new XC_MethodReplacement() {
//                    @Override
//                    protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
//                        try {
//                            final MotionEvent event = (MotionEvent) methodHookParam.args[0];
//                            if (mHelper.onTouchEvent(event)) {
//                                return true;
//                            }
//                        } catch (Throwable t) {
//                            logE(t);
//                        }
//                        return invokeOriginalMethod(methodHookParam);
//                    }
//                });
    }

    protected String getPanelCollapsedName() {
        return "onAllPanelsCollapsed";
    }

    protected void hookPanelConstructor(ClassLoader classLoader) {
        final Class<?> classPanelHolder = XposedHelpers.findClass(getPanelHolderName(), classLoader);
        XposedBridge.hookAllConstructors(classPanelHolder, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                try {
                    final FrameLayout panelHolder = (FrameLayout) param.thisObject;

                    // need to reload on each package?
                    mHelper = new FlyingHelper(panelHolder, 1, false);
                    XposedHelpers.setAdditionalInstanceField(panelHolder, FIELD_FLYING_HELPER, mHelper);

                    panelHolder.getContext().registerReceiver(mGlobalReceiver, NFW.STATUS_BAR_FILTER);
                    panelHolder.getContext().registerReceiver(new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            logD("reload settings");
                            // need to reload on each package?
                            Settings settings = (Settings) intent.getSerializableExtra(NFW.EXTRA_SETTINGS);
                            mHelper.onSettingsLoaded(settings);
                        }
                    }, NFW.SETTINGS_CHANGED_FILTER);
                    log("attached to status bar");

                } catch (Throwable t) {
                    logE(t);
                }
            }
        });
    }

    @Override
    protected void hookPanelHolderDraw(ClassLoader classLoader){
        XposedHelpers.findAndHookMethod(View.class, "draw", Canvas.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            View v = (View) param.thisObject;
                            if (v.getClass().getName().endsWith("PanelHolder")) {
                                final Canvas canvas = (Canvas) param.args[0];
                                mHelper.draw(canvas);
                            }
                        } catch (Throwable t) {
                            logE(t);
                        }
                    }
                });
    }
}
