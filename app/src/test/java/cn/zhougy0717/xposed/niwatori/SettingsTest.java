package cn.zhougy0717.xposed.niwatori;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import de.robv.android.xposed.XposedBridge;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(
        constants = BuildConfig.class,
        sdk = 26
)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareForTest({FlyingHelper.class, WorldReadablePreference.class, XposedBridge.class, NFW.class})
public class SettingsTest {
    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Test
    public void it_should_update_specified_items_in_the_settings() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application);
        Settings settings = new Settings(prefs);
        prefs.edit()
                .putInt("key_small_screen_pivot_x", 12)
                .putInt("key_small_screen_pivot_y", 34)
                .putInt("key_small_screen_size", 56)
                .putInt("key_initial_x_percent", 78)
                .putInt("key_initial_y_percent", 90)
                .apply();
        settings.update(prefs);
        assertEquals(0.12f, settings.getSmallScreenPivotX(), 0.0);
        assertEquals(0.34f, settings.getSmallScreenPivotY(), 0.0);
        assertEquals(0.56f, settings.getSmallScreenSize(), 0.0);
//        assertEquals(78, settings.initialXp);
//        assertEquals(90, settings.initialYp);
    }
}