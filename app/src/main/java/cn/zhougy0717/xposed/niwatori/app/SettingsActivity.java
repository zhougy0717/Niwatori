package cn.zhougy0717.xposed.niwatori.app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.widget.Toast;

import com.google.common.base.Objects;
import com.google.common.base.Strings;

import java.io.File;

import cn.zhougy0717.xposed.niwatori.BuildConfig;
import cn.zhougy0717.xposed.niwatori.NFW;
import cn.zhougy0717.xposed.niwatori.R;
import cn.zhougy0717.xposed.niwatori.app.util.InAppBillingActivity;

/**
 * Created by tkgktyk on 2015/02/13.
 */
public class SettingsActivity extends InAppBillingActivity {
    private static final String TAG = SettingsActivity.class.getSimpleName();
    private static final boolean PURCHASED = BuildConfig.DEBUG && true;

    private static final String SKU_PREMIUM_SETTINGS = "premium_settings";
    private boolean mHasPremiumSettings;
    private SettingsFragment mSettingsFragment;

    @Override
    protected String getBase64EncodedPublicKey() {
        return "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtbZkkt7/GU/XBfnnQKPUPeub74NKsOi7TTdQLeFigeVwx2oLbTDoi094/AGF+yD3qKYn+fAtJrVKvZ7ebbESgR5EMub4VDunbhjYq+liAnVnEDr2AWWf7WiOzYaYElpA6xi02Aa1Sslw+iE62ThUA1c6454+rI/g6X7n4URFJSzVd+ZkqT6itEvnr0/apLIs05xhZBLRQTF15c6zDOTejVdqNaZdwFHVDCD2w71uuoFgRhdO3GqFmjwFWyaBkdj3V+8ZZ+dOd+6WX5GysE6uans2zBrmGR+ujUQPNymjXAj1H3o+XTHUIBo4gnlFqGeM9ncQOSSdpfX5w3aISxDwMwIDAQAB";
    }

    @Override
    protected void initBilling() {
        addSku(SKU_PREMIUM_SETTINGS);
        setOnBillingEventListener(new OnBillingEventListener() {
            @Override
            public void onUpdatePurchase(String sku, boolean purchased) {
                if (Objects.equal(sku, SKU_PREMIUM_SETTINGS)) {
                    mHasPremiumSettings = purchased || PURCHASED;
                    NFW.getSharedPreferences(SettingsActivity.this)
                            .edit()
                            .putBoolean(getString(R.string.key_purchase_premium_settings), purchased)
                            .apply();
                    if (mSettingsFragment != null && mSettingsFragment.isVisible()) {
                        mSettingsFragment.updatePremiumSettings(mHasPremiumSettings);
                    }
                }
            }

            @Override
            public void onBillingIsUnavailable() {
                mHasPremiumSettings = false;
                NFW.getSharedPreferences(SettingsActivity.this)
                        .edit()
                        .putBoolean(getString(R.string.key_purchase_premium_settings), false)
                        .apply();
                if (mSettingsFragment != null && mSettingsFragment.isVisible()) {
                    mSettingsFragment.disablePremiumSettings();
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);

        if (BuildConfig.DEBUG) {
            setTitle("[DEBUG] " + getTitle());
        }

        mHasPremiumSettings =
                NFW.getSharedPreferences(SettingsActivity.this)
                        .getBoolean(getString(R.string.key_purchase_premium_settings), false);

        mSettingsFragment = SettingsFragment.newInstance(mHasPremiumSettings);
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.container, mSettingsFragment)
                .commit();
    }

    public static class BaseFragment extends PreferenceFragment {
        private final SharedPreferences.OnSharedPreferenceChangeListener mChangeListener
                = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                NFW.sendSettingsChanged(getActivity(), sharedPreferences);
            }
        };

        @SuppressWarnings("deprecation")
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
//            getPreferenceManager().setSharedPreferencesMode(PreferenceActivity.MODE_WORLD_READABLE);
//            File prefsFile = new File(getContext().getFilesDir() + "/../shared_prefs/" + NFW.PACKAGE_NAME + "_preferences" + ".xml");
//            prefsFile.setReadable(true, false);
            getPreferenceManager().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(mChangeListener);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            getPreferenceManager().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(mChangeListener);
        }

        protected Preference findPreference(@StringRes int id) {
            return findPreference(getString(id));
        }

        protected void showListSummary(@StringRes int id) {
            showListSummary(id, null);
        }

        protected void showListSummary(@StringRes int id,
                                       @Nullable final Preference.OnPreferenceChangeListener extraListener) {
            ListPreference list = (ListPreference) findPreference(id);
            list.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    setListSummary((ListPreference) preference, (String) newValue);
                    if (extraListener != null) {
                        return extraListener.onPreferenceChange(preference, newValue);
                    }
                    return true;
                }
            });
            // pre-perform
            list.getOnPreferenceChangeListener().onPreferenceChange(list, list.getValue());
        }

        private void setListSummary(ListPreference pref, String value) {
            int index = pref.findIndexOfValue(value);
            CharSequence entry;
            if (index != -1) {
                entry = pref.getEntries()[index];
            } else {
                entry = "default";
            }
            pref.setSummary(getString(R.string.current_s1, entry));
        }

        protected void showTextSummary(@StringRes int id) {
            showTextSummary(id, null);
        }

        protected void showTextSummary(@StringRes int id, @Nullable final String suffix) {
            EditTextPreference et = (EditTextPreference) findPreference(id);
            et.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String value = (String) newValue;
                    if (!Strings.isNullOrEmpty(suffix)) {
                        value += suffix;
                    }
                    preference.setSummary(getString(R.string.current_s1, value));
                    return true;
                }
            });
            et.getOnPreferenceChangeListener().onPreferenceChange(et,
                    et.getText());
        }

        protected void openActivity(@StringRes int id, final Class<?> cls) {
            openActivity(id, cls, null);
        }

        protected void openActivity(@StringRes int id, final Class<?> cls, final ExtendsPutter putter) {
            Preference pref = findPreference(id);
            pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent activity = new Intent(preference.getContext(), cls);
                    if (putter != null) {
                        putter.putExtends(activity);
                    }
                    startActivity(activity);
                    return true;
                }
            });
        }

        protected interface ExtendsPutter {
            void putExtends(Intent activityIntent);
        }

        protected void setUpSwitch(@StringRes int id, final OnSwitchChangeListener listener) {
            SwitchPreference sw = (SwitchPreference) findPreference(id);
            sw.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean enabled = (Boolean) newValue;
                    listener.onChange((SwitchPreference) preference, enabled);
                    return true;
                }
            });
            sw.getOnPreferenceChangeListener().onPreferenceChange(sw, sw.isChecked());
        }

        protected interface OnSwitchChangeListener {
            void onChange(SwitchPreference sw, boolean enabled);
        }
    }

    public static class SettingsFragment extends BaseFragment
            implements ConfirmDialogFragment.OnConfirmedListener {
        private static final int REQUEST_INITIAL_POSITION = 1;
        private static final int REQUEST_SMALL_SCREEN = 2;

        private static final int mPurchasePremiumSettings = R.string.key_purchase_premium_settings;
        private static final String ARG_HAS_PREMIUM_SETTINGS = NFW.PACKAGE_NAME + ".HAS_PREMIUM_SETTINGS";

        private SettingsActivity mSettingsActivity;

        private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updatePreferences();
            }
        };

        public static SettingsFragment newInstance(boolean hasPremiumSettings) {
            Bundle args = new Bundle();
            args.putBoolean(ARG_HAS_PREMIUM_SETTINGS, hasPremiumSettings);
            SettingsFragment settings = new SettingsFragment();
            settings.setArguments(args);
            return settings;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            mSettingsActivity = (SettingsActivity) activity;

            mSettingsActivity.registerReceiver(mReceiver, NFW.SETTINGS_CHANGED_FILTER);
        }

        @Override
        public void onDetach() {
            super.onDetach();

            mSettingsActivity.unregisterReceiver(mReceiver);

            mSettingsActivity = null;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_settings);

            updatePremiumSettings(getArguments().getBoolean(ARG_HAS_PREMIUM_SETTINGS, false));

            updatePreferences();
        }

        private void updatePreferences() {
            if (mSettingsActivity != null && !mSettingsActivity.isBillingSupported()) {
                disablePremiumSettings();
            }

            findPreference(R.string.key_purchase_premium_settings)
                    .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            if (mSettingsActivity != null) {
                                mSettingsActivity.purchase(SKU_PREMIUM_SETTINGS);
                            }
                            return true;
                        }
                    });

            // Settings
            openActivity(R.string.key_blacklist, AppSelectActivity.class, new ExtendsPutter() {
                @Override
                public void putExtends(Intent activityIntent) {
                    activityIntent.putExtra(AppSelectActivity.EXTRA_PREF_KEY_ID, R.string.key_blacklist);
                    activityIntent.putExtra(AppSelectActivity.EXTRA_TITLE_ID, R.string.blacklist_activity_name);
                }
            });
            showListSummary(R.string.key_extra_action);
            showListSummary(R.string.key_action_when_tap_outside);
            showListSummary(R.string.key_action_when_double_tap_outside);
            // movable screen
            showTextSummary(R.string.key_speed);
            showListSummary(R.string.key_boundary_color_ms, new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final int width = Math.round(getResources().getDimension(R.dimen.boundary_width));
                    final int color = Color.parseColor((String) newValue);
                    final GradientDrawable drawable = NFW.makeBoundaryDrawable(width, color);
                    final int size = Math.round(getResources().getDimension(android.R.dimen.app_icon_size));
                    drawable.setSize(size, size);
                    preference.setIcon(drawable);
                    return true;
                }
            });
            findPreference(R.string.key_initial_position)
                    .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            if (hasPremiumSettings()) {
                                openActivity(InitialPositionActivity.class);
                            } else {
                                ConfirmDialogFragment.newInstance(
                                        getString(R.string.title_premium_settings),
                                        getString(R.string.note_premium_settings),
                                        null, SettingsFragment.this, REQUEST_INITIAL_POSITION)
                                        .show(getFragmentManager(), "initial_position");
                            }
                            return true;
                        }
                    });
            // small screen
            showListSummary(R.string.key_boundary_color_ss, new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final int width = Math.round(getResources().getDimension(R.dimen.boundary_width));
                    final int color = Color.parseColor((String) newValue);
                    final GradientDrawable drawable = NFW.makeBoundaryDrawable(width, color);
                    final int size = Math.round(getResources().getDimension(android.R.dimen.app_icon_size));
                    drawable.setSize(size, size);
                    preference.setIcon(drawable);
                    return true;
                }
            });
            findPreference(R.string.key_small_screen)
                    .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            if (hasPremiumSettings()) {
                                openActivity(SmallScreenActivity.class);
                            } else {
                                ConfirmDialogFragment.newInstance(
                                        getString(R.string.title_premium_settings),
                                        getString(R.string.note_premium_settings),
                                        null, SettingsFragment.this, REQUEST_SMALL_SCREEN)
                                        .show(getFragmentManager(), "small_screen");
                            }
                            return true;
                        }
                    });
            openActivity(R.string.key_another_resize_method_targets, AppSelectActivity.class,
                    new ExtendsPutter() {
                        @Override
                        public void putExtends(Intent activityIntent) {
                            activityIntent.putExtra(AppSelectActivity.EXTRA_PREF_KEY_ID,
                                    R.string.key_another_resize_method_targets);
                            activityIntent.putExtra(AppSelectActivity.EXTRA_TITLE_ID,
                                    R.string.another_resize_method_targets_activity_name);
                        }
                    });
            setUpSwitch(R.string.key_small_screen_persistent, new OnSwitchChangeListener() {
                @Override
                public void onChange(SwitchPreference sw, boolean enabled) {
//                    Context context = sw.getContext();
//                    Intent service = new Intent(context, PersistentService.class);
//                    if (enabled) {
//                        context.startService(service);
//                    } else {
//                        context.stopService(service);
//                    }
                }
            });
            // Other
            showListSummary(R.string.key_extra_action_on_recents);
            // About
            Preference about = findPreference(R.string.key_about);
            about.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent activity = new Intent(preference.getContext(), AboutActivity.class);
                    activity.putExtra(AboutActivity.EXTRA_DONATED, hasPremiumSettings());
                    startActivity(activity);
                    return true;
                }
            });
            about.setSummary(getString(R.string.app_name) + " " + BuildConfig.VERSION_NAME);
        }

        private void updatePremiumSettings(boolean purchased) {
            findPreference(mPurchasePremiumSettings).setEnabled(!purchased);
        }

        private void disablePremiumSettings() {
            Preference pref = findPreference(mPurchasePremiumSettings);
            pref.setEnabled(false);
            pref.setSummary(R.string.summary_unavailable_premium_settings);
        }

        private void openActivity(Class<?> cls) {
            Intent activity = new Intent(getActivity(), cls);
            startActivity(activity);
        }

        @Override
        public void onConfirmed(int requestCode, Bundle extras) {
            Toast.makeText(getActivity(), R.string.thank_you_very_much, Toast.LENGTH_SHORT).show();
            switch (requestCode) {
                case REQUEST_INITIAL_POSITION:
                    openActivity(InitialPositionActivity.class);
                    break;
                case REQUEST_SMALL_SCREEN:
                    openActivity(SmallScreenActivity.class);
                    break;
            }
        }

        private boolean hasPremiumSettings() {
            return mSettingsActivity != null && mSettingsActivity.mHasPremiumSettings;
        }
    }
}
