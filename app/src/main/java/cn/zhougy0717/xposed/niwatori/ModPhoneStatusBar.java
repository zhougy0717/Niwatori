package cn.zhougy0717.xposed.niwatori;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import cn.zhougy0717.xposed.niwatori.handlers.StatusBarHandler;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static android.content.Context.KEYGUARD_SERVICE;

/**
 * Created by tkgktyk on 2015/02/13.
 */
public class ModPhoneStatusBar extends XposedModule {
//    private static final String CLASS_PHONE_STATUS_BAR =
//            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)? "com.android.systemui.statusbar.phone.StatusBar":
//                    "com.android.systemui.statusbar.phone.PhoneStatusBar";
//    private static final String CLASS_PHONE_STATUS_BAR_VIEW = "com.android.systemui.statusbar.phone.PhoneStatusBarView";
//
//    protected static final String FIELD_FLYING_HELPER = NFW.NAME + "_flyingHelper";
//
//    // for status bar
//    protected static FlyingHelper mHelper;

//    protected static Object mPhoneStatusBar;
//    private static View mPhoneStatusBarView;

//    abstract protected String getPanelHolderName();
//    abstract protected String getPanelCollapsedName();

    /**
     * Keep this for some while in case we want to add the status bar extra action shortcut.
     */

//    protected void expandNotificationBar(){
//        XposedHelpers.callMethod(mPhoneStatusBar, "animateExpandNotificationsPanel");
//    }
//    protected void expandQuickSettings(){
//        XposedHelpers.callMethod(mPhoneStatusBar, "animateExpandSettingsPanel");
//    }

    // On Nougat, we use below functions to expand status bar
    //    @Override
//    protected void expandQuickSettings(){
//        Object mNotifPanel = XposedHelpers.getObjectField(mPhoneStatusBar, "mNotificationPanel");
//        try {
//            XposedHelpers.callMethod(mNotifPanel, "instantExpand");
//            if (XposedHelpers.getBooleanField(mNotifPanel, "mQsExpansionEnabled")) {
//                XposedHelpers.callMethod(mNotifPanel, "setQsExpansion",
//                        XposedHelpers.getIntField(mNotifPanel, "mQsMaxExpansionHeight"));
//            }
//        }
//        catch (Throwable t) {
//            XposedHelpers.callMethod(mNotifPanel, "expandWithQs");
//        }
//    }
//
//    @Override
//    protected void expandNotificationBar(){
//        Object mNotifPanel = XposedHelpers.getObjectField(mPhoneStatusBar, "mNotificationPanel");
//        try {
//            XposedHelpers.callMethod(mNotifPanel, "instantExpand");
//        } catch (Throwable t) {
//            XposedHelpers.callMethod(mNotifPanel, "expand", true);
//        }
//    }

//    protected final BroadcastReceiver mGlobalReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            try {
//                final String action = intent.getAction();
//                logD("global broadcast receiver: " + action);
//                View statusBarView = (View)XposedHelpers.callMethod(mPhoneStatusBar, "getStatusBarView");
//                final int mState = XposedHelpers.getIntField(statusBarView, "mState");
//                if (action.startsWith(NFW.PREFIX_ACTION_SB)) {
//                    consumeMyAction(action);
//                    return;
//                }
//                if (mState == 0) { // STATE_CLOSED = 0
//                    return;
//                }
//                // target is status bar
//                mHelper.performAction(action);
//                abortBroadcast();
//                if (mHelper.getSettings().logActions) {
//                    log("statusbar consumed: " + action);
//                }
//            } catch (Throwable t) {
//                logE(t);
//            }
//        }
//
//        @SuppressWarnings("ResourceType")
//        private void consumeMyAction(String action) {
//            if (action.equals(NFW.ACTION_SB_EXPAND_NOTIFICATIONS)) {
//                expandNotificationBar();
//                mHelper.performExtraAction();
//            } else if (action.equals(NFW.ACTION_SB_EXPAND_QUICK_SETTINGS)) {
//                expandQuickSettings();
//                mHelper.performExtraAction();
//            }
//            if (mHelper.getSettings().logActions) {
//                log("statusbar consumed: " + action);
//            }
//        }
//    };

    /**
     * Keep this for some while in case we want to add the status bar extra action shortcut.
     */

    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (!loadPackageParam.packageName.equals("com.android.systemui")) {
            return;
        }
        try {
            installToStatusBar(loadPackageParam.classLoader);
            //
            // for Software Keys
            //
            Settings settings = WorldReadablePreference.getSettings();
            if (settings.extraActionOnRecents != NFW.NONE_ON_RECENTS) {
                final ClassLoader classLoader = loadPackageParam.classLoader;
//                modifySoftwareKey(classLoader);
                log("prepared to modify software recents key");
            }
        } catch (Throwable t) {
            logE(t);
        }
    }

    private void installToStatusBar(ClassLoader classLoader) {
        StatusBarHandler statusBarHandler = new StatusBarHandler();
        statusBarHandler.install(classLoader);
    }

//    private static void modifySoftwareKey(ClassLoader classLoader) {
//        final Class<?> classPhoneStatusBar = XposedHelpers.findClass(CLASS_PHONE_STATUS_BAR, classLoader);
//        XposedBridge.hookAllMethods(classPhoneStatusBar, "prepareNavigationBarView",
//                new XC_MethodHook() {
//                    @Override
//                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                        logD("prepareNavigationBarView");
//                        try {
//                            Object phoneStatusBar = param.thisObject;
//                            final View navigationBarView = (View) XposedHelpers.getObjectField(
//                                    phoneStatusBar, "mNavigationBarView");
//                            modifyRecentsKey(phoneStatusBar, navigationBarView);
//                        } catch (Throwable t) {
//                            logE(t);
//                        }
//                    }
//
//                    private void modifyRecentsKey(final Object phoneStatusBar, View navigationBarView) {
//                        final View recentsButton = (View) XposedHelpers.callMethod(
//                                navigationBarView, "getRecentsButton");
//                        final View.OnClickListener clickListener
//                                = (View.OnClickListener) XposedHelpers.getObjectField(
//                                phoneStatusBar, "mRecentsClickListener");
//                        final View.OnTouchListener touchListener
//                                = (View.OnTouchListener) XposedHelpers.getObjectField(
//                                phoneStatusBar, "mRecentsPreloadOnTouchListener");
//                        View.OnLongClickListener localLCL = null;
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                            localLCL = (View.OnLongClickListener) XposedHelpers.getObjectField(
//                                    phoneStatusBar, "mLongPressBackRecentsListener");
//                        }
//                        final View.OnLongClickListener longClickListener = localLCL;
//                        recentsButton.setLongClickable(false);
//                        recentsButton.setOnLongClickListener(null);
//                        recentsButton.setOnClickListener(null);
//                        final GestureDetector gestureDetector = new GestureDetector(
//                                navigationBarView.getContext(), new GestureDetector.SimpleOnGestureListener() {
//                            @Override
//                            public boolean onSingleTapConfirmed(MotionEvent e) {
//                                try {
//                                    Settings settings = mHelper.getSettings();
//                                    if (settings.extraActionOnRecents != NFW.TAP_ON_RECENTS) {
//                                        clickListener.onClick(recentsButton);
//                                    } else {
//                                        NFW.performAction(recentsButton.getContext(),
//                                                settings.extraAction);
//                                    }
//                                } catch (Throwable t) {
//                                    logE(t);
//                                }
//                                return true;
//                            }
//
//                            @Override
//                            public void onLongPress(MotionEvent e) {
//                                try {
//                                    Settings settings = mHelper.getSettings();
//                                    if (settings.extraActionOnRecents != NFW.LONG_PRESS_ON_RECENTS) {
//                                        if (longClickListener != null) {
//                                            longClickListener.onLongClick(recentsButton);
//                                        } else {
//                                            clickListener.onClick(recentsButton);
//                                        }
//                                    } else {
//                                        NFW.performAction(recentsButton.getContext(),
//                                                settings.extraAction);
//                                    }
//                                    recentsButton.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
//                                } catch (Throwable t) {
//                                    logE(t);
//                                }
//                            }
//
//                            @Override
//                            public boolean onDoubleTap(MotionEvent e) {
//                                try {
//                                    Settings settings = mHelper.getSettings();
//                                    if (settings.extraActionOnRecents != NFW.DOUBLE_TAP_ON_RECENTS) {
//                                        if (settings.extraActionOnRecents == NFW.TAP_ON_RECENTS) {
//                                            clickListener.onClick(recentsButton);
//                                        } else {
//                                            return false;
//                                        }
//                                    } else {
//                                        NFW.performAction(recentsButton.getContext(),
//                                                settings.extraAction);
//                                    }
//                                    recentsButton.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
//                                } catch (Throwable t) {
//                                    logE(t);
//                                }
//                                return true;
//                            }
//                        });
//                        recentsButton.setOnTouchListener(new View.OnTouchListener() {
//                            @Override
//                            public boolean onTouch(View v, MotionEvent event) {
//                                try {
//                                    // original touchListener always return false.
//                                    touchListener.onTouch(v, event);
//                                } catch (Throwable t) {
//                                    logE(t);
//                                }
//                                return gestureDetector.onTouchEvent(event);
//                            }
//                        });
//                    }
//                });
//    }
}
