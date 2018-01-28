package cn.zhougy0717.xposed.niwatori;

import android.view.MotionEvent;
import android.view.View;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by zhougua on 1/8/2018.
 */

public class ModPhoneStatusBar_M extends ModPhoneStatusBar {
    protected String getPanelHolderName(){
        return "com.android.systemui.statusbar.phone.PanelHolder";
    }

    protected void hookPanelHolderOnTouch(ClassLoader classLoader){
        final Class<?> classPanelHolder = XposedHelpers.findClass(getPanelHolderName(), classLoader);
        XposedHelpers.findAndHookMethod(classPanelHolder, "onTouchEvent", MotionEvent.class,
                new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
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
        return "onAllPanelsCollapsed";
    }
}
