package jp.tkgktyk.xposed.niwatori.app;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.common.collect.Sets;

import java.util.Set;

import jp.tkgktyk.flyinglayout.FlyingLayout;
import jp.tkgktyk.xposed.niwatori.InitialPosition;
import jp.tkgktyk.xposed.niwatori.NFW;
import jp.tkgktyk.xposed.niwatori.R;

/**
 * Created by tkgktyk on 2015/03/27.
 */
public class MyApp extends Application {
    private static final String TAG = MyApp.class.getSimpleName();

    @Override
    public void onCreate() {
        Log.d(TAG, "check version");
        // get last running version
        String keyVersionName = getString(R.string.key_version_name);
        MyVersion old = new MyVersion(NFW.getSharedPreferences(this).getString(keyVersionName, ""));
        // save current version
        MyVersion current = new MyVersion(this);

        if (current.isNewerThan(old)) {
            Log.d(TAG, "updated");
            onVersionUpdated(current, old);

            // reload preferences and put new version name
            final SharedPreferences prefs = NFW.getSharedPreferences(this);
            prefs.edit()
                    .putString(keyVersionName, current.toString())
                    .apply();
            // set default value for Change Settings Actions
            final String keyInitX = getString(R.string.key_initial_x_percent);
            if (!prefs.contains(keyInitX)) {
                prefs.edit()
                        .putInt(keyInitX, InitialPosition.DEFAULT_X_PERCENT)
                        .apply();
            }
            final String keyPivotX = getString(R.string.key_small_screen_pivot_x);
            if (!prefs.contains(keyPivotX)) {
                prefs.edit()
                        .putInt(keyPivotX, Math.round(FlyingLayout.DEFAULT_PIVOT_X * 100))
                        .apply();
            }
        }
        Log.d(TAG, "start application");

        super.onCreate();
    }

    protected void onVersionUpdated(MyVersion next, MyVersion old) {
        final SharedPreferences prefs = NFW.getSharedPreferences(this);
        if (old.isOlderThan("0.3.5")) {
            prefs.edit().clear().commit();
        }
        if (old.isOlderThan("0.3.8")) {
            // add initial value
            final String keyTargets = getString(R.string.key_another_resize_method_targets);
            Set<String> targets = prefs.getStringSet(keyTargets, Sets.<String>newHashSet());
            targets.add("com.android.chrome");
            prefs.edit()
                    .putStringSet(keyTargets, targets)
                    .commit();
        }
    }

    public class MyVersion {
        public static final int BASE = 1000;

        int major = 0;
        int minor = 0;
        int revision = 0;

        public MyVersion(String version) {
            set(version);
        }

        public MyVersion(Context context) {
            // set current package's version
            PackageManager pm = context.getPackageManager();
            String version = null;
            try {
                PackageInfo info = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_ACTIVITIES);
                version = info.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            if (version == null) {
                version = "0.0.0";
            }
            set(version);
        }

        public void set(String version) {
            if (TextUtils.isEmpty(version)) {
                return;
            }

            String[] v = version.split("\\.");
            int n = v.length;
            if (n >= 1) {
                major = Integer.parseInt(v[0]);
            }
            if (n >= 2) {
                minor = Integer.parseInt(v[1]);
            }
            if (n >= 3) {
                revision = Integer.parseInt(v[2]);
            }
        }

        public int toInt() {
            return major * BASE * BASE + minor * BASE + revision;
        }

        public boolean isNewerThan(MyVersion v) {
            return toInt() > v.toInt();
        }

        public boolean isNewerThan(String v) {
            return isNewerThan(new MyVersion(v));
        }

        public boolean isOlderThan(MyVersion v) {
            return toInt() < v.toInt();
        }

        public boolean isOlderThan(String v) {
            return isOlderThan(new MyVersion(v));
        }

        @Override
        public String toString() {
            return Integer.toString(major)
                    + "." + Integer.toString(minor)
                    + "." + Integer.toString(revision);
        }
    }
}
