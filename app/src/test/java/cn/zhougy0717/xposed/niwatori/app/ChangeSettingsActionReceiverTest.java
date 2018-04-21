package cn.zhougy0717.xposed.niwatori.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import cn.zhougy0717.xposed.niwatori.BuildConfig;
import cn.zhougy0717.xposed.niwatori.NFW;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(
        constants = BuildConfig.class,
        sdk = 26
)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*","javax.xml.*", "org.xml.sax.*", "org.w3c.dom.*",  "org.springframework.context.*", "org.apache.log4j.*" })
@PrepareForTest({NFW.class})
public class ChangeSettingsActionReceiverTest {
    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Test
    public void it_should_update_global_pref_with_intent_extras() throws Exception {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application);
        PowerMockito.mockStatic(NFW.class);
        PowerMockito.when(NFW.class, "getSharedPreferences", any(Context.class))
                .thenReturn(prefs);
        Context context = mock(Context.class);
        Intent intent = new Intent();
        intent.putExtra("key_small_screen_pivot_x", 1);
        intent.putExtra("screen_resized", true);

        ChangeSettingsActionReceiver receiver = new ChangeSettingsActionReceiver();
        receiver.onReceive(context, intent);
        assertEquals(1, prefs.getInt("key_small_screen_pivot_x", 0));
        assertTrue(prefs.getBoolean("screen_resized", false));
    }
}