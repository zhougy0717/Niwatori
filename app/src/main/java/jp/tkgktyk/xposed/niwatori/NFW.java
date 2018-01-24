package jp.tkgktyk.xposed.niwatori;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.FrameLayout;

import com.google.common.base.Strings;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import jp.tkgktyk.flyinglayout.FlyingLayout;
import jp.tkgktyk.xposed.niwatori.app.PersistentService;

/**
 * Created by tkgktyk on 2015/02/12.
 * Niwatori - Fly the Window
 */
public class NFW {
    private static final String TAG = NFW.class.getSimpleName();

    public static final String PACKAGE_NAME = NFW.class.getPackage().getName();
    public static final String NAME = NFW.class.getSimpleName();
    public static final String PREFIX_ACTION = PACKAGE_NAME + ".intent.action.";
    public static final String PREFIX_EXTRA = PACKAGE_NAME + ".intent.extra.";

    public static final String ACTION_NONE = "";
    public static final String ACTION_MOVABLE_SCREEN = PREFIX_ACTION + "MOVABLE_SCREEN";
    public static final String ACTION_PIN = PREFIX_ACTION + "PIN";
    public static final String ACTION_PIN_OR_RESET = PREFIX_ACTION + "PIN_OR_RESET";
    public static final String ACTION_SMALL_SCREEN = PREFIX_ACTION + "SMALL_SCREEN";
    public static final String ACTION_FORCE_SMALL_SCREEN = PREFIX_ACTION + "FORCE_SMALL_SCREEN";
    public static final String ACTION_EXTRA_ACTION = PREFIX_ACTION + "EXTRA_ACTION";
    public static final String ACTION_RESET = PREFIX_ACTION + "RESET";
    public static final String ACTION_SOFT_RESET = PREFIX_ACTION + "SOFT_RESET";

    public static final String PREFIX_ACTION_SB = PREFIX_ACTION + "SB_";
    public static final String ACTION_SB_EXPAND_NOTIFICATIONS = PREFIX_ACTION_SB + "EXPAND_NOTIFICATIONS";
    public static final String ACTION_SB_EXPAND_QUICK_SETTINGS = PREFIX_ACTION_SB + "EXPAND_QUICK_SETTINGS";

    public static final String ACTION_CS_SWAP_LEFT_RIGHT = PREFIX_ACTION + "CS_SWAP_LEFT_RIGHT";

    public static final String ACTION_SETTINGS_CHANGED = PREFIX_ACTION + "SETTINGS_CHANGED";
    public static final String EXTRA_SETTINGS = PREFIX_EXTRA + "SETTINGS";

    /**
     * Static IntentFilters
     */
    public static final IntentFilter STATUS_BAR_FILTER;
    public static final IntentFilter FOCUSED_DIALOG_FILTER;
    public static final IntentFilter FOCUSED_ACTIVITY_FILTER;
    public static final IntentFilter ACTIVITY_FILTER;
    public static final IntentFilter SETTINGS_CHANGED_FILTER = new IntentFilter(ACTION_SETTINGS_CHANGED);
    /**
     * Receivers are set priority.
     * 1. Status bar
     * 2. Focused Dialog
     * 3. Focused Activity
     * 4. Activity
     * *. Unfoused Dialog <- unregistered
     */
    private static final int PRIORITY_STATUS_BAR = IntentFilter.SYSTEM_HIGH_PRIORITY;
    private static final int PRIORITY_FOCUSED_DIALOG = IntentFilter.SYSTEM_HIGH_PRIORITY / 10;
    private static final int PRIORITY_FOCUSED_ACTIVITY = IntentFilter.SYSTEM_HIGH_PRIORITY / 100;
    private static final int PRIORITY_ACTIVITY = IntentFilter.SYSTEM_HIGH_PRIORITY / 1000;

    /**
     * IntentFilters initialization
     */
    static {
        STATUS_BAR_FILTER = new IntentFilter();
        STATUS_BAR_FILTER.addAction(NFW.ACTION_MOVABLE_SCREEN);
        STATUS_BAR_FILTER.addAction(NFW.ACTION_PIN);
        STATUS_BAR_FILTER.addAction(NFW.ACTION_PIN_OR_RESET);
        STATUS_BAR_FILTER.addAction(NFW.ACTION_SMALL_SCREEN);
        STATUS_BAR_FILTER.addAction(NFW.ACTION_FORCE_SMALL_SCREEN);
        STATUS_BAR_FILTER.addAction(NFW.ACTION_EXTRA_ACTION);
        STATUS_BAR_FILTER.addAction(NFW.ACTION_RESET);
        STATUS_BAR_FILTER.addAction(NFW.ACTION_SOFT_RESET);
        FOCUSED_DIALOG_FILTER = new IntentFilter(STATUS_BAR_FILTER);
        FOCUSED_ACTIVITY_FILTER = new IntentFilter(STATUS_BAR_FILTER);
        ACTIVITY_FILTER = new IntentFilter(STATUS_BAR_FILTER);
        // Exclusive
        STATUS_BAR_FILTER.addAction((NFW.ACTION_SB_EXPAND_NOTIFICATIONS));
        STATUS_BAR_FILTER.addAction((NFW.ACTION_SB_EXPAND_QUICK_SETTINGS));
        // Priority
        STATUS_BAR_FILTER.setPriority(NFW.PRIORITY_STATUS_BAR);
        FOCUSED_DIALOG_FILTER.setPriority(NFW.PRIORITY_FOCUSED_DIALOG);
        FOCUSED_ACTIVITY_FILTER.setPriority(NFW.PRIORITY_FOCUSED_ACTIVITY);
        ACTIVITY_FILTER.setPriority(NFW.PRIORITY_ACTIVITY);
    }

    public static final int NONE_ON_RECENTS = 0;
    public static final int TAP_ON_RECENTS = 1;
    public static final int DOUBLE_TAP_ON_RECENTS = 2;
    public static final int LONG_PRESS_ON_RECENTS = 3;


    public static SharedPreferences getSharedPreferences(Context context) {
        WorldReadablePreference.sharedPreferenceFix();
        SharedPreferences prefs = context.getSharedPreferences(PACKAGE_NAME + "_preferences", Context.MODE_PRIVATE);
        return prefs;
    }

    public static void sendSettingsChanged(Context context, SharedPreferences prefs) {
        Log.d(TAG, "send settings changed");
        NFW.Settings settings = new NFW.Settings(prefs);
        Intent intent = new Intent(NFW.ACTION_SETTINGS_CHANGED);
        intent.putExtra(NFW.EXTRA_SETTINGS, settings);
        context.sendBroadcast(intent);
    }

    public static void performAction(@NonNull Context context, @Nullable String action) {
        if (!Strings.isNullOrEmpty(action)) {
            context.sendOrderedBroadcast(new Intent(action), null);
        }
    }

    public static boolean isDefaultAction(@Nullable String action) {
        return Strings.isNullOrEmpty(action);
    }

    public static Context getNiwatoriContext(Context context) {
        Context niwatoriContext = null;
        try {
            if (context.getPackageName().equals(NFW.PACKAGE_NAME)) {
                niwatoriContext = context;
            } else {
                niwatoriContext = context.createPackageContext(
                        NFW.PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY);
            }
        } catch (Throwable t) {
            XposedModule.logE(t);
        }
        return niwatoriContext;
    }

    public static GradientDrawable makeBoundaryDrawable(int width, int color) {
        final GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.TRANSPARENT);
        drawable.setStroke(width, color);
        return drawable;
    }

    public static void requestResizedGlobal(Context context) {
        context.sendBroadcast(new Intent(PersistentService.ACTION_REQUEST_RESIZE));
    }

    public static void requestResizedGlobal(FrameLayout decorView) {
        decorView.getContext().sendBroadcast(new Intent(PersistentService.ACTION_REQUEST_RESIZE));
    }

    public static void setResizedGlobal(Context context, boolean resized) {
        context.sendBroadcast(new Intent(resized ?
                PersistentService.ACTION_SET_RESIZED : PersistentService.ACTION_UNSET_RESIZED));
    }

    public static class Settings implements Serializable {
        static final long serialVersionUID = 1L;

        public Set<String> blackList;
        public boolean animation;
        public boolean autoReset;
        public String extraAction;
        public String actionWhenTapOutside;
        public String actionWhenDoubleTapOutside;

        public float speed;
        public boolean autoPin;
        public int boundaryColorMS;
        public int initialXp;
        public int initialYp;

        public int boundaryColorSS;
        public float smallScreenSize;
        public float smallScreenPivotX;
        public float smallScreenPivotY;
        public Set<String> anotherResizeMethodTargets;
        public boolean smallScreenPersistent;

        public boolean logActions;

        public int extraActionOnRecents;

        public Settings(SharedPreferences prefs) {
            load(prefs);
        }

        public void load(SharedPreferences prefs) {
            blackList = prefs.getStringSet("key_blacklist", Collections.<String>emptySet());
            animation = prefs.getBoolean("key_animation", true);
            autoReset = prefs.getBoolean("key_auto_reset", false);
            extraAction = prefs.getString("key_extra_action", ACTION_MOVABLE_SCREEN);
            actionWhenTapOutside = prefs.getString("key_action_when_tap_outside", ACTION_SOFT_RESET);
            actionWhenDoubleTapOutside = prefs.getString("key_action_when_double_tap_outside", ACTION_PIN);

            speed = Float.parseFloat(prefs.getString("key_speed", Float.toString(FlyingLayout.DEFAULT_SPEED)));
            autoPin = prefs.getBoolean("key_auto_pin", false);
            boundaryColorMS = Color.parseColor(prefs.getString("key_boundary_color_ms", "#689F38")); // default is Light Green
            initialXp = prefs.getInt("key_initial_x_percent", InitialPosition.DEFAULT_X_PERCENT);
            initialYp = prefs.getInt("key_initial_y_percent", InitialPosition.DEFAULT_Y_PERCENT);

            boundaryColorSS = Color.parseColor(prefs.getString("key_boundary_color_ss", "#00000000")); // default is Transparent
            smallScreenSize = prefs.getInt("key_small_screen_size", 70) / 100f;
            smallScreenPivotX = prefs.getInt("key_small_screen_pivot_x",
                    Math.round(FlyingLayout.DEFAULT_PIVOT_X * 100)) / 100f;
            smallScreenPivotY = prefs.getInt("key_small_screen_pivot_y",
                    Math.round(FlyingLayout.DEFAULT_PIVOT_Y * 100)) / 100f;
            anotherResizeMethodTargets = prefs.getStringSet("key_another_resize_method_targets",
                    Collections.<String>emptySet());
            smallScreenPersistent = prefs.getBoolean("key_small_screen_persistent", false);

            extraActionOnRecents = Integer.parseInt(
                    prefs.getString("key_extra_action_on_recents", "0"));

            logActions = prefs.getBoolean("key_log_actions", false) || BuildConfig.DEBUG;
        }
    }
}
