package cn.zhougy0717.xposed.niwatori;

import android.os.Build;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by tkgktyk on 2015/02/12.
 */
public class Niwatori implements IXposedHookZygoteInit, IXposedHookLoadPackage {
    private XSharedPreferences mPrefs;
    private ModPhoneStatusBar mStatusBar;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        XposedModule.logD("SDK: " + Build.VERSION.SDK_INT);

        ModActivity.initZygote();
//        ModInputMethod.initZygote();
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        (new ModPhoneStatusBar()).handleLoadPackage(loadPackageParam);
    }
}
