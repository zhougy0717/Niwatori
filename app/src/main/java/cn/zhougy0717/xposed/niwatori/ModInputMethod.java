package cn.zhougy0717.xposed.niwatori;

import android.content.Context;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by tkgktyk on 2015/02/12.
 */
public class ModInputMethod extends XposedModule {
    public static void initZygote(/*XSharedPreferences prefs*/) {
        try {
//            NFW.Settings settings = newSettings(prefs);
            Settings settings = WorldReadablePreference.getSettings();
            if (settings.autoReset) {
                final XC_MethodHook onSoftInputShown = new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            logD(param.method.getName());
                            final Context context = ((View) XposedHelpers.getObjectField(
                                    param.thisObject, "mCurRootView")).getContext();
                            NFW.performAction(context, NFW.ACTION_RESET);
                        } catch (Throwable t) {
                            logE(t);
                        }
                    }
                };
                // for general EditText
                XposedHelpers.findAndHookMethod(InputMethodManager.class,
                        "showSoftInput", View.class, int.class, ResultReceiver.class,
                        onSoftInputShown);
                // for SearchView (on ActionBar)
                XposedHelpers.findAndHookMethod(InputMethodManager.class,
                        "showSoftInputUnchecked", int.class, ResultReceiver.class,
                        onSoftInputShown);
                // for another case??? just to be sure
                XposedHelpers.findAndHookMethod(InputMethodManager.class,
                        "showSoftInputFromInputMethod", IBinder.class, int.class,
                        onSoftInputShown);
            }
        } catch (Throwable t) {
            logE(t);
        }
    }
}
