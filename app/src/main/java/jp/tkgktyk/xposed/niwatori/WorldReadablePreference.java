package jp.tkgktyk.xposed.niwatori;

import android.content.Context;
import android.os.Build;

import java.io.File;

import de.robv.android.xposed.XSharedPreferences;

/**
 * Created by zhougua on 1/17/2018.
 */

public class WorldReadablePreference {
    private static XSharedPreferences mPrefs = null;

    public static void sharedPreferenceFix(Context context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            File pkgFolder = new File(context.getDataDir().getAbsolutePath());
            pkgFolder.setExecutable(true, false);
        }
    }

    public static NFW.Settings getSettings() {
        if (mPrefs == null) {
            mPrefs = new XSharedPreferences(NFW.PACKAGE_NAME);
        }
        mPrefs.makeWorldReadable();
        mPrefs.reload();
        return new NFW.Settings(mPrefs);
    }
}
