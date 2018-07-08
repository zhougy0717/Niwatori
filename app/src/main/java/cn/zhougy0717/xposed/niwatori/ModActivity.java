package cn.zhougy0717.xposed.niwatori;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.google.common.base.Strings;

import cn.zhougy0717.xposed.niwatori.handlers.DialogHandler;
import cn.zhougy0717.xposed.niwatori.handlers.PopupWindowHandler;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by tkgktyk on 2015/02/12.
 */
public class ModActivity extends XposedModule {
    private static final String CLASS_DECOR_VIEW_M = "com.android.internal.policy.PhoneWindow$DecorView";
    private static final String CLASS_DECOR_VIEW_N = "com.android.internal.policy.DecorView";
    private static final String CLASS_DECOR_VIEW =
            (Build.VERSION.SDK_INT >= 24) ? CLASS_DECOR_VIEW_N :
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ? CLASS_DECOR_VIEW_M :
                            "com.android.internal.policy.impl.PhoneWindow$DecorView";

    //    private static final String CLASS_SOFT_INPUT_WINDOW = "android.inputmethodservice.SoftInputWindow";
    private static final String CLASS_CONTEXT_IMPL = "android.app.ContextImpl";

    private static final String FIELD_FLYING_HELPER = NFW.NAME + "_flyingHelper";

    private static final String FIELD_SETTINGS_CHANGED_RECEIVER = NFW.NAME + "_settingsChangedReceiver";

    public static FlyingHelper createFlyingHelper(FrameLayout decorView) {
        try {
            FlyingHelper helper = (FlyingHelper) XposedHelpers.getAdditionalInstanceField(decorView, FIELD_FLYING_HELPER);
            if (helper == null) {
                helper = new FlyingHelper(decorView, 1, false);
                XposedHelpers.setAdditionalInstanceField(decorView, FIELD_FLYING_HELPER, helper);
            }

            final FlyingHelper h = helper;
            decorView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return h.onTouchEvent(event);
                }
            });
            decorView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    boolean changed = (left!=oldLeft) || (right!=oldRight) || (bottom!=oldBottom) || (top!=oldTop);
                    h.onLayout(changed, left, top, right, bottom);
                    while(!h.mLayoutCallbacks.isEmpty()) {
                        Runnable r = h.mLayoutCallbacks.poll();
                        r.run();
                    }
                }
            });
            return helper;
        } catch (Throwable t) {
            logE(t);
            return null;
        }
    }

    public static void initZygote() {
        try {
            installToDecorView();
//            installToActivity();
            ActivityHandler.install();
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
//            XposedBridge.hookAllConstructors(classDecorView, new XC_MethodHook() {
//                @Override
//                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                    try {
//                        final FrameLayout decorView = (FrameLayout) param.thisObject;
//                        // need to reload on each package?
//                        createFlyingHelper(decorView);
////                        setBackground(decorView);
//                    } catch (Throwable t) {
//                        logE(t);
//                    }
//                }
//            });


            XposedBridge.hookAllMethods(classDecorView, "dispatchTouchEvent", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod (MethodHookParam param) throws Throwable {
                    final FrameLayout decorView = (FrameLayout)param.thisObject;
                    final FlyingHelper helper = getHelper(decorView);
                    if (helper == null){
                        return invokeOriginalMethod(param);
                    }

                    MotionEvent event = (MotionEvent)param.args[0];
                    if (!helper.isResized()) {
                        helper.getTriggerGesture().onTouchEvent(event);
                    }
                    boolean result = (boolean)invokeOriginalMethod(param);
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        // We don't want to hijack ACTION_DOWN.
                        return true;
                    }
                    return result;
                }
            });
            /**
             * REVISIT: It looks like not that useful.
             */
//            XposedHelpers.findAndHookMethod(View.class, "setBackground", Drawable.class,
//                    new XC_MethodHook() {
//                        @Override
//                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                            if (!param.thisObject
//                                    .getClass()
//                                    .getName()
//                                    .equals(CLASS_DECOR_VIEW)) {
//                                return;
//                            }
//                            final FrameLayout decorView = (FrameLayout) param.thisObject;
//                            final Drawable drawable = (Drawable) param.args[0];
//                            param.args[0] = censorDrawable(decorView, drawable);
//                        }
//                    });
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

        } catch (Throwable t) {
            logE(t);
        }
    }

    private static void setBackground(View decorView) {
        decorView.setBackground(censorDrawable(decorView, decorView.getBackground()));
    }

    public static Drawable censorDrawable(View decorView, Drawable drawable) {
        if (drawable == null) {
            final TypedValue a = new TypedValue();
            if (decorView.getContext().getTheme().resolveAttribute(android.R.attr.windowBackground, a, true)) {
                if (a.type >= TypedValue.TYPE_FIRST_COLOR_INT && a.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                    // color
                    final int color = a.data;
                    logD("background color: " + String.format("#%08X", color));
                    logD("set opaque background color");
                    drawable = new ColorDrawable(color);
                } else {
                    try {
                        final Drawable d = decorView.getResources().getDrawable(a.resourceId);
                        logD("background drawable opacity: " + Integer.toString(d.getOpacity()));
                        logD("set opaque background drawable");
                        drawable = d;
                    }
                    catch (Throwable t) {
                        drawable = new ColorDrawable(Color.BLACK);
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

    public static FlyingHelper getHelper(@NonNull final FrameLayout decorView) {
        FlyingHelper helper = (FlyingHelper) XposedHelpers.getAdditionalInstanceField(
                decorView, FIELD_FLYING_HELPER);
        return helper;
    }
}
