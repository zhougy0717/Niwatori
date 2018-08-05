package cn.zhougy0717.xposed.niwatori;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.ViewGroup;

import jp.tkgktyk.flyinglayout.FlyingLayout;

class MySimpleOnFlyingEventListener extends FlyingLayout.SimpleOnFlyingEventListener {
    private FlyingHelper mHelper;
    public MySimpleOnFlyingEventListener(FlyingHelper flyingHelper) {
        mHelper = flyingHelper;
    }
    @Override
    public void onDragFinished(ViewGroup v) {
        if (mHelper.getSettings().autoPin) {
            mHelper.pin();
        }
    }

    @Override
    public void onClickOutside(ViewGroup v) {
        if (!NFW.isDefaultAction(mHelper.getSettings().actionWhenTapOutside)) {
            mHelper.performAction(mHelper.getSettings().actionWhenTapOutside);
//                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }
    }

    @Override
    public void onLongPressOutside(ViewGroup v) {
        /**
         * DON'T USE LONG PRESS ACTION
         * --
         * Touch event listener loses the touch event when view is gone by touch event.
         * Then long tap handler is not stopped so its event is fired.
         * ex. the outside of Dialog, status bar shade.
         */
//                if (!NFW.isDefaultAction(getSettings().actionWhenLongPressOutside)) {
//                    performAction(getSettings().actionWhenLongPressOutside);
//                    v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
//                }
    }

    @Override
    public void onDoubleClickOutside(ViewGroup v) {
        if (!NFW.isDefaultAction(mHelper.getSettings().actionWhenDoubleTapOutside)) {
            mHelper.performAction(mHelper.getSettings().actionWhenDoubleTapOutside);
//                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }
    }

    private void changeSize(ViewGroup v, float delta){
        float smallScreenSize = mHelper.getSettings().getSmallScreenSize();

        smallScreenSize += delta;
        if (smallScreenSize < FlyingHelper.SMALLEST_SMALL_SCREEN_SIZE) {
            smallScreenSize = FlyingHelper.SMALLEST_SMALL_SCREEN_SIZE;
        }
        else if(smallScreenSize > FlyingHelper.BIGGEST_SMALL_SCREEN_SIZE){
            smallScreenSize = FlyingHelper.BIGGEST_SMALL_SCREEN_SIZE;
        }
        Settings.ScreenData data = mHelper.getScreenData();
        if (data == null) {
            data = new Settings.ScreenData(mHelper.getSettings());
        }
        data.smallScreenSize = smallScreenSize;
        mHelper.setScreenData(data);
        mHelper.onSettingsLoaded();
    }

    @Override
    public void onShrink(ViewGroup v){
        changeSize(v, -FlyingHelper.SMALL_SCREEN_SIZE_DELTA);
    }

    @Override
    public void onEnlarge(ViewGroup v){
        changeSize(v, FlyingHelper.SMALL_SCREEN_SIZE_DELTA);
    }

    @Override
    public void onFlingUp(ViewGroup v){
        if (!mHelper.isMovable()) {
            mHelper.performAction(NFW.ACTION_SOFT_RESET);
        }
    }

    @Override
    public void onFlingDown(ViewGroup v) {
        if (!mHelper.isMovable()) {
            mHelper.performAction(NFW.ACTION_PIN_OR_RESET);
        }
    }
}
