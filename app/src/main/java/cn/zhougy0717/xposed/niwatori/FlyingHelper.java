package cn.zhougy0717.xposed.niwatori;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import cn.zhougy0717.xposed.niwatori.app.ChangeSettingsActionReceiver;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import jp.tkgktyk.flyinglayout.FlyingLayout;

/**
 * Created by tkgktyk on 2015/02/13.
 */
public class FlyingHelper extends FlyingLayout.Helper {
    private static final String TAG = FlyingHelper.class.getSimpleName();
    public static final String TEMP_SCREEN_INFO_PREF_FILENAME = "temp_screen_info";
    public static final float SMALLEST_SMALL_SCREEN_SIZE = 0.4f;
    public static final float BIGGEST_SMALL_SCREEN_SIZE = 0.9f;
    public static final float SMALL_SCREEN_SIZE_DELTA = 0.04f;
    public static final int SMALL_SCREEN_MARGIN_X = 4;
    public static final int SMALL_SCREEN_MARGIN_Y = 8;

    private final InputMethodManager mInputMethodManager;
//    private NFW.Settings mSettings;
    public Queue<Runnable> mLayoutCallbacks;

    private final GradientDrawable mBoundaryDrawable = NFW.makeBoundaryDrawable(0, 0);
    private int mBoundaryWidth;

    public FlyingHelper(FrameLayout view, int frameLayoutHierarchy, boolean useContainer/*,
                        NFW.Settings settings*/) throws NoSuchMethodException {
        super(view, frameLayoutHierarchy);

        mInputMethodManager = (InputMethodManager) view.getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        Settings settings = WorldReadablePreference.getSettings();
        initialize(useContainer, settings);

        mLayoutCallbacks = new LinkedList<Runnable>();
    }

    private void initialize(boolean useContainer, Settings settings) {
        final Context niwatoriContext = NFW.getNiwatoriContext(getAttachedView().getContext());
        if (niwatoriContext != null) {
            mBoundaryWidth = Math.round(niwatoriContext.getResources().getDimension(R.dimen.boundary_width));
            // flying padding
            final int padding = Math.round(niwatoriContext.getResources()
                    .getDimension(R.dimen.flying_view_padding));
            setHorizontalPadding(padding);
            setVerticalPadding(padding);
        }
        setTouchEventEnabled(false);
        setUseContainer(useContainer);
        setOnFlyingEventListener(new MySimpleOnFlyingEventListener(this));

        onSettingsLoaded(settings);
    }

    public void onSettingsLoaded() {
        setSpeed(getSettings().speed);
        float smallScreenPivotX = getSettings().getSmallScreenPivotX();
        float smallScreenPivotY = getSettings().getSmallScreenPivotY();
        float smallScreenSize = getSettings().getSmallScreenSize();
        setPivot(smallScreenPivotX, smallScreenPivotY);
        if (getSettings().anotherResizeMethodTargets.contains(getAttachedView().getContext().getPackageName())) {
            setResizeMode(FlyingLayout.RESIZE_MODE_PADDING);
        } else {
            setResizeMode(FlyingLayout.RESIZE_MODE_SCALE);
        }
        if (isResized() && !isMovable()) {
            setScale(smallScreenSize);
        }
        else if (getSettings().smallScreenPersistent && getSettings().screenResized
                && !isResized() && !isMovable()){
            goHomeWithMargin();
            resize(true);
        }
        else if (getSettings().smallScreenPersistent && !getSettings().screenResized
                && isResized() && !isMovable()){
            goHome(getSettings().animation);
            resize(false);
        }
        updateBoundary();
        getAttachedView().post(new Runnable() {
            @Override
            public void run() {
                getAttachedView().requestLayout();
            }
        });
    }

    public void onSettingsLoaded(Settings settings) {
        onSettingsLoaded();
    }

    public Settings getRemoteSettings() {
        return WorldReadablePreference.getSettings();
    }
    public Settings getSettings() {
//        return mSettings;
        try {
            SharedPreferences prefs = getAttachedView().getContext().getSharedPreferences(TEMP_SCREEN_INFO_PREF_FILENAME, 0);
            return WorldReadablePreference.getSettings().update(prefs);
        }
        catch (Throwable t) {
            return WorldReadablePreference.getSettings();
        }
    }

    private void updateBoundaryOnUnresize() {
        if (isMovable()) {
            mBoundaryDrawable.setStroke(mBoundaryWidth, getSettings().boundaryColorMS);
        } else {
            mBoundaryDrawable.setStroke(mBoundaryWidth, Color.TRANSPARENT);
        }
        getAttachedView().postInvalidate();
    }

    private void updateBoundaryOnResize() {
        if (isMovable()) {
            mBoundaryDrawable.setStroke(mBoundaryWidth, getSettings().boundaryColorMS);
        } else {
            mBoundaryDrawable.setStroke(mBoundaryWidth, getSettings().boundaryColorSS);
        }
        getAttachedView().postInvalidate();
    }

    private void updateBoundary() {
        if (isMovable()) {
            mBoundaryDrawable.setStroke(mBoundaryWidth, getSettings().boundaryColorMS);
        } else if (isResized()) {
            mBoundaryDrawable.setStroke(mBoundaryWidth, getSettings().boundaryColorSS);
        } else {
            mBoundaryDrawable.setStroke(mBoundaryWidth, Color.TRANSPARENT);
        }
        getAttachedView().postInvalidate();
    }

    public boolean isMovable() {
        return getTouchEventEnabled();
    }

    private void disableMovable() {
        setTouchEventEnabled(false);
        updateBoundary();
    }

    private void enableMovable() {
        setTouchEventEnabled(true);
        updateBoundary();
    }

    public void performExtraAction() {
        final String action = getSettings().extraAction;
        if (getSettings().logActions) {
            XposedBridge.log(action);
        }
        if (action.equals(NFW.ACTION_MOVABLE_SCREEN)) {
            forceMovable();
            updateBoundary();
        } else if (action.equals(NFW.ACTION_PIN_OR_RESET)) {
            forcePinOrReset();
            updateBoundary();
        } else if (action.equals(NFW.ACTION_SMALL_SCREEN)) {
            if (!isResized()) {
                resize(true);
            }
        }
    }

    public void performAction(String action) {
        if (getSettings().logActions) {
            XposedBridge.log(getAttachedView().getContext().getPackageName() + " do action: " + action);
            Log.e(TAG, getAttachedView().getContext().getPackageName() + " do action: " + action);
        }
        if (action.equals(NFW.ACTION_RESET)) {
            resetState(true);
        } else if (action.equals(NFW.ACTION_SOFT_RESET)) {
            resetState(false);
        } else if (action.equals(NFW.ACTION_MOVABLE_SCREEN)) {
            toggleMovable();
        } else if (action.equals(NFW.ACTION_PIN)) {
            pin();
        } else if (action.equals(NFW.ACTION_PIN_OR_RESET)) {
            pinOrReset();
        } else if (action.equals(NFW.ACTION_SMALL_SCREEN)) {
            resize();
        } else if (action.equals(NFW.ACTION_FORCE_SMALL_SCREEN)) {
            if (!isResized()) {
                resize(true);
            }
            else {
                goHomeWithMargin();
                onSettingsLoaded();
            }
        } else if (action.equals(NFW.ACTION_EXTRA_ACTION)) {
            performAction(getSettings().extraAction);
        } else if (action.equals(NFW.ACTION_CS_SWAP_LEFT_RIGHT)) {
            SharedPreferences prefs = getAttachedView().getContext().getSharedPreferences(TEMP_SCREEN_INFO_PREF_FILENAME, 0);
            int pivotX = 100 - (int)(100*getSettings().getSmallScreenPivotX());
            prefs.edit()
                    .putInt("key_small_screen_pivot_x", pivotX)
                    .apply();
            goHomeWithMargin();
            onSettingsLoaded();
        }
    }

    private void toggleMovable() {
        if (isMovable()) {
            disableMovable();
            if (isResized()){
                goHomeWithMargin();
            }
            else {
                goHome(getSettings().animation);
            }
        } else {
            forceMovable();
        }
        updateBoundary();
    }

    private void forceMovable() {
        if (!isResized() && staysHome()) {
            moveToInitialPosition(false);
            hideSoftInputMethod();
        }
        enableMovable();
    }

    public void pin() {
        if (!isResized() && staysHome()) {
            moveToInitialPosition(true);
            hideSoftInputMethod();
            disableMovable();
        } else if (isMovable()) {
            disableMovable();
        } else {
            enableMovable();
            hideSoftInputMethod();
        }
        updateBoundary();
    }

    private void pinOrReset() {
        if (staysHome() || (isResized()&&staysHomeWithMargin())) {
            forcePinOrReset();
        } else {
            if (isResized()) {
                goHomeWithMargin();
            }
            else {
                goHome(getSettings().animation);
            }
            disableMovable();
        }
        updateBoundary();
    }

    private void forcePinOrReset() {
        moveToInitialPosition(true);
        hideSoftInputMethod();
        disableMovable();
    }

    public void resize() {
        resize(!isResized());
    }

    public void resize(boolean force) {
        setPivot(getSettings().smallScreenPivotX, getSettings().smallScreenPivotY);
        if (WorldReadablePreference.getSettings().smallScreenPersistent) {
//            NFW.setResizedGlobal(getAttachedView().getContext(), force);
            SharedPreferences prefs = getAttachedView().getContext().getSharedPreferences(TEMP_SCREEN_INFO_PREF_FILENAME, 0);
            prefs.edit().putBoolean("screen_resized", force).apply();
        }
//        SharedPreferences prefs = getAttachedView().getContext().getSharedPreferences(TEMP_SCREEN_INFO_PREF_FILENAME, 0);
//        if (getSettings().getSmallScreenPivotX() < 0.5) {
//            prefs.edit()
//                    .putInt("key_small_screen_pivot_x", 0)
//                    .putInt("key_initial_x_percent", -2*FlyingHelper.SMALL_SCREEN_MARGIN_X)
//                    .apply();
//        }
//        else {
//            prefs.edit()
//                    .putInt("key_small_screen_pivot_x", 100)
//                    .putInt("key_initial_x_percent", 2*FlyingHelper.SMALL_SCREEN_MARGIN_X)
//                    .apply();
//        }
//        if (force) {
//            Log.e("Ben", "initial x in resize: " + getSettings().initialXp);
//            moveToInitialPosition(false);
//        }
//        else {
//            prefs.edit().remove("key_initial_x_percent").apply();
//        }
        if (force) {
            goHomeWithMargin();
            forceResize();
            updateBoundaryOnResize();
        } else {
            goHome(getSettings().animation);
            super.resize(FlyingLayout.DEFAULT_SCALE, getSettings().animation);
            updateBoundaryOnUnresize();
        }
    }

    private void forceResize() {
//        if (getPivotX() == getSettings().smallScreenPivotX
//                && getPivotY() == getSettings().smallScreenPivotY) {
//            setPivot(getSettings().smallScreenPivotX, getSettings().smallScreenPivotY);
//        }
        super.resize(getSettings().smallScreenSize, getSettings().animation);
        hideSoftInputMethod();
    }

    private boolean moveToInitialPosition(boolean pin) {
        final InitialPosition pos = new InitialPosition(getSettings().initialXp, getSettings().initialYp);
        if (pin && pos.getXp() == 0 && pos.getYp() == 0) {
            // default position for pin
            pos.setXp(0); // 0%
            pos.setYp(Math.round(50*getScale())); // 50%
        }
        final int x = pos.getX(getAttachedView());
        final int y = pos.getY(getAttachedView());
        boolean moved = false;
        if (x != 0 || y != 0) {
            moved = true;
            moveWithoutSpeed(x, y, getSettings().animation);
        }
        return moved;
    }

    private boolean staysHomeWithMargin(){
        boolean left = (getSettings().getSmallScreenPivotX() < 0.5);
        return getOffsetX()== (left?1:-1)* SMALL_SCREEN_MARGIN_X *getAttachedView().getWidth()/100 &&
                getOffsetY() == -SMALL_SCREEN_MARGIN_Y *getAttachedView().getHeight()/100;
    }

    private void goHomeWithMarginWithLayoutInfo() {
        boolean left = (getSettings().getSmallScreenPivotX() < 0.5);
        boolean animation = getSettings().animation;
        int marginX = (left ? 1 : -1) * SMALL_SCREEN_MARGIN_X * getAttachedView().getWidth() / 100;
        int marginY = -SMALL_SCREEN_MARGIN_Y * getAttachedView().getHeight() / 100;
        goTo(marginX, marginY);
    }

    private void goTo(int x, int y) {
        int deltaX = x - getOffsetX();
        int deltaY = y - getOffsetY();
        moveWithoutSpeed(deltaX, deltaY, getSettings().animation);
    }

    private void goHomeWithMargin(){
        if (getAttachedView().getWidth() == 0 && getAttachedView().getHeight() == 0) {
            mLayoutCallbacks.add(new Runnable() {
                @Override
                public void run() {
                    goHomeWithMarginWithLayoutInfo();
                }
            });
        }
        else {
            goHomeWithMarginWithLayoutInfo();
        }
    }

    public void resetState(boolean force) {
        boolean handled = false;
        if (isMovable()) {
            disableMovable();
            // goHome must be placed after pin() for "Reset when collapsed"
            // option.
            if (isResized()){
                if(!staysHomeWithMargin()) {
                    goHomeWithMargin();
                }
                handled = true;
            }
            else if (!staysHome()) {
                goHome(getSettings().animation);
                handled = true;
            }
        }
        if ((force || !handled) && isResized()) {
            if (!staysHomeWithMargin()){
                goHomeWithMargin();
            }
            else {
                goHome(getSettings().animation);
                resize(false);
            }
        }
    }

    public void onExit() {
        Intent intent = new Intent(NFW.getNiwatoriContext(getAttachedView().getContext()), ChangeSettingsActionReceiver.class);

        SharedPreferences prefs = getAttachedView().getContext().getSharedPreferences(TEMP_SCREEN_INFO_PREF_FILENAME, Context.MODE_PRIVATE);
        int size = Math.round(100*getSettings().getSmallScreenSize());
        int pivotX = Math.round(100*getSettings().getSmallScreenPivotX());
        int pivotY = Math.round(100*getSettings().getSmallScreenPivotY());
        boolean resized = getSettings().screenResized;
        boolean save =  false;
        if (size != Math.round(100*getRemoteSettings().getSmallScreenSize())) {
            intent.putExtra("key_small_screen_size", size);
            save = true;
        }
        if (pivotX != Math.round(100*getRemoteSettings().getSmallScreenPivotX())) {
            intent.putExtra("key_small_screen_pivot_x", pivotX);
            save = true;
        }
        if (pivotY != Math.round(100*getRemoteSettings().getSmallScreenPivotY())) {
            intent.putExtra("key_small_screen_pivot_y", pivotY);
            save = true;
        }
        if (resized != getRemoteSettings().screenResized){
            intent.putExtra("screen_resized", resized);
            save = true;
        }
        if (save) {
            getAttachedView().getContext().sendBroadcast(intent);
        }
//        intent.putExtra("key_small_screen_size", Math.round(100*getSettings().getSmallScreenSize()));
//        intent.putExtra("key_small_screen_pivot_x", Math.round(100*getSettings().getSmallScreenPivotX()));
//        intent.putExtra("key_small_screen_pivot_y", Math.round(100*getSettings().getSmallScreenPivotY()));
//        getAttachedView().getContext().sendBroadcast(intent);
        prefs.edit().clear().apply();
    }

    private void hideSoftInputMethod() {
        mInputMethodManager.hideSoftInputFromWindow(getAttachedView().getWindowToken(), 0);
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Object mForegroundInfo = XposedHelpers.getObjectField(getAttachedView(), "mForegroundInfo");
            if (mForegroundInfo != null) {
                XposedHelpers.setBooleanField(mForegroundInfo, "mBoundsChanged", true);
            }
        } else {
            XposedHelpers.setBooleanField(getAttachedView(), "mForegroundBoundsChanged", true);
        }
    }

    public void draw(Canvas canvas) {
        mBoundaryDrawable.setBounds(getBoundaryRect());
        mBoundaryDrawable.draw(canvas);
    }

    @SuppressLint("NewApi")
    public void setForeground(View decorview) {
        decorview.setForeground(mBoundaryDrawable);
    }

}