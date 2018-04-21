package cn.zhougy0717.xposed.niwatori;

import android.content.Context;
import android.content.Intent;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

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
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareForTest({XposedBridge.class})
public class NFWTest {
    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(XposedBridge.class);
        PowerMockito.doNothing().when(XposedBridge.class, "log", anyString());
    }

    @Test
    public void it_should_sendBroadcast_to_ChangeSettingsActionReceiver() throws Exception {
        Context context = mock(Context.class);
        when(context.getPackageName()).thenReturn(NFW.PACKAGE_NAME);
//        PowerMockito.mockStatic(NFW.class);
//        PowerMockito.when(NFW.class, "getNiwatoriContext", any(Context.class)).thenReturn(context);
        NFW.setResizedGlobal(context, true);
        ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(context).sendBroadcast(argumentCaptor.capture());
        assertTrue(argumentCaptor.getValue().getBooleanExtra("screen_resized", false));
    }
}