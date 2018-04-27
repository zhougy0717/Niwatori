package cn.zhougy0717.xposed.niwatori;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.thoughtworks.xstream.security.InterfaceTypePermission;

import org.codehaus.plexus.util.cli.Arg;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.powermock.reflect.Whitebox;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import cn.zhougy0717.xposed.niwatori.app.ChangeSettingsActionReceiver;
import de.robv.android.xposed.XposedBridge;
import jp.tkgktyk.flyinglayout.FlyingLayout;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyPrivate;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Created by zhougua on 4/18/2018.
 */
@RunWith(RobolectricTestRunner.class)
@Config(
        constants = BuildConfig.class,
        sdk = 26
        )
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareForTest({FlyingHelper.class, WorldReadablePreference.class, XposedBridge.class, NFW.class})
public class FlyingHelperTest {
    @Rule
    public PowerMockRule rule = new PowerMockRule();

    private FlyingHelper helper;
    private FlyingHelper spyHelper;
    private SharedPreferences globalPrefs;
    private SharedPreferences localPrefs;

    @Before
    public void setUp() throws Exception {
        globalPrefs = PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application);
        FrameLayout decorView = new FrameLayout(RuntimeEnvironment.application);
        PowerMockito.mockStatic(WorldReadablePreference.class);
        when(WorldReadablePreference.class, "getSettings").thenReturn(new Settings(globalPrefs));

        PowerMockito.mockStatic(XposedBridge.class);
        PowerMockito.doNothing().when(XposedBridge.class, "log", anyString());

        helper = new FlyingHelper(decorView, 0, false);
        spyHelper = spy(helper);
    }

    @After
    public void tearDown(){
        globalPrefs.edit().clear().apply();
        if (localPrefs != null){
            localPrefs.edit().clear().apply();
        }
    }
    @Test
    public void it_should_save_pivotX_in_local_SharedPreference() throws Exception {
        doNothing().when(spyHelper).onSettingsLoaded();
        spyHelper.performAction(NFW.ACTION_CS_SWAP_LEFT_RIGHT);

        localPrefs = spyHelper.getAttachedView().getContext().getSharedPreferences(FlyingHelper.TEMP_SCREEN_INFO_PREF_FILENAME, 0);
        assertEquals(100, localPrefs.getInt("key_small_screen_pivot_x", 0));
    }

    @Test
    public void it_should_load_pivotX_firstly_from_local_SharedPreference() throws Exception {
        doNothing().when(spyHelper).setScale(anyFloat());
        doNothing().when(spyHelper, "updateBoundary");
        localPrefs = spyHelper.getAttachedView().getContext().getSharedPreferences(FlyingHelper.TEMP_SCREEN_INFO_PREF_FILENAME, 0);
        localPrefs.edit().putInt("key_small_screen_pivot_x", 12).apply();
        localPrefs.edit().putInt("key_small_screen_pivot_y", 56).apply();
        globalPrefs.edit()
                .putInt("key_small_screen_pivot_x", 34)
                .apply();
        globalPrefs.edit()
                .putInt("key_small_screen_pivot_y", 78)
                .apply();
        when(WorldReadablePreference.class, "getSettings").thenReturn(new Settings(globalPrefs));
        spyHelper.onSettingsLoaded();

        verify(spyHelper).setPivot(0.12f, 0.56f);
    }

    @Test
    public void it_should_load_pivotX_secondly_from_global_XSharedPreference() throws Exception {
        doNothing().when(spyHelper).setScale(anyFloat());
        doNothing().when(spyHelper, "updateBoundary");

        globalPrefs.edit()
                .putInt("key_small_screen_pivot_x", 34)
                .apply();
        when(WorldReadablePreference.class, "getSettings").thenReturn(new Settings(globalPrefs));
        spyHelper.onSettingsLoaded();

        verify(spyHelper).setPivot(0.34f,0.5f);
    }

    @Test
    public void it_should_notify_Niwatori_to_save_pivotX() throws Exception {
        doNothing().when(spyHelper).onSettingsLoaded();

        Context mockContext = mock(Context.class);
        ViewGroup mockView = mock(ViewGroup.class);
        when(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(globalPrefs);
        when(spyHelper.getAttachedView()).thenReturn(mockView);
        when(mockView.getContext()).thenReturn(mockContext);
        PowerMockito.mockStatic(NFW.class);
        when(NFW.class, "getNiwatoriContext", any(Context.class)).thenReturn(mockContext);
        spyHelper.performAction(NFW.ACTION_CS_SWAP_LEFT_RIGHT);

        ArgumentCaptor<Intent> argmentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mockContext).sendBroadcast(argmentCaptor.capture());
        Intent intent = argmentCaptor.getValue();
        assertEquals(ChangeSettingsActionReceiver.class.getCanonicalName(), intent.getComponent().getClassName());
        assertEquals(100, intent.getIntExtra("key_small_screen_pivot_x", 0));
    }

    @Test
    public void it_should_see_local_prefs_for_pivotX_firstly_by_performAction() throws Exception {
        doNothing().when(spyHelper).onSettingsLoaded();

        Context mockContext = mock(Context.class);
        ViewGroup mockView = mock(ViewGroup.class);
        when(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(globalPrefs);
        when(spyHelper.getAttachedView()).thenReturn(mockView);
        when(mockView.getContext()).thenReturn(mockContext);
        PowerMockito.mockStatic(NFW.class);
        when(NFW.class, "getNiwatoriContext", any(Context.class)).thenReturn(mockContext);

        globalPrefs.edit()
                .putInt("key_small_screen_pivot_x", 12)
                .apply();
        spyHelper.performAction(NFW.ACTION_CS_SWAP_LEFT_RIGHT);
        assertEquals(88, globalPrefs.getInt("key_small_screen_pivot_x", 0));
    }

    @Test
    public void it_should_get_global_pivotX_if_no_local_one() throws Exception {
        doNothing().when(spyHelper).onSettingsLoaded();
        globalPrefs.edit().putInt("key_small_screen_pivot_x", 34).apply();
        when(WorldReadablePreference.class, "getSettings").thenReturn(new Settings(globalPrefs));
        spyHelper.performAction(NFW.ACTION_CS_SWAP_LEFT_RIGHT);
        assertEquals(66, spyHelper.getAttachedView().getContext().getSharedPreferences(FlyingHelper.TEMP_SCREEN_INFO_PREF_FILENAME, 0)
                .getInt("key_small_screen_pivot_x", 0));
    }

    @Test
    public void it_should_only_setPivot_once_by_resize_force() {
        spyHelper.resize(true);
        verify(spyHelper, times(1)).setPivot(anyFloat(), anyFloat());
    }

    private int captureSmallScreenSize(Context context){
        ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(context).sendBroadcast(argumentCaptor.capture());
        Intent intent = argumentCaptor.getValue();
        return intent.getIntExtra("key_small_screen_size", 0);
    }

    @Test
    public void it_should_load_smallScreenSize_firstly_from_local_SharedPreference() throws Exception {
        doNothing().when(spyHelper).setScale(anyFloat());
        doNothing().when(spyHelper, "updateBoundary");
        when(spyHelper.isResized()).thenReturn(true);
        localPrefs = spyHelper.getAttachedView().getContext().getSharedPreferences(FlyingHelper.TEMP_SCREEN_INFO_PREF_FILENAME, 0);
        localPrefs.edit().putInt("key_small_screen_size", 12).apply();
        globalPrefs.edit()
                .putInt("key_small_screen_size", 34)
                .apply();
        when(WorldReadablePreference.class, "getSettings").thenReturn(new Settings(globalPrefs));
        spyHelper.onSettingsLoaded();

        verify(spyHelper).setScale(0.12f);
    }

    @Test
    public void it_should_load_smallScreenSize_secondly_from_global_XSharedPreference() throws Exception {
        doNothing().when(spyHelper).setScale(anyFloat());
        doNothing().when(spyHelper, "updateBoundary");
        when(spyHelper.isResized()).thenReturn(true);

        globalPrefs.edit()
                .putInt("key_small_screen_size", 34)
                .apply();
        when(WorldReadablePreference.class, "getSettings").thenReturn(new Settings(globalPrefs));
        spyHelper.onSettingsLoaded();

        verify(spyHelper).setScale(0.34f);
    }

    @Test
    public void it_should_clear_localPrefs_and_sendBroadcast_to_update_globalPrefs() throws Exception {
        localPrefs = spyHelper.getAttachedView().getContext().getSharedPreferences(FlyingHelper.TEMP_SCREEN_INFO_PREF_FILENAME, 0);
        localPrefs.edit()
                .putInt("key_small_screen_pivot_x", 12)
                .putInt("key_small_screen_size", 34)
                .apply();

        FrameLayout mockView = mock(FrameLayout.class);
        Context mockContext = mock(Context.class);
        when(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(localPrefs);
        when(spyHelper.getAttachedView()).thenReturn(mockView);
        when(mockView.getContext()).thenReturn(mockContext);

        PowerMockito.mockStatic(NFW.class);
        when(NFW.class, "getNiwatoriContext", any(Context.class)).thenReturn(mockContext);

        spyHelper.onExit();

        assertEquals(0, localPrefs.getAll().keySet().size());

        ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mockContext).sendBroadcast(argumentCaptor.capture());
        Intent intent = argumentCaptor.getValue();
        assertEquals(ChangeSettingsActionReceiver.class.getCanonicalName(), intent.getComponent().getClassName());
        assertEquals(12, intent.getIntExtra("key_small_screen_pivot_x", 0));
        assertEquals(34, intent.getIntExtra("key_small_screen_size", 0));
    }

//    @Test
//    public void it_should_use_0_or_100_as_pivotX_in_resize(){
//        SharedPreferences prefs = spyHelper.getAttachedView().getContext().getSharedPreferences(FlyingHelper.TEMP_SCREEN_INFO_PREF_FILENAME, Context.MODE_PRIVATE);
//        prefs.edit().putInt("key_small_screen_pivot_x", 12).apply();
//        spyHelper.resize(true);
//        assertEquals(0, prefs.getInt("key_small_screen_pivot_x", 12));
//        prefs.edit().putInt("key_small_screen_pivot_x", 78).apply();
//        spyHelper.resize(true);
//        assertEquals(0, prefs.getInt("key_small_screen_pivot_x", 100));
//    }
//
//    @Test
//    public void it_should_moveToInitialPosition_before_resize() throws Exception {
//        FrameLayout mockView = mock(FrameLayout.class);
//        when(mockView.getWidth()).thenReturn(100);
//        when(mockView.getHeight()).thenReturn(100);
//        when(mockView.getContext()).thenReturn(RuntimeEnvironment.application);
//        Whitebox.setInternalState(spyHelper, "mView", mockView);
//
//        SharedPreferences prefs = spyHelper.getAttachedView().getContext().getSharedPreferences(FlyingHelper.TEMP_SCREEN_INFO_PREF_FILENAME, Context.MODE_PRIVATE);
//        prefs.edit().putInt("key_small_screen_pivot_x", 12).apply();
//        spyHelper.resize(true);
//        verify(spyHelper).moveWithoutSpeed(FlyingHelper.SMALL_SCREEN_MARGIN, 0, true);
//
//        prefs.edit().putInt("key_small_screen_pivot_x", 78).apply();
//        spyHelper.resize(true);
//        verify(spyHelper).moveWithoutSpeed(-FlyingHelper.SMALL_SCREEN_MARGIN, 0, true);
//    }
}