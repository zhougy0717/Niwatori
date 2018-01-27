package jp.tkgktyk.xposed.niwatori;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import jp.tkgktyk.flyinglayout.FlyingLayout;

/**
 * Created by tkgktyk on 2015/02/13.
 */
public class FlyingHelper extends FlyingLayout.Helper {
    private static final String TAG = FlyingHelper.class.getSimpleName();

    private final InputMethodManager mInputMethodManager;
//    private NFW.Settings mSettings;

    private final GradientDrawable mBoundaryDrawable = NFW.makeBoundaryDrawable(0, 0);
    private int mBoundaryWidth;

    public FlyingHelper(FrameLayout view, int frameLayoutHierarchy, boolean useContainer/*,
                        NFW.Settings settings*/) throws NoSuchMethodException {
        super(view, frameLayoutHierarchy);

        mInputMethodManager = (InputMethodManager) view.getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        NFW.Settings settings = WorldReadablePreference.getSettings();
        initialize(useContainer, settings);
    }

    private void initialize(boolean useContainer, NFW.Settings settings) {
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
        setOnFlyingEventListener(new FlyingLayout.SimpleOnFlyingEventListener() {
            @Override
            public void onDragFinished(ViewGroup v) {
                if (getSettings().autoPin) {
                    pin();
                }
            }

            @Override
            public void onClickOutside(ViewGroup v) {
                if (!NFW.isDefaultAction(getSettings().actionWhenTapOutside)) {
                    performAction(getSettings().actionWhenTapOutside);
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
                if (!NFW.isDefaultAction(getSettings().actionWhenDoubleTapOutside)) {
                    performAction(getSettings().actionWhenDoubleTapOutside);
//                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                }
            }
        });

        onSettingsLoaded(settings);
    }

    public void setSettings(NFW.Settings settings) {
//        mSettings = settings;
    }

    public void onSettingsLoaded(NFW.Settings settings) {
//        mSettings = settings;
        setSpeed(getSettings().speed);
        setPivot(getSettings().smallScreenPivotX, getSettings().smallScreenPivotY);
        if (getSettings().anotherResizeMethodTargets.contains(getAttachedView().getContext().getPackageName())) {
            setResizeMode(FlyingLayout.RESIZE_MODE_PADDING);
        } else {
            setResizeMode(FlyingLayout.RESIZE_MODE_SCALE);
        }
        if (isResized()) {
            setScale(getSettings().smallScreenSize);
        }
        updateBoundary();
        getAttachedView().post(new Runnable() {
            @Override
            public void run() {
                getAttachedView().requestLayout();
            }
        });
    }

    public NFW.Settings getSettings() {
//        return mSettings;
        return WorldReadablePreference.getSettings();
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
            Intent intent = new Intent(action);
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            getAttachedView().getContext().sendBroadcast(intent);
        }
    }

    private void toggleMovable() {
        if (isMovable()) {
            disableMovable();
            goHome(getSettings().animation);
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

    private void pin() {
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
        if (staysHome()) {
            forcePinOrReset();
        } else {
            goHome(getSettings().animation);
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
//        if (mSettings.smallScreenPersistent) {
        if (WorldReadablePreference.getSettings().smallScreenPersistent) {
            NFW.setResizedGlobal(getAttachedView().getContext(), force);
        }
        if (force) {
            forceResize();
            updateBoundaryOnResize();
        } else {
//            if (getPivotX() == getSettings().smallScreenPivotX
//                    && getPivotY() == getSettings().smallScreenPivotY) {
            super.resize(FlyingLayout.DEFAULT_SCALE, getSettings().animation);
            updateBoundaryOnUnresize();
//            } else {
//                setPivot(getSettings().smallScreenPivotX, getSettings().smallScreenPivotY);
//                performLayoutAdjustment(getSettings().animation);
//                updateBoundaryOnResize();
//            }
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
            pos.setYp(50); // 50%
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

    public void resetState(boolean force) {
        boolean handled = false;
        if (isMovable() || !staysHome()) {
            disableMovable();
            // goHome must be placed after pin() for "Reset when collapsed"
            // option.
            goHome(getSettings().animation);
            handled = true;
        }
        if ((force || !handled) && isResized()) {
            resize(false);
        }
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

    public void setForeground(View decorview) {
        decorview.setForeground(mBoundaryDrawable);
    }
}