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
        Log.e("Ben", "on Drag Finished");
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
        int w = v.getWidth();
        int h = v.getHeight();

        SharedPreferences prefs = v.getContext().getSharedPreferences(FlyingHelper.TEMP_SCREEN_INFO_PREF_FILENAME, Context.MODE_PRIVATE);
        mHelper.getSettings().update(prefs);
        float smallScreenPivotX = mHelper.getSettings().getSmallScreenPivotX();
        float smallScreenPivotY = mHelper.getSettings().getSmallScreenPivotY();
        float smallScreenSize = mHelper.getSettings().getSmallScreenSize();

        float oldSize = smallScreenSize;
        smallScreenSize += delta;
        if (smallScreenSize < FlyingHelper.SMALLEST_SMALL_SCREEN_SIZE) {
            smallScreenSize = FlyingHelper.SMALLEST_SMALL_SCREEN_SIZE;
        }
        else if(smallScreenSize > FlyingHelper.BIGGEST_SMALL_SCREEN_SIZE){
            smallScreenSize = FlyingHelper.BIGGEST_SMALL_SCREEN_SIZE;
        }
        float realDelta = smallScreenSize - oldSize;
        prefs.edit().putInt("key_small_screen_size",Math.round(100*smallScreenSize)).apply();
        Log.e("Ben", "small screen size in local pref " + prefs.getInt("key_small_screen_size", 0));
        mHelper.onSettingsLoaded();
//        if (smallScreenPivotX < 0.5f) {
//            mHelper.moveWithoutSpeed(Math.round(w*realDelta*smallScreenPivotX),Math.round(-h*realDelta*(1-smallScreenPivotY)),false);
//        }
//        else {
//            mHelper.moveWithoutSpeed(Math.round(-w*realDelta*(1-smallScreenPivotX)),Math.round(-h*realDelta*(1-smallScreenPivotY)),false);
//        }
    }

    @Override
    public void onShrink(ViewGroup v){
        changeSize(v, -FlyingHelper.SMALL_SCREEN_SIZE_DELTA);
    }

    @Override
    public void onEnlarge(ViewGroup v){
        changeSize(v, FlyingHelper.SMALL_SCREEN_SIZE_DELTA);
    }
}
