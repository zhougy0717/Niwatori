package cn.zhougy0717.xposed.niwatori;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

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
    public static final int SMALL_SCREEN_MARGIN = 10;

    private final InputMethodManager mInputMethodManager;
//    private NFW.Settings mSettings;

    private final GradientDrawable mBoundaryDrawable = NFW.makeBoundaryDrawable(0, 0);
    private int mBoundaryWidth;

    public FlyingHelper(FrameLayout view, int frameLayoutHierarchy, boolean useContainer/*,
                        NFW.Settings settings*/) throws NoSuchMethodException {
        super(view, frameLayoutHierarchy);

        mInputMethodManager = (InputMethodManager) view.getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        Settings settings = WorldReadablePreference.getSettings();
        initialize(useContainer, settings);
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

    public void setSettings(Settings settings) {
//        mSettings = settings;
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
            XposedBridge.log(action);
        }
        Log.e("Ben", "action: " + action);
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
        } else if (action.equals(NFW.ACTION_EXTRA_ACTION)) {
            performAction(getSettings().extraAction);
        } else if (action.equals(NFW.ACTION_CS_SWAP_LEFT_RIGHT)) {
            SharedPreferences prefs = getAttachedView().getContext().getSharedPreferences(TEMP_SCREEN_INFO_PREF_FILENAME, 0);
            int pivotX = (int)(100*getSettings().getSmallScreenPivotX());

//            if (getSettings().getSmallScreenPivotX() < 0.5) {
//                prefs.edit()
//                        .putInt("key_small_screen_pivot_x", 0)
//                        .putInt("key_initial_x_percent", FlyingHelper.SMALL_SCREEN_MARGIN)
//                        .apply();
//            }
//            else {
//                prefs.edit()
//                        .putInt("key_small_screen_pivot_x", 100)
//                        .putInt("key_initial_x_percent", -FlyingHelper.SMALL_SCREEN_MARGIN)
//                        .apply();
//            }
//            moveToInitialPosition(false);
            prefs.edit()
                    .putInt("key_small_screen_pivot_x", 100-pivotX)
                    .apply();
            Intent intent = new Intent(NFW.getNiwatoriContext(getAttachedView().getContext()), ChangeSettingsActionReceiver.class);
            intent.putExtra("key_small_screen_pivot_x", 100-pivotX);
            getAttachedView().getContext().sendBroadcast(intent);
            if (pivotX < 0.5) {
                moveWithoutSpeed(-2* SMALL_SCREEN_MARGIN * getAttachedView().getWidth() / 100, -getOffsetY(), false);
            }
            else {
                moveWithoutSpeed(2* SMALL_SCREEN_MARGIN * getAttachedView().getWidth() / 100, -getOffsetY(), false);
            }
            onSettingsLoaded();
        }
    }

    private void toggleMovable() {
        if (isMovable()) {
            disableMovable();
            if (isResized()){
                goHomeWithMargin(getSettings().animation);
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
        Log.e("Ben", "isResized: " + isResized() + ", staysHomeWithMargin:" + staysHomeWithMargin() + ", OffsetX" + getOffsetX());
        if (staysHome() || (isResized()&&staysHomeWithMargin())) {
            forcePinOrReset();
        } else {
            if (isResized()) {
                goHomeWithMargin(getSettings().animation);
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
            NFW.setResizedGlobal(getAttachedView().getContext(), force);
        }
//        SharedPreferences prefs = getAttachedView().getContext().getSharedPreferences(TEMP_SCREEN_INFO_PREF_FILENAME, 0);
//        if (getSettings().getSmallScreenPivotX() < 0.5) {
//            prefs.edit()
//                    .putInt("key_small_screen_pivot_x", 0)
//                    .putInt("key_initial_x_percent", -2*FlyingHelper.SMALL_SCREEN_MARGIN)
//                    .apply();
//        }
//        else {
//            prefs.edit()
//                    .putInt("key_small_screen_pivot_x", 100)
//                    .putInt("key_initial_x_percent", 2*FlyingHelper.SMALL_SCREEN_MARGIN)
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
            goHomeWithMargin(getSettings().animation);
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
        Log.e("Ben", "left:" + left + ", offsetX:"+(left?1:-1)*SMALL_SCREEN_MARGIN*getAttachedView().getWidth()/100);
        return getOffsetX()== (left?1:-1)*SMALL_SCREEN_MARGIN*getAttachedView().getWidth()/100 && getOffsetY()==0;
    }

    private void goHomeWithMargin(boolean animation){
        boolean left = (getSettings().getSmallScreenPivotX() < 0.5);
        moveWithoutSpeed(-getOffsetX() + (left?1:-1)*SMALL_SCREEN_MARGIN * getAttachedView().getWidth() / 100, -getOffsetY(), animation);
    }

    public void resetState(boolean force) {
        boolean handled = false;
        if (isMovable()) {
            disableMovable();
            // goHome must be placed after pin() for "Reset when collapsed"
            // option.
            if (isResized()){
                if(!staysHomeWithMargin()) {
                    goHomeWithMargin(getSettings().animation);
                }
                handled = true;
            }
            else if (!staysHome()) {
                goHome(getSettings().animation);
                handled = true;
            }
        }
        if ((force || !handled) && isResized()) {
            goHome(getSettings().animation);
            resize(false);
        }
    }

    public void onExit() {
        Intent intent = new Intent(NFW.getNiwatoriContext(getAttachedView().getContext()), ChangeSettingsActionReceiver.class);

        SharedPreferences prefs = getAttachedView().getContext().getSharedPreferences(TEMP_SCREEN_INFO_PREF_FILENAME, Context.MODE_PRIVATE);
//        getSettings().update(prefs);
        intent.putExtra("key_small_screen_size", Math.round(100*getSettings().getSmallScreenSize()));
        intent.putExtra("key_small_screen_pivot_x", Math.round(100*getSettings().getSmallScreenPivotX()));
        intent.putExtra("key_small_screen_pivot_y", Math.round(100*getSettings().getSmallScreenPivotY()));
        getAttachedView().getContext().sendBroadcast(intent);
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