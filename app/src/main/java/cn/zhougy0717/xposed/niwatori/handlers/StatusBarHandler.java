package cn.zhougy0717.xposed.niwatori.handlers;

import android.app.KeyguardManager;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import cn.zhougy0717.xposed.niwatori.NFW;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import static android.content.Context.KEYGUARD_SERVICE;

public class StatusBarHandler extends BaseHandler {
    @Override
    protected IFlyingHandler allocateHandler(FrameLayout decorView) {
        return new CustomizedHandler(decorView);
    }

    @Override
    protected IFlyingHandler allocateHandler(Object obj) {
        assert (false); // You should not get here.
        return null;
    }

    @Override
    protected FrameLayout getDecorView(Object obj) {
        assert (false); // You should not get here.
        return null;
    }

    private static class CustomizedHandler extends FlyingHandler {

        private GestureDetector gestureDetector;

        protected CustomizedHandler(FrameLayout decorView) {
            super(decorView);
            mActionReceiver.setFilter(NFW.STATUS_BAR_FILTER);
//            mSettingsLoadedReceiver.setFilter(NFW.STATUS_BAR_FILTER);

            XposedHelpers.setAdditionalInstanceField(decorView, "IGNORE_NEXT_UP", false);
            gestureDetector = new GestureDetector(mDecorView.getContext(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    mHelper.performAction(NFW.ACTION_SMALL_SCREEN);
//                mHelper.performAction(NFW.ACTION_MOVABLE_SCREEN);
                    return true;
                }
            });
        }

        @Override
        protected void actionOnFling() {
            // Do nothing
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (!mHelper.isResized() && mHelper.staysHome()) {
                if (gestureDetector.onTouchEvent(event)) {
                    /**
                     *  Why do we need to ignore next ACTION_UP?
                     *  Double tap takes effect in the next ACTION_DOWN event.
                     *  If we don't ignore the coming ACTION_UP, systemui will respond to it and collapse the notification panel.
                     *  That's not what I want.
                     */
                    XposedHelpers.setAdditionalInstanceField(mDecorView, "IGNORE_NEXT_UP", true);
                    return true;
                }
            }


            // ------------------------------------
            try {
                if (mHelper.onTouchEvent(event)) {
                    XposedHelpers.setAdditionalInstanceField(mDecorView, "IGNORE_NEXT_UP", true);
                    return true;
                }
            } catch (Throwable t) {
                logE(t);
            }

            /**
             * We want to block till the up event comes.
             */
            boolean ignoreUp = (boolean) XposedHelpers.getAdditionalInstanceField(mDecorView, "IGNORE_NEXT_UP");
            if (ignoreUp && event.getAction() != MotionEvent.ACTION_UP) {
                return true;
            }
            if (ignoreUp && event.getAction() == MotionEvent.ACTION_UP) {
                XposedHelpers.setAdditionalInstanceField(mDecorView, "IGNORE_NEXT_UP", false);
                ignoreUp = (boolean) XposedHelpers.getAdditionalInstanceField(mDecorView, "IGNORE_NEXT_UP");
                return true;
            }
            return false;
        }

        @Override
        public void dealWithPersistentIn() {
            // Do nothing
        }

        @Override
        public void dealWithPersistentOut() {
            mHelper.resetState(true);
            mHelper.sendLocalScreenData();
        }

        public void draw(Canvas canvas) {
            mHelper.draw(canvas);
        }
    }

    private IFlyingHandler mHandler;
    private static final String CLASS_PHONE_STATUS_BAR_VIEW = "com.android.systemui.statusbar.phone.PhoneStatusBarView";
    private static final String CLASS_NOTIFICATION_PANEL_VIEW = "com.android.systemui.statusbar.phone.NotificationPanelView";
    private static final String CLASS_SCRIM_CONTROLLER = "com.android.systemui.statusbar.phone.ScrimController";

    private final String classNameStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return "com.android.systemui.statusbar.phone.StatusBar";
        } else {
            return "com.android.systemui.statusbar.phone.PhoneStatusBar";
        }
    }

    private final String classNamePanelHolder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return "com.android.systemui.statusbar.phone.NotificationsQuickSettingsContainer";
        } else {
            return "com.android.systemui.statusbar.phone.PanelHolder";
        }
    }

    private String methodNameOnPanelCollapsed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return "onPanelCollapsed";
        } else {
            return "onAllPanelsCollapsed";
        }
    }

    public void install(ClassLoader classLoader) {
        final Class<?> classPanelHolder = XposedHelpers.findClass(classNamePanelHolder(), classLoader);
        final Class<?> classPhoneStatusBarView = XposedHelpers.findClass(CLASS_PHONE_STATUS_BAR_VIEW, classLoader);
        final Class<?> classPanelView = XposedHelpers.findClass(CLASS_NOTIFICATION_PANEL_VIEW, classLoader);
        final Class<?> classStatusBar = XposedHelpers.findClass(classNameStatusBar(), classLoader);
        final Class<?> classScrim = XposedHelpers.findClass(CLASS_SCRIM_CONTROLLER, classLoader);
        XposedBridge.hookAllConstructors(classPanelHolder, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                try {
                    final FrameLayout panelHolder = (FrameLayout) param.thisObject;
                    mHandler = createFlyingHandler(panelHolder);

                    /**
                     * TODO:
                     *  1. We can try using onPanelPeeked to register the receivers and unregister them in onPanelCollapsed
                     *  2. We can try merging the mGlobalReceiver with ActionReceiver class.
                     */
                    mHandler.registerReceiver();
                    mHandler.dealWithPersistentIn();
                    log("attached to status bar");
                } catch (Throwable t) {
                    logE(t);
                }
            }
        });

        XposedHelpers.findAndHookMethod(classPhoneStatusBarView, methodNameOnPanelCollapsed(),
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        mHandler.unregisterReceiver();
                        mHandler.dealWithPersistentOut();
                    }
                });

        XposedBridge.hookAllMethods(classPanelView, "onHeadsUpStateChanged", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                boolean show = (boolean) param.args[1];
                if (show) {
                    mHandler.unregisterReceiver();
                } else {
                    mHandler.registerReceiver();
                }
            }
        });

        XposedBridge.hookAllMethods(classStatusBar, "onScreenTurnedOff", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                mHelper.resetState(true);
//                mHelper.sendLocalScreenData();
                mHandler.unregisterReceiver();
                mHandler.dealWithPersistentOut();
            }
        });

        XposedHelpers.findAndHookMethod(classPanelView, "dispatchDraw", Canvas.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            final Canvas canvas = (Canvas) param.args[0];
                            mHandler.draw(canvas);
                        } catch (Throwable t) {
                            logE(t);
                        }
                    }
                });
        XposedBridge.hookAllMethods(classScrim, "setExcludedBackgroundArea", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.args[0] = new Rect(0, 0, 0, 0);
            }
        });

        XposedBridge.hookAllConstructors(classPanelView, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                View v = (View) param.thisObject;
                v.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        KeyguardManager mKeyguardManager = (KeyguardManager) v.getContext().getSystemService(KEYGUARD_SERVICE);
                        if (mKeyguardManager.inKeyguardRestrictedInputMode()) {
                            return false;
                        }
                        if (mHandler.onTouchEvent(event)) {
                            return true;
                        }
                        return false;
                    }
                });
            }
        });
    }
}
