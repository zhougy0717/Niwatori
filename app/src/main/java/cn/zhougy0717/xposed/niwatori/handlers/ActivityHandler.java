package cn.zhougy0717.xposed.niwatori.handlers;

import android.app.Activity;
import android.app.Application;
import android.app.TabActivity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import com.google.common.base.Strings;

import cn.zhougy0717.xposed.niwatori.FlyingHelper;
import cn.zhougy0717.xposed.niwatori.ModActivity;
import cn.zhougy0717.xposed.niwatori.NFW;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by zhougua on 1/13/2018.
 */

public class ActivityHandler extends BaseHandler {
    private static final String CLASS_DECOR_VIEW_M = "com.android.internal.policy.PhoneWindow$DecorView";
    private static final String CLASS_DECOR_VIEW_N = "com.android.internal.policy.DecorView";
    public static final String CLASS_DECOR_VIEW =
            (Build.VERSION.SDK_INT >= 24) ? CLASS_DECOR_VIEW_N :
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ? CLASS_DECOR_VIEW_M :
                            "com.android.internal.policy.impl.PhoneWindow$DecorView";
    private static final String CLASS_CONTEXT_IMPL = "android.app.ContextImpl";

    @Override
    final protected BaseHandler.IFlyingHandler allocateHandler(FrameLayout decorView) {
        return new CustomizedHandler(decorView);
    }

    @Override
    final protected IFlyingHandler allocateHandler(Object obj) {
        FrameLayout decorView = (FrameLayout)((Activity)obj).getWindow().peekDecorView();
        return allocateHandler(decorView);
    }

    @Override
    final protected FrameLayout getDecorView(Object obj) {
        return (FrameLayout)((Activity)obj).getWindow().peekDecorView();
    }

    private static class CustomizedHandler extends FlyingHandler {
        protected CustomizedHandler(FrameLayout decorView) {
            super(decorView);
            mActionReceiver.setFilter(NFW.FOCUSED_ACTIVITY_FILTER);
            mSettingsLoadedReceiver.setFilter(NFW.FOCUSED_ACTIVITY_FILTER);
        }

        @Override
        protected void actionOnFling() {
            // Do nothing
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return mEdgeGesture.onTouchEvent(event);
        }

        @Override
        public void dealWithPersistentIn() {
            /**
             * Because onResume and onPause are running in parallel,
             * the order of registerReceiver and sendBroadcast is not promised.
             * This handler is handling the case of:
             *      registerReceiver is invoked after sendBroadcast
             * In this case, SettingsLoadRecever won't be triggered.
             * Because of the latency between write and read consistently, the Runnable should be executed by 50ms delayed.
             * Refer to comment in SettingsLoadReceiver for another case.
             */
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mHelper.getSettings().smallScreenPersistent) {
                        if (mHelper.getSettings().screenResized && !mHelper.isResized()) {
                            mHelper.performAction(NFW.ACTION_FORCE_SMALL_SCREEN);
                        } else if (mHelper.getSettings().screenResized && mHelper.isResized()) {
                            mHelper.performAction(NFW.ACTION_REFRESH_SMALL_SCREEN);
                        } else if (!mHelper.getSettings().screenResized && mHelper.isResized()) {
                            mHelper.performAction(NFW.ACTION_RESET);
                        }
                    }
                }
            }, 50);

            if (mDecorView.getBackground() == null) {
                XposedHelpers.callMethod(mDecorView, "setWindowBackground", ModActivity.censorDrawable(mDecorView, null));
            }
        }

        @Override
        public void dealWithPersistentOut() {
            if (mHelper != null && !mHelper.getSettings().smallScreenPersistent) {
                // NOTE: When fire actions from shortcut (ActionActivity), it causes onPause and onResume events
                // because through an Activity. So shouldn't reset automatically.
                mHelper.resetState(true);
            }

            mHelper.onExit();
        }

        public boolean onInterceptTouchEvent(MotionEvent event){
            return mHelper.onInterceptTouchEvent(event);
        }
        public void draw(Canvas canvas){
            mHelper.draw(canvas);
        }
        public void rotate(){
            mHelper.rotate();
        }
    }

    public void install() {
        try {
            //
            // initialize addtional fields
            //
            XposedHelpers.findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Application application = (Application) param.thisObject;
                    application.registerActivityLifecycleCallbacks(new MyActivityLifecyckeCallbacks());
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
                                final FrameLayout decorView = (FrameLayout) activity.getWindow().peekDecorView();
                                final IFlyingHandler handler = createFlyingHandler(decorView);
                                final Configuration newConfig = (Configuration) param.args[0];
                                if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                                    handler.rotate();
                                } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                                    handler.rotate();
                                }
                            } catch (Throwable t) {
                                logE(t);
                            }
                        }
                    });

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
            final Class<?> classDecorView = XposedHelpers.findClass(
                    CLASS_DECOR_VIEW, null);
            XposedBridge.hookAllMethods(classDecorView, "dispatchTouchEvent", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    final FrameLayout decorView = (FrameLayout) param.thisObject;
                    IFlyingHandler handler = createFlyingHandler(decorView);

                    MotionEvent event = (MotionEvent) param.args[0];
                    handler.onTouchEvent(event);
                }
            });

            XposedHelpers.findAndHookMethod(classDecorView, "onInterceptTouchEvent", MotionEvent.class,
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                            try {
                                final FrameLayout decorView = (FrameLayout) methodHookParam.thisObject;
                                final MotionEvent event = (MotionEvent) methodHookParam.args[0];
                                final IFlyingHandler handler = createFlyingHandler(decorView);
                                if (handler.onInterceptTouchEvent(event)) {
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
                                final IFlyingHandler handler = createFlyingHandler(decorView);
                                handler.draw(canvas);
                            } catch (Throwable t) {
                                logE(t);
                            }
                        }
                    });
        } catch (Throwable t) {
            logE(t);
        }
    }

    private class MyActivityLifecyckeCallbacks implements Application.ActivityLifecycleCallbacks {
        private IFlyingHandler mHandler;
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

        }

        @Override
        public void onActivityStarted(Activity activity) {

        }

        @Override
        public void onActivityResumed(Activity activity) {
            if (activity instanceof TabActivity) {
                return;
            }

            try {
                FrameLayout decorView = (FrameLayout) activity.getWindow().peekDecorView();
                mHandler = createFlyingHandler(decorView);
                mHandler.registerReceiver();
                PopupWindowHandler.onResume(activity);
                DialogHandler.setCurrentActivity(activity);
                mHandler.dealWithPersistentIn();
            } catch (Throwable t) {
                logE(t);
            }
        }

        @Override
        public void onActivityPaused(Activity activity) {
            if (activity instanceof TabActivity) {
                return;
            }
            try {
                logD(activity + "#onPause");
                mHandler.unregisterReceiver();
                mHandler.dealWithPersistentOut();
                PopupWindowHandler.onPause(activity);
            } catch (Throwable t) {
                logE(t);
            }
        }

        @Override
        public void onActivityStopped(Activity activity) {

        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

        }

        @Override
        public void onActivityDestroyed(Activity activity) {

        }
    }
}
