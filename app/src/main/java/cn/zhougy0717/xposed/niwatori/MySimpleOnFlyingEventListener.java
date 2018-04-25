package cn.zhougy0717.xposed.niwatori;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;

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

    private void changeSize(ViewGroup v, int delta){
        int w = v.getWidth();
        int h = v.getHeight();

        int smallScreenSize = (int)(100*mHelper.getSettings().smallScreenSize);
        int smallScreenPivotX = (int)(100*mHelper.getSettings().smallScreenPivotX);
        int smallScreenPivotY = (int)(100*mHelper.getSettings().smallScreenPivotY);
        SharedPreferences prefs = v.getContext().getSharedPreferences(FlyingHelper.TEMP_SCREEN_INFO_PREF_FILENAME, Context.MODE_PRIVATE);
        if (prefs.getAll().keySet().contains("key_small_screen_size")) {
            smallScreenSize = prefs.getInt("key_small_screen_size", 70);
        }
        if (prefs.getAll().keySet().contains("key_small_screen_pivot_x")) {
            smallScreenPivotX = prefs.getInt("key_small_screen_pivot_x", 0);
        }
        if (prefs.getAll().keySet().contains("key_small_screen_pivot_y")) {
            smallScreenPivotY = prefs.getInt("key_small_screen_pivot_y", 100);
        }

        int oldSize = smallScreenSize;
        smallScreenSize += delta;
        if (smallScreenSize < FlyingHelper.SMALLEST_SMALL_SCREEN_SIZE) {
            smallScreenSize = FlyingHelper.SMALLEST_SMALL_SCREEN_SIZE;
        }
        else if(smallScreenSize > FlyingHelper.BIGGEST_SMALL_SCREEN_SIZE){
            smallScreenSize = FlyingHelper.BIGGEST_SMALL_SCREEN_SIZE;
        }
        int realDelta = smallScreenSize - oldSize;
        prefs.edit().putInt("key_small_screen_size", smallScreenSize).apply();
        mHelper.onSettingsLoaded();
        if (smallScreenPivotX < 50) {
            mHelper.moveWithoutSpeed(w*realDelta*smallScreenPivotX/(100*100),-h*realDelta*(100-smallScreenPivotY)/(100*100),false);
        }
        else {
            mHelper.moveWithoutSpeed(-w*realDelta*(100-smallScreenPivotX)/(100*100),-h*realDelta*(100-smallScreenPivotY)/(100*100),false);
        }
    }

    @Override
    public void onScrollLeft(ViewGroup v){
        changeSize(v, -FlyingHelper.SMALL_SCREEN_SIZE_DELTA);
    }

    @Override
    public void onScrollRight(ViewGroup v){
        changeSize(v, FlyingHelper.SMALL_SCREEN_SIZE_DELTA);
    }
}
