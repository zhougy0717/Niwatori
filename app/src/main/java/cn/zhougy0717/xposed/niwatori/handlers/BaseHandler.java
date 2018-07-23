package cn.zhougy0717.xposed.niwatori.handlers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Canvas;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import cn.zhougy0717.xposed.niwatori.ActionReceiver;
import cn.zhougy0717.xposed.niwatori.FlyingHelper;
import cn.zhougy0717.xposed.niwatori.IReceiver;
import cn.zhougy0717.xposed.niwatori.ModActivity;
import cn.zhougy0717.xposed.niwatori.NFW;
import cn.zhougy0717.xposed.niwatori.SettingsLoadReceiver;
import cn.zhougy0717.xposed.niwatori.XposedModule;

import de.robv.android.xposed.XposedHelpers;

/**
 * Created by zhougua on 1/11/2018.
 */

public abstract class BaseHandler extends XposedModule {
    protected static final String FIELD_FLYING_HANDLER = NFW.NAME + "_flyingHandler";

    protected static Activity mCurrentActivity = null;

    protected interface IFlyingHandler {
        public void registerReceiver();
        public void unregisterReceiver();
        public void dealWithPersistentIn();
        public void dealWithPersistentOut();
        public boolean edgeDetected(MotionEvent event);
        public boolean onTouchEvent(MotionEvent event);

        // Below are only used in Activity Handler
        public boolean onInterceptTouchEvent(MotionEvent event);
        public void draw(Canvas canvas);
        public void rotate();
    }

    // TODO: Use STRATEGY model to have dependency inject.
    abstract protected IFlyingHandler allocateHandler(FrameLayout decorView);
    abstract protected IFlyingHandler allocateHandler(Object obj);
    abstract protected FrameLayout getDecorView(Object obj);

    protected IFlyingHandler createFlyingHandler(Object obj) {
        FrameLayout decorView = getDecorView(obj);
        IFlyingHandler handler = (FlyingHandler) XposedHelpers.getAdditionalInstanceField(decorView, FIELD_FLYING_HANDLER);
        if (handler == null) {
            handler = allocateHandler(obj);
            XposedHelpers.setAdditionalInstanceField(decorView, FIELD_FLYING_HANDLER, handler);
        }
        return handler;
    }

    /**
     * Don't remove this.
     * Sometimes we don't have clear relationship between DecorView and its owner.
     * So it's easy to just create handler based on DecorView.
     */
    protected IFlyingHandler createFlyingHandler(FrameLayout decorView) {
        IFlyingHandler handler = (FlyingHandler) XposedHelpers.getAdditionalInstanceField(decorView, FIELD_FLYING_HANDLER);
        if (handler == null) {
            handler = allocateHandler(decorView);
            XposedHelpers.setAdditionalInstanceField(decorView, FIELD_FLYING_HANDLER, handler);
        }
        return handler;
    }

    protected static abstract class FlyingHandler implements IFlyingHandler{
        protected FrameLayout mDecorView;
        protected FlyingHelper mHelper;
        protected IReceiver mActionReceiver;
        protected IReceiver mSettingsLoadedReceiver;
        protected GestureDetector mEdgeGesture;

        protected abstract void actionOnFling();
        public abstract boolean onTouchEvent(MotionEvent event);
        public abstract void dealWithPersistentIn();
        public abstract void dealWithPersistentOut();


        protected FlyingHandler(FrameLayout decorView){
            mDecorView = decorView;
            mHelper = ModActivity.createFlyingHelper(mDecorView);
            mActionReceiver = ActionReceiver.getInstance(mDecorView, NFW.FOCUSED_DIALOG_FILTER);
            mSettingsLoadedReceiver = SettingsLoadReceiver.getInstance(mDecorView, NFW.SETTINGS_CHANGED_FILTER);

            mEdgeGesture = new GestureDetector(mDecorView.getContext(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onFling(MotionEvent event1, MotionEvent event2,float velocityX, float velocityY) {
                    if (!mHelper.edgeDetected(event1)) {
                        return false;
                    }
                    if (mHelper.isResized()){
                        return false;
                    }
                    if (Math.abs(event1.getY() - event2.getY()) <= mDecorView.getHeight()*0.08) {
                        return false;
                    }
                    actionOnFling();
                    NFW.performAction(mDecorView.getContext(), NFW.ACTION_SMALL_SCREEN);
                    return true;
                }

                @Override
                public boolean onDown(MotionEvent event){
                    if (!edgeDetected(event)) {
                        return false;
                    }
                    return true;
                }
            });
        }

        public void registerReceiver(){
            mActionReceiver.register();
            mSettingsLoadedReceiver.register();
        }

        public void unregisterReceiver(){
            mActionReceiver.unregister();
            mSettingsLoadedReceiver.unregister();
        }

        public boolean edgeDetected(MotionEvent event){
            return mHelper.edgeDetected(event);
        }

        public boolean onInterceptTouchEvent(MotionEvent event) {
            return false;
        }
        public void draw(Canvas canvas) {
            // Do nothing
        }
        public void rotate() {
            // Do nothing
        }
    }

    public interface IFloatingWindowHandler extends IFlyingHandler{
        public void setSwitchFromOutside(boolean fromOutside);
        public boolean isSwitchFromOutside();
        public void switchFromOutside();
        public void switchFromActivity();
    }
    protected static abstract class FloatingWindowHandler extends FlyingHandler implements IFlyingHandler, IFloatingWindowHandler{
        private boolean mFromOutside = false;
        protected FloatingWindowHandler(FrameLayout decorView) {
            super(decorView);
        }

        private void syncWithActivity(Activity activity) {
            FlyingHelper helper = ModActivity.getHelper((FrameLayout) activity.getWindow().peekDecorView());
            mHelper.syncResize(helper.isResized());
        }

        private void syncWithNiwatori(){
            mHelper.syncResize(mHelper.getSettings().screenResized);
        }

        @Override
        public void setSwitchFromOutside(boolean fromOutside){
            mFromOutside = fromOutside;
        }

        @Override
        public boolean isSwitchFromOutside(){
            return mFromOutside;
        }
        @Override
        public void switchFromOutside(){
            syncWithNiwatori();
        }

        @Override
        public void switchFromActivity(){
            if (mCurrentActivity != null) {
                syncWithActivity(mCurrentActivity);
            }
        }
        @Override
        public void dealWithPersistentIn(){
            switchFromActivity();
        }

        @Override
        public void dealWithPersistentOut() {
            if (mHelper!=null && !mHelper.getSettings().smallScreenPersistent) {
                // NOTE: When fire actions from shortcut (ActionActivity), it causes onPause and onActivityResume events
                // because through an Activity. So shouldn't reset automatically.
                mHelper.resetState(true);
            }
            if (mCurrentActivity != null) {
                FlyingHelper helper = ModActivity.getHelper((FrameLayout) mCurrentActivity.getWindow().peekDecorView());
                helper.syncResize(mHelper.isResized());
            }
        }
    }
}
