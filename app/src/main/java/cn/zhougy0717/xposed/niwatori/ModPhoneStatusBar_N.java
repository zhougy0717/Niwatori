package cn.zhougy0717.xposed.niwatori;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by zhougua on 1/8/2018.
 */

public class ModPhoneStatusBar_N extends ModPhoneStatusBar {
    protected String getPanelHolderName(){
        return "com.android.systemui.statusbar.phone.NotificationsQuickSettingsContainer";
    }

    @Override
    protected void expandQuickSettings(){
        Object mNotifPanel = XposedHelpers.getObjectField(mPhoneStatusBar, "mNotificationPanel");
        try {
            XposedHelpers.callMethod(mNotifPanel, "instantExpand");
            if (XposedHelpers.getBooleanField(mNotifPanel, "mQsExpansionEnabled")) {
                XposedHelpers.callMethod(mNotifPanel, "setQsExpansion",
                        XposedHelpers.getIntField(mNotifPanel, "mQsMaxExpansionHeight"));
            }
        }
        catch (Throwable t) {
            XposedHelpers.callMethod(mNotifPanel, "expandWithQs");
        }
    }

    @Override
    protected void expandNotificationBar(){
        Object mNotifPanel = XposedHelpers.getObjectField(mPhoneStatusBar, "mNotificationPanel");
        try {
            XposedHelpers.callMethod(mNotifPanel, "instantExpand");
        } catch (Throwable t) {
            XposedHelpers.callMethod(mNotifPanel, "expand", true);
        }
    }
    protected void hookPanelHolderOnTouch(ClassLoader classLoader){
        XposedHelpers.findAndHookMethod(View.class, "onTouchEvent", MotionEvent.class,
            new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    if (!methodHookParam.thisObject
                            .getClass()
                            .getName()
                            .equals(getPanelHolderName())) {
                        return invokeOriginalMethod(methodHookParam);
                    }
                    try {
                        final MotionEvent event = (MotionEvent) methodHookParam.args[0];
                        if (mHelper.onTouchEvent(event)) {
                            return true;
                        }
                    } catch (Throwable t) {
                        logE(t);
                    }
                    return invokeOriginalMethod(methodHookParam);
                }
            });
    }

    protected String getPanelCollapsedName() {
        return "onPanelCollapsed";
    }
}
