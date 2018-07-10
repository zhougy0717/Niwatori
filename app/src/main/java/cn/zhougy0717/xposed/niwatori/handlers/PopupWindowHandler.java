package cn.zhougy0717.xposed.niwatori.handlers;

import android.app.Activity;
import android.graphics.Rect;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.PopupWindow;

import cn.zhougy0717.xposed.niwatori.NFW;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class PopupWindowHandler extends BaseHandler {
    private static IFlyingHandler mActiveHandler = null;

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
        private CustomizedHandler(PopupWindow pw) {
            this((FrameLayout) XposedHelpers.getObjectField(pw, "mDecorView"));
        }

        @Override
        protected void actionOnFling() {
            // Do nothing
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

                decor.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        int width = decor.getWidth();
                        int height = decor.getHeight();

                        Rect r = new Rect();
                        decor.getWindowVisibleDisplayFrame(r);
                        if (((double)width/(r.right - r.left) <= 0.5) && ((double)height/(r.bottom - r.top) <= 0.5)) {
                            return;
                        }

                        if (mActiveHandler != null){
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
                            final FrameLayout decorView = (FrameLayout) methodHookParam.thisObject;
                            final MotionEvent event = (MotionEvent) methodHookParam.args[0];
                            IFlyingHandler handler = createFlyingHandler(decorView);
                            if (handler.onTouchEvent(event)) {
                                return true;
                            }
                        } catch (Throwable t) {
                            logE(t);
                        }
                        return invokeOriginalMethod(methodHookParam);
                    }
                });
    }

    public static void onPause(Activity activity){
        // TODO: save the handler under the activity's name
        if(mActiveHandler != null){
            mActiveHandler.unregisterReceiver();
            mActiveHandler.dealWithPersistentOut();
        }
    }

    public static void onResume(Activity activity){
        // TODO: check the existing handler using the activity's class name
        mCurrentActivity = activity;
        if(mActiveHandler != null){
            mActiveHandler.registerReceiver();
            mActiveHandler.dealWithPersistentIn();
        }
    }
}
