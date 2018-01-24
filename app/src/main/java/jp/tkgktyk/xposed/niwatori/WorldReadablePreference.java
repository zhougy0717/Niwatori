package jp.tkgktyk.xposed.niwatori;

import android.content.Context;
import android.os.Build;
import android.os.FileObserver;
import android.util.Log;

import java.io.File;

import de.robv.android.xposed.XSharedPreferences;

/**
 * Created by zhougua on 1/17/2018.
 */

public class WorldReadablePreference extends XposedModule{
    private static XSharedPreferences mPrefs = null;
    private static final String mPrefFile = "/data/data/" + NFW.PACKAGE_NAME + "/shared_prefs/" + NFW.PACKAGE_NAME + "_preferences.xml";
    private static final String mPrefFolder = "/data/data/" + NFW.PACKAGE_NAME + "/shared_prefs/";
    private static final String mPkgFolder = "/data/data/" + NFW.PACKAGE_NAME;

    private static FileObserver mFileObserver = null;

    private static void permissionFix() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            (new File(mPkgFolder)).setExecutable(true, false);
            (new File(mPkgFolder)).setReadable(true, false);

            (new File(mPrefFolder)).setExecutable(true, false);
            (new File(mPrefFolder)).setReadable(true, false);

            (new File(mPrefFile)).setReadable(true, false);
        }
    }

    private static void createPrefObserver() {
        if (mFileObserver != null) {
            return;
        }
        mFileObserver = new FileObserver(mPrefFolder,
                FileObserver.ATTRIB) {
            @Override
            public void onEvent(int event, String path) {
                if ((event & FileObserver.ATTRIB) != 0) {
                    permissionFix();
                }
            }
        };
        mFileObserver.startWatching();
    }

    public static void sharedPreferenceFix() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            permissionFix();
        }
        createPrefObserver();
    }

    public static NFW.Settings getSettings() {
        if (mPrefs == null) {
            mPrefs = new XSharedPreferences(NFW.PACKAGE_NAME);
        }
        permissionFix();
        mPrefs.makeWorldReadable();
        mPrefs.reload();
//        log("pref exists: " + mPrefs.getFile().exists() + ", can read: " +mPrefs.getFile().canRead());
        return new NFW.Settings(mPrefs);
    }
}
