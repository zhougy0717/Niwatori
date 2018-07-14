package cn.zhougy0717.xposed.niwatori.handlers;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.PopupWindow;

import java.util.LinkedList;
import java.util.Queue;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class PopupWindowHandler extends BaseHandler {
    private static IFlyingHandler mActiveHandler = null;
    public static Queue<Runnable> mLayoutCallbacks = new LinkedList<Runnable>();;

    @Override
    final protected IFlyingHandler allocateHandler(FrameLayout decorView) {
        return new CustomizedHandler(decorView);
    }

    @Override
    final protected IFlyingHandler allocateHandler(Object obj){
        return new CustomizedHandler((PopupWindow)obj);
    }

    @Override
    final protected FrameLayout getDecorView(Object obj){
        return (FrameLayout) XposedHelpers.getObjectField(obj, "mDecorView");
    }

    private static class CustomizedHandler extends FlyingHandler {
        private PopupWindow mPopupWindow;
        private CustomizedHandler(PopupWindow pw) {
            this((FrameLayout) XposedHelpers.getObjectField(pw, "mDecorView"));
            mPopupWindow = pw;
        }

        @Override
        protected void actionOnFling() {
            // Do nothing
            WindowManager.LayoutParams p = (WindowManager.LayoutParams) mDecorView.getLayoutParams();
            boolean left = mHelper.getSettings().getSmallScreenPivotX() < 0.5;
            int gravity = left ? Gravity.LEFT : Gravity.RIGHT;
            XposedHelpers.setIntField(mPopupWindow, "mGravity", Gravity.BOTTOM | gravity);
            p.x = 0;
            mDecorView.setLayoutParams(p);
            XposedHelpers.callMethod(mPopupWindow, "update");
        }

        private CustomizedHandler(FrameLayout decorView) {
            super(decorView);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event){
            if(mEdgeGesture.onTouchEvent(event)) {
                return true;
            }
            return mHelper.onTouchEvent(event);
        }
    }

    public void install() {
        //
        // register receiver
        //
        XposedBridge.hookAllMethods(PopupWindow.class, "invokePopup", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                final PopupWindow pw = (PopupWindow) param.thisObject;
                final FrameLayout decor = (FrameLayout) XposedHelpers.getObjectField(pw, "mDecorView");

                mLayoutCallbacks.add(new Runnable() {
                    @Override
                    public void run() {
                        DisplayMetrics dm = decor.getContext().getResources().getDisplayMetrics();
                        if (((double)decor.getWidth()/dm.widthPixels <= 0.4) && ((double)decor.getHeight()/dm.heightPixels <= 0.4)) {
                            return;
                        }

                        try {
                            mActiveHandler = createFlyingHandler(pw);
                            mActiveHandler.registerReceiver();
                            mActiveHandler.dealWithPersistentIn();
                        } catch (Throwable t) {
                            logE(t);
                        }
                    }
                });
                decor.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){
                    @Override
                    public void onGlobalLayout() {
                        while(!mLayoutCallbacks.isEmpty()) {
                            Runnable r = mLayoutCallbacks.poll();
                            r.run();
                        }
                    }
                });
            }
        });

        XposedBridge.hookAllMethods(PopupWindow.class, "dismiss", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (mActiveHandler != null) {
                    mActiveHandler.dealWithPersistentOut();
                    mActiveHandler.unregisterReceiver();
                    mActiveHandler = null;
                }
            }
        });

        final Class<?> classPopupDecorView = XposedHelpers.findClass("android.widget.PopupWindow$PopupDecorView", null);
        XposedHelpers.findAndHookMethod(classPopupDecorView, "dispatchTouchEvent", MotionEvent.class,
                new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                        try {
                            final MotionEvent event = (MotionEvent) methodHookParam.args[0];
                            if (mActiveHandler != null) {
                                mActiveHandler.onTouchEvent(event);
                                if ((event.getAction() == MotionEvent.ACTION_DOWN) && mActiveHandler.edgeDetected(event)) {
                                    // We want to hijack ACTION_DOWN on edge. Beucase ACTION_DOWN will dismiss the dialog.
                                    return false;
                                }
                            }
                        } catch (Throwable t) {
                            logE(t);
                        }
                        return invokeOriginalMethod(methodHookParam);
                    }
                });
    }

    public static void onActivityPause(){
        // TODO: save the handler under the activity's name
        if(mActiveHandler != null){
            mActiveHandler.unregisterReceiver();
            mActiveHandler.dealWithPersistentOut();
        }
    }

    public static void onActivityResume(Activity activity){
        // TODO: check the existing handler using the activity's class name
        mCurrentActivity = activity;
        if(mActiveHandler != null){
            mActiveHandler.registerReceiver();
            mActiveHandler.dealWithPersistentIn();
        }
    }
}
