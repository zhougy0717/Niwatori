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

public class WorldReadablePreference {
    private static XSharedPreferences mPrefs = null;
    private static File mPrefFile = null;
    private static File mPkgFolder = null;
    private static File mPrefFolder = null;
    private static FileObserver mFileObserver = null;

    public static void sharedPreferenceFix(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (mPrefFile != null) {
                mPkgFolder.setExecutable(true, false);
                mPkgFolder.setReadable(true, false);

                mPrefFolder.setExecutable(true, false);
                mPrefFolder.setReadable(true, false);

                mPrefFile.setReadable(true, false);

                if (mFileObserver == null) {
//                    mFileObserver = new FileObserver(mPrefFolder.getAbsolutePath(),
//                            FileObserver.ATTRIB) {
//                        @Override
//                        public void onEvent(int event, String path) {
//                            if ((event & FileObserver.ATTRIB) != 0) {
//                                sharedPreferenceFix();
//                                Log.e("Ben", "attrib changes");
//                            }
//                        }
//                    };
//                    mFileObserver.startWatching();
                }
            }
        }
    }

    public static NFW.Settings getSettings() {
        if (mPrefs == null) {
            mPrefs = new XSharedPreferences(NFW.PACKAGE_NAME);
            mPrefFile = mPrefs.getFile();
            mPrefFolder = new File(mPrefFile.getParent());
//            mPkgFolder = new File(mPrefFolder.getAbsolutePath() + "/../");
            mPkgFolder = new File("/data/data/" + NFW.PACKAGE_NAME);
        }
        sharedPreferenceFix();
        mPrefs.makeWorldReadable();
        mPrefs.reload();
        Log.e("Ben", "pref file can read: " + mPrefFile.canRead());
        Log.e("Ben", "pref: " + mPrefFile.getAbsolutePath() + ", pref folder:" + mPrefFolder.getAbsolutePath() + ", pkg folder:"+mPkgFolder.getAbsolutePath());
        return new NFW.Settings(mPrefs);
    }
}
