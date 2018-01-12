package jp.tkgktyk.xposed.niwatori;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.TabHost;

import com.google.common.base.Strings;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import jp.tkgktyk.flyinglayout.FlyingLayout;
import jp.tkgktyk.xposed.niwatori.app.DialogHandler;
import jp.tkgktyk.xposed.niwatori.app.PopupWindowHandler;

/**
 * Created by tkgktyk on 2015/02/12.
 */
public class ModActivity extends XposedModule {
    private static final String CLASS_DECOR_VIEW_M = "com.android.internal.policy.PhoneWindow$DecorView";
    private static final String CLASS_DECOR_VIEW_N = "com.android.internal.policy.DecorView";
    private static final String CLASS_DECOR_VIEW =
            (Build.VERSION.SDK_INT >= 24)? CLASS_DECOR_VIEW_N :
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)? CLASS_DECOR_VIEW_M :
                            "com.android.internal.policy.impl.PhoneWindow$DecorView";

    private static final String CLASS_SOFT_INPUT_WINDOW = "android.inputmethodservice.SoftInputWindow";
    private static final String CLASS_CONTEXT_IMPL = "android.app.ContextImpl";

    private static final String FIELD_FLYING_HELPER = NFW.NAME + "_flyingHelper";

    private static final String FIELD_RECEIVER_REGISTERED = NFW.NAME + "_receiverRegistered";
    private static final String FIELD_HAS_FOCUS = NFW.NAME + "_hasFocus";

    private static final String FIELD_DIALOG_ACTION_RECEIVER = NFW.NAME + "_dialogActionReceiver";
    private static final BroadcastReceiver mActivityActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                final String action = intent.getAction();
                logD("activity broadcast receiver: " + action);
                final Activity activity = (Activity) context;
                if (isTabContent(activity)) {
                    logD(activity.getLocalClassName() + " is a tab content.");
                    return;
                }
                final FlyingHelper helper = getHelper(activity);
                if (helper == null) {
                    logD("DecorView is null.");
                    return;
                }
                final String packageName = activity.getPackageName();
                if (helper.getSettings().blackList.contains(packageName)) {
                    if (helper.getSettings().logActions) {
                        log(activity.toString() + "is ignored");
                    }
                    return;
                }
                helper.performAction(action);
                abortBroadcast();
                if (helper.getSettings().logActions) {
                    log(packageName + " consumed: " + action);
                }
            } catch (Throwable t) {
                logE(t);
            }
        }
    };

    private static final String FIELD_SETTINGS_CHANGED_RECEIVER = NFW.NAME + "_settingsChangedReceiver";

    private static XSharedPreferences mPrefs;

    public static FlyingHelper createFlyingHelper (FrameLayout decorView, int frameLayoutHierarchy){
        try {
            FlyingHelper helper = (FlyingHelper) XposedHelpers.getAdditionalInstanceField(decorView, FIELD_FLYING_HELPER);
            if(helper == null){
                helper = new FlyingHelper(decorView, frameLayoutHierarchy, false, newSettings(mPrefs));
                XposedHelpers.setAdditionalInstanceField(decorView, FIELD_FLYING_HELPER, helper);
            }
            return helper;
        }
        catch (Throwable t){
            logE(t);
            return null;
        }
    }
    public static void initZygote(XSharedPreferences prefs) {
        mPrefs = prefs;
        try {
            installToDecorView();
            installToActivity();
            (new DialogHandler()).install();
            (new PopupWindowHandler()).install();
            logD("prepared to attach to Activity and Dialog");

            final XC_MethodReplacement startActivity = new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    logD("startActivity: hooked to send broadcast");
                    try {
                        final Context context = (Context) methodHookParam.thisObject;
                        final Intent intent = (Intent) methodHookParam.args[0];
                        final String action = intent.getAction();
                        if (!Strings.isNullOrEmpty(action) && action.startsWith(NFW.PREFIX_ACTION)) {
                            NFW.performAction(context, action);
                            return null;
                        }
                    } catch (Throwable t) {
                        logE(t);
                    }
                    // When an exception occurs in original method, the exception is transformed
                    // into InvocationTargetException.
                    try {
                        return invokeOriginalMethod(methodHookParam);
                    } catch (Throwable t) {
                        logE(t);
                        throw new ActivityNotFoundException(t.toString());
                    }
                }
            };
            /**
             * General Activity
             */
            XposedBridge.hookAllMethods(Activity.class, "startActivity", startActivity);
            /**
             * LMT Launcher (Pie)
             */
            XposedBridge.hookAllMethods(ContextWrapper.class, "startActivity", startActivity);
            /**
             * GravityBox
             */
            final Class<?> classContextImpl = XposedHelpers.findClass(CLASS_CONTEXT_IMPL, null);
            XposedBridge.hookAllMethods(classContextImpl, "startActivity", startActivity);
        } catch (Throwable t) {
            logE(t);
        }
    }

    private static void installToDecorView() {
        try {
            final Class<?> classDecorView = XposedHelpers.findClass(
//                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M?
//                            CLASS_DECOR_VIEW_M: CLASS_DECOR_VIEW, null);
                    ModActivity.CLASS_DECOR_VIEW, null);
            XposedBridge.hookAllConstructors(classDecorView, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        final FrameLayout decorView = (FrameLayout) param.thisObject;
                        // need to reload on each package?
                        final FlyingHelper helper = new FlyingHelper(decorView, 1, false, newSettings(mPrefs));
                        XposedHelpers.setAdditionalInstanceField(decorView,
                                FIELD_FLYING_HELPER, helper);
//                        setBackground(decorView);
                    } catch (Throwable t) {
                        logE(t);
                    }
                }
            });

            XposedHelpers.findAndHookMethod(View.class, "setBackground", Drawable.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (!param.thisObject
                                    .getClass()
                                    .getName()
                                    .equals(CLASS_DECOR_VIEW)) {
                                return;
                            }
                            final FrameLayout decorView = (FrameLayout) param.thisObject;
                            final Drawable drawable = (Drawable) param.args[0];
                            param.args[0] = censorDrawable(decorView, drawable);
                        }
                    });
            XposedHelpers.findAndHookMethod(classDecorView, "onInterceptTouchEvent", MotionEvent.class,
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                            try {
                                final FrameLayout decorView = (FrameLayout) methodHookParam.thisObject;
                                final MotionEvent event = (MotionEvent) methodHookParam.args[0];
                                final FlyingHelper helper = getHelper(decorView);
                                if (helper != null && helper.onInterceptTouchEvent(event)) {
                                    return true;
                                }
                            } catch (Throwable t) {
                                logE(t);
                            }
                            return invokeOriginalMethod(methodHookParam);
                        }
                    });
            XposedHelpers.findAndHookMethod(classDecorView, "onTouchEvent", MotionEvent.class,
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                            try {
                                final FrameLayout decorView = (FrameLayout) methodHookParam.thisObject;
                                final MotionEvent event = (MotionEvent) methodHookParam.args[0];
                                final FlyingHelper helper = getHelper(decorView);
                                if (helper != null && helper.onTouchEvent(event)) {
                                    return true;
                                }
                            } catch (Throwable t) {
                                logE(t);
                            }
                            return invokeOriginalMethod(methodHookParam);
                        }
                    });
            XposedHelpers.findAndHookMethod(classDecorView, "draw", Canvas.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            try {
                                final FrameLayout decorView = (FrameLayout) param.thisObject;
                                final Canvas canvas = (Canvas) param.args[0];
                                final FlyingHelper helper = getHelper(decorView);
                                if (helper != null) {
                                    helper.draw(canvas);
                                }
                            } catch (Throwable t) {
                                logE(t);
                            }
                        }
                    });
            final Class<?> classFrameLayout = classDecorView.getSuperclass();
            XposedHelpers.findAndHookMethod(classFrameLayout, "onLayout", boolean.class,
                    int.class, int.class, int.class, int.class, new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                            try {
                                final FrameLayout decorView = (FrameLayout) methodHookParam.thisObject;
                                final FlyingHelper helper = getHelper(decorView);

                                if (helper != null) {
                                    final boolean changed = (Boolean) methodHookParam.args[0];
                                    final int left = (Integer) methodHookParam.args[1];
                                    final int top = (Integer) methodHookParam.args[2];
                                    final int right = (Integer) methodHookParam.args[3];
                                    final int bottom = (Integer) methodHookParam.args[4];
                                    helper.onLayout(changed, left, top, right, bottom);
                                    return null;
                                }
                            } catch (Throwable t) {
                                logE(t);
                            }
                            return invokeOriginalMethod(methodHookParam);
                        }
                    });
//            XposedHelpers.findAndHookMethod(classFrameLayout, "onMeasure", int.class, int.class,
//                    new XC_MethodReplacement() {
//                @Override
//                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
//                    try {
//                        final FrameLayout decorView = (FrameLayout) methodHookParam.thisObject;
//                        final FlyingHelper helper = getHelper(decorView);
//                        if (helper != null) {
//                            logD();
//                            final int widthMeasureSpec = (Integer) methodHookParam.args[0];
//                            final int heightMeasureSpec = (Integer) methodHookParam.args[1];
//                            helper.onMeasure(widthMeasureSpec, heightMeasureSpec);
//                            return null;
//                        }
//                    } catch (Throwable t) {
//                        logE(t);
//                    }
//                    return invokeOriginalMethod(methodHookParam);
//                }
//            });

            XposedHelpers.findAndHookMethod(classDecorView, "onAttachedToWindow", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        final FrameLayout decorView = (FrameLayout) param.thisObject;
                        final BroadcastReceiver settingsLoadedReceiver = new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context context, Intent intent) {
                                logD(decorView.getContext().getPackageName() + ": reload settings");
                                // need to reload on each package?
                                final FlyingHelper helper = getHelper(decorView);
                                if (helper != null) {
                                    NFW.Settings settings = (NFW.Settings) intent.getSerializableExtra(NFW.EXTRA_SETTINGS);
                                    getHelper(decorView).onSettingsLoaded(settings);
                                }
                            }
                        };
                        XposedHelpers.setAdditionalInstanceField(decorView,
                                FIELD_SETTINGS_CHANGED_RECEIVER, settingsLoadedReceiver);
                        decorView.getContext().registerReceiver(settingsLoadedReceiver,
                                NFW.SETTINGS_CHANGED_FILTER);
                    } catch (Throwable t) {
                        logE(t);
                    }
                }
            });
            XposedHelpers.findAndHookMethod(classDecorView, "onDetachedFromWindow", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        final FrameLayout decorView = (FrameLayout) param.thisObject;
                        final BroadcastReceiver settingsLoadedReceiver =
                                (BroadcastReceiver) XposedHelpers.getAdditionalInstanceField(decorView,
                                        FIELD_SETTINGS_CHANGED_RECEIVER);
                        if (settingsLoadedReceiver != null) {
                            decorView.getContext().unregisterReceiver(settingsLoadedReceiver);
                        }
                    } catch (Throwable t) {
                        logE(t);
                    }
                }
            });


        } catch (Throwable t) {
            logE(t);
        }
    }

    private static void setBackground(View decorView) {
        decorView.setBackground(censorDrawable(decorView, decorView.getBackground()));
    }

    private static Drawable censorDrawable(View decorView, Drawable drawable) {
        if (drawable == null) {
            final TypedValue a = new TypedValue();
            if (decorView.getContext().getTheme().resolveAttribute(android.R.attr.windowBackground, a, true)) {
                if (a.type >= TypedValue.TYPE_FIRST_COLOR_INT && a.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                    // color
                    final int color = a.data;
                    logD("background color: " + String.format("#%08X", color));
                    if (Color.alpha(color) == 0xFF) {
                        // opaque
                        logD("set opaque background color");
                        drawable = new ColorDrawable(color);
                    }
                } else {
                    final Drawable d = decorView.getResources().getDrawable(a.resourceId);
                    logD("background drawable opacity: " + Integer.toString(d.getOpacity()));
                    if (d.getOpacity() == PixelFormat.OPAQUE) {
                        // opaque
                        logD("set opaque background drawable");
                        drawable = d;
                    }
                }
            }
//        } else if (drawable.getOpacity() == PixelFormat.OPAQUE) {
//            logD("decorView has opaque background drawable");
//            decorView.setBackground(drawable);
        }
        return drawable;
    }

    @Nullable
    private static FrameLayout getDecorView(@NonNull Activity activity) {
        return (FrameLayout) activity.getWindow().peekDecorView();
    }

    @Nullable
    private static FlyingHelper getHelper(@NonNull Activity activity) {
        final FrameLayout decorView = getDecorView(activity);
        if (decorView == null) {
            return null;
        }
        return getHelper(decorView);
    }

    private static FlyingHelper getHelper(@NonNull FrameLayout decorView) {
        return (FlyingHelper) XposedHelpers.getAdditionalInstanceField(
                decorView, FIELD_FLYING_HELPER);
    }

    private static void installToActivity() {
        //
        // initialize addtional fields
        //
        XposedBridge.hookAllConstructors(Activity.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    final Activity activity = (Activity) param.thisObject;
                    XposedHelpers.setAdditionalInstanceField(activity, FIELD_RECEIVER_REGISTERED, false);
                    XposedHelpers.setAdditionalInstanceField(activity, FIELD_HAS_FOCUS, false);
                } catch (Throwable t) {
                    logE(t);
                }
            }
        });
        //
        // register broadcast receiver
        //
        XposedHelpers.findAndHookMethod(Activity.class, "onWindowFocusChanged", boolean.class
                , new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    final Activity activity = (Activity) param.thisObject;
                    final boolean hasFocus = (Boolean) param.args[0];
                    logD(activity + "#onWindowFocusChanged: hasFocus=" + hasFocus);
                    registerReceiver(activity, hasFocus);
                } catch (Throwable t) {
                    logE(t);
                }
            }
        });
        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    final Activity activity = (Activity) param.thisObject;
                    final boolean hasFocus = activity.hasWindowFocus();
                    logD(activity + "#onResume: hasFocus=" + hasFocus);
                    registerReceiver(activity, hasFocus);

                    final FlyingHelper helper = getHelper(activity);
                    if (helper != null && helper.getSettings().smallScreenPersistent) {
                        NFW.requestResizedGlobal(activity);
                    }
                } catch (Throwable t) {
                    logE(t);
                }
            }
        });
        XposedHelpers.findAndHookMethod(Activity.class, "onPause", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    Activity activity = (Activity) param.thisObject;
                    logD(activity + "#onPause");
                    unregisterReceiver(activity);
                } catch (Throwable t) {
                    logE(t);
                }
            }
        });
        XposedHelpers.findAndHookMethod(Activity.class, "onStop", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    Activity activity = (Activity) param.thisObject;
                    logD(activity + "#onStop");
                    resetAutomatically(activity);
                } catch (Throwable t) {
                    logE(t);
                }
            }
        });
        //
        // screen rotation
        //
        XposedHelpers.findAndHookMethod(Activity.class, "onConfigurationChanged", Configuration.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            final Activity activity = (Activity) param.thisObject;
                            final FlyingHelper helper = getHelper(activity);
                            if (helper == null) {
                                logD("DecorView is null");
                                return;
                            }
                            final Configuration newConfig = (Configuration) param.args[0];
                            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                                helper.rotate();
                            } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                                helper.rotate();
                            }
                        } catch (Throwable t) {
                            logE(t);
                        }
                    }
                });
    }

    private static boolean isTabContent(Activity activity) {
        final FrameLayout decor = getDecorView(activity);
        if (decor != null) {
            for (ViewParent v = decor.getParent(); v != null; v = v.getParent()) {
                if (v instanceof TabHost) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void registerReceiver(Activity activity, boolean focused) {
        final Boolean registered = (Boolean) XposedHelpers
                .getAdditionalInstanceField(activity, FIELD_RECEIVER_REGISTERED);
        final Boolean hasFocus = (Boolean) XposedHelpers
                .getAdditionalInstanceField(activity, FIELD_HAS_FOCUS);
        boolean register = false;
        if (focused) {
            if (!hasFocus) {
                // got focus
                if (registered) {
                    logD("register again");
                    activity.unregisterReceiver(mActivityActionReceiver);
                }
                register = true;
            } else {
                // keep focus
                if (!registered) {
                    register = true;
                }
            }
        } else if (hasFocus) {
            // lost focus
            if (registered) {
                logD("register again");
                activity.unregisterReceiver(mActivityActionReceiver);
                register = true;
//                resetAutomatically(activity);
            }
        } else {
            // keep unfocus
            if (!registered) {
                register = true;
            }
        }
        if (register) {
            activity.registerReceiver(mActivityActionReceiver,
                    focused ? NFW.FOCUSED_ACTIVITY_FILTER : NFW.ACTIVITY_FILTER);
            XposedHelpers.setAdditionalInstanceField(
                    activity, FIELD_RECEIVER_REGISTERED, true);
            logD("registered");
        }
        XposedHelpers.setAdditionalInstanceField(
                activity, FIELD_HAS_FOCUS, focused);
    }

    private static void unregisterReceiver(Activity activity) {
        final Boolean registered = (Boolean) XposedHelpers
                .getAdditionalInstanceField(activity, FIELD_RECEIVER_REGISTERED);
        if (registered) {
            activity.unregisterReceiver(mActivityActionReceiver);
            XposedHelpers.setAdditionalInstanceField(
                    activity, FIELD_RECEIVER_REGISTERED, false);
            logD("unregistered");

//            resetAutomatically(activity);
        } else {
            logD("not registered");
        }
    }

    private static void resetAutomatically(Activity activity) {
        final FlyingHelper helper = getHelper(activity);
        if (helper == null) {
            logD("DecorView is null");
            return;
        }
        if (helper.getSettings().autoReset) {
            // When fire actions from shortcut (ActionActivity), it causes onPause and onResume events
            // because through an Activity. So shouldn't reset automatically.
            helper.resetState(true);
        }
    }
}
