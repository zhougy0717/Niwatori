package cn.zhougy0717.xposed.niwatori;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.view.View;
import android.widget.FrameLayout;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import cn.zhougy0717.xposed.niwatori.app.ActionActivity;
import de.robv.android.xposed.XposedBridge;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(
        constants = BuildConfig.class,
        sdk = 26
)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*","javax.xml.*", "org.xml.sax.*", "org.w3c.dom.*",  "org.springframework.context.*", "org.apache.log4j.*" })
@PrepareForTest({XposedBridge.class, ReceiverManager.class, MyActivityLifecyckeCallbacks.class, ModActivity.class})
public class MyActivityLifecyckeCallbacksTest {
    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(XposedBridge.class);
        PowerMockito.doNothing().when(XposedBridge.class, "log", anyString());
    }

    @Test
    public void it_should_clear_out_the_temp_pref() throws Exception {
        PowerMockito.whenNew(ReceiverManager.class).withAnyArguments().thenReturn(PowerMockito.mock(ReceiverManager.class));
        MyActivityLifecyckeCallbacks callbacks = new MyActivityLifecyckeCallbacks();
        Activity activity = Robolectric.setupActivity(Activity.class);

        FlyingHelper mockHelper = mock(FlyingHelper.class);
        PowerMockito.mockStatic(ModActivity.class);
        when(ModActivity.class, "getHelper", any(FrameLayout.class)).thenReturn(mockHelper);
        callbacks.onActivityPaused(activity);
        verify(mockHelper).onExit();
    }
}