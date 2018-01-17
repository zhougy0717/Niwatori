package jp.tkgktyk.xposed.niwatori;

import de.robv.android.xposed.XSharedPreferences;

/**
 * Created by zhougua on 1/17/2018.
 */

public class PreferenceManager {
    private static XSharedPreferences mPrefs = null;

    public static NFW.Settings getSettings() {
        if (mPrefs == null) {
            mPrefs = new XSharedPreferences(NFW.PACKAGE_NAME);;
        }
        mPrefs.makeWorldReadable();
        mPrefs.reload();
        return new NFW.Settings(mPrefs);
    }
}
