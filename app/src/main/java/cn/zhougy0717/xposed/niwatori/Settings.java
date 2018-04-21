package cn.zhougy0717.xposed.niwatori;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import jp.tkgktyk.flyinglayout.FlyingLayout;

/**
 * Created by zhougua on 4/17/2018.
 */
public class Settings implements Serializable {
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

    public boolean screenResized;

    public Settings(SharedPreferences prefs) {
        load(prefs);
    }

    public void load(SharedPreferences prefs) {
        blackList = prefs.getStringSet("key_blacklist", Collections.<String>emptySet());
        animation = prefs.getBoolean("key_animation", true);
        autoReset = prefs.getBoolean("key_auto_reset", false);
        extraAction = prefs.getString("key_extra_action", NFW.ACTION_MOVABLE_SCREEN);
        actionWhenTapOutside = prefs.getString("key_action_when_tap_outside", NFW.ACTION_SOFT_RESET);
        actionWhenDoubleTapOutside = prefs.getString("key_action_when_double_tap_outside", NFW.ACTION_PIN);

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

        screenResized = prefs.getBoolean("screen_resized", false);

    }
}
