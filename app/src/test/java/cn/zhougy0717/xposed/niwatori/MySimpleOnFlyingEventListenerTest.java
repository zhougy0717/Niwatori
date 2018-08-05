package cn.zhougy0717.xposed.niwatori;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.FrameLayout;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Set;

import jp.tkgktyk.flyinglayout.FlyingLayout;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(
        constants = BuildConfig.class,
        sdk = 26
)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareForTest({WorldReadablePreference.class, NFW.class})
public class MySimpleOnFlyingEventListenerTest {
    @Rule
    public PowerMockRule rule = new PowerMockRule();

    private SharedPreferences globalPrefs;
    private SharedPreferences localPrefs;

    @Before
    public void setUp() throws Exception {
        globalPrefs = PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application);
    }
    @After
    public void tearDown(){
        globalPrefs.edit().clear().apply();
        if (localPrefs != null){
            localPrefs.edit().clear().apply();
        }
    }
    @Test
    public void it_should_call_onSettingsLoad_performAction_in_order() throws Exception {
        FlyingHelper helper = mock(FlyingHelper.class);
        when(helper.getSettings()).thenReturn(mock(Settings.class));
        when(helper.getScreenData()).thenReturn(new Settings.ScreenData(new Settings(globalPrefs)));
        FlyingLayout.OnFlyingEventListener listener = new MySimpleOnFlyingEventListener(helper);

        FrameLayout v = new FrameLayout(RuntimeEnvironment.application);
        listener.onShrink(v);

        verify(helper).onSettingsLoaded();
    }

    @Test
    public void it_should_load_local_prefs_for_smallScreenSize_while_onScroll() throws Exception {
        FlyingHelper helper = mock(FlyingHelper.class);
        Settings settings = new Settings(globalPrefs);
        Settings.ScreenData data = new Settings.ScreenData(settings);
        data.smallScreenSize = 0.6f;
        when(helper.getSettings()).thenReturn(settings.update(data));

        when(helper.getScreenData()).thenReturn(data);
        FrameLayout v = new FrameLayout(RuntimeEnvironment.application);
        FlyingLayout.OnFlyingEventListener listener = new MySimpleOnFlyingEventListener(helper);
        listener.onShrink(v);
        ArgumentCaptor<Settings.ScreenData> captor = ArgumentCaptor.forClass(Settings.ScreenData.class);
        verify(helper).setScreenData(captor.capture());
        assertEquals(Math.round(60 - 100*FlyingHelper.SMALL_SCREEN_SIZE_DELTA), (int)(100*captor.getValue().smallScreenSize));
    }

    @Test
    public void it_should_load_global_prefs_if_no_local_one() throws Exception {
        globalPrefs.edit().putInt("key_small_screen_size", 60).apply();

        FlyingHelper helper = mock(FlyingHelper.class);
        Settings settings = new Settings(globalPrefs);
        when(helper.getSettings()).thenReturn(settings);
        when(helper.getScreenData()).thenReturn(new Settings.ScreenData(settings));

        FrameLayout v = new FrameLayout(RuntimeEnvironment.application);

        FlyingLayout.OnFlyingEventListener listener = new MySimpleOnFlyingEventListener(helper);
        listener.onShrink(v);
        ArgumentCaptor<Settings.ScreenData> captor = ArgumentCaptor.forClass(Settings.ScreenData.class);
        verify(helper).setScreenData(captor.capture());
        assertEquals(Math.round(60 - 100*FlyingHelper.SMALL_SCREEN_SIZE_DELTA), (int)(100*captor.getValue().smallScreenSize));
    }

//    @Test
//    public void it_should_save_smallScreenSize_to_local_SharedPreference() throws Exception {
//        localPrefs = RuntimeEnvironment.application.getSharedPreferences(FlyingHelper.TEMP_SCREEN_INFO_PREF_FILENAME, Context.MODE_PRIVATE);
//        localPrefs.edit().putInt("key_small_screen_size", 60).apply();
//
//        FlyingHelper helper = mock(FlyingHelper.class);
//        when(helper.getSettings()).thenReturn(new Settings(globalPrefs));
//        FlyingLayout.OnFlyingEventListener listener = new MySimpleOnFlyingEventListener(helper);
//        FrameLayout v = new FrameLayout(RuntimeEnvironment.application);
//        listener.onShrink(v);
//        assertEquals(Math.round(60 - 100*FlyingHelper.SMALL_SCREEN_SIZE_DELTA), localPrefs.getInt("key_small_screen_size", 0));
//    }


    @Test
    public void it_should_not_decrease_size_lower_than_limit() throws Exception {
        PowerMockito.mockStatic(NFW.class);
        PowerMockito.when(NFW.class, "getNiwatoriContext", any(Context.class)).thenReturn(RuntimeEnvironment.application);

        FlyingHelper helper = mock(FlyingHelper.class);
        Settings settings = new Settings(globalPrefs);
        Settings.ScreenData data = new Settings.ScreenData(settings);
        data.smallScreenSize = FlyingHelper.SMALLEST_SMALL_SCREEN_SIZE;
        when(helper.getSettings()).thenReturn(settings.update(data));
        when(helper.getScreenData()).thenReturn(data);


        FlyingLayout.OnFlyingEventListener listener = new MySimpleOnFlyingEventListener(helper);
        FrameLayout v = new FrameLayout(RuntimeEnvironment.application);
        listener.onShrink(v);
        ArgumentCaptor<Settings.ScreenData> captor = ArgumentCaptor.forClass(Settings.ScreenData.class);
        verify(helper).setScreenData(captor.capture());
        assertEquals(Math.round(100*FlyingHelper.SMALLEST_SMALL_SCREEN_SIZE), (int)(100*captor.getValue().smallScreenSize));
    }
}