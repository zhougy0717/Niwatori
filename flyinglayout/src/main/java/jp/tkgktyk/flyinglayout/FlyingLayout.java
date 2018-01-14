package jp.tkgktyk.flyinglayout;

import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;

import com.google.common.collect.Lists;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;

/**
 * Created by tkgktyk on 2014/01/01.
 */
public class FlyingLayout extends FrameLayout {

    public static final int RESIZE_MODE_SCALE = 0;
    public static final int RESIZE_MODE_PADDING = 1;

    public static final float DEFAULT_SPEED = 1.5f;
    public static final int DEFAULT_HORIZONTAL_PADDING = 0;
    public static final int DEFAULT_VERTICAL_PADDING = 0;
    public static final boolean DEFAULT_TOUCH_EVENT_ENABLED = true;
    public static final boolean DEFAULT_USE_CONTAINER = false;
    public static final float DEFAULT_SCALE = 1.0f;
    public static final float DEFAULT_PIVOT_X = 0.0f;
    public static final float DEFAULT_PIVOT_Y = 1.0f;
    public static final int DEFAULT_RESIZE_MODE = RESIZE_MODE_SCALE;
    private static final String TAG = FlyingLayout.class.getSimpleName();
    private Helper mHelper;

    public FlyingLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        installHelper();

        fetchAttribute(context, attrs, defStyle);
    }

    public FlyingLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FlyingLayout(Context context) {
        super(context);
        installHelper();
    }

    private void installHelper() {
        int i = 0;
        for (Class<?> cls = this.getClass(); cls != null; cls = cls.getSuperclass()) {
            if (cls.equals(FrameLayout.class)) {
                break;
            }
            ++i;
        }
        mHelper = new Helper(this, i);
    }

    private void fetchAttribute(Context context, AttributeSet attrs,
                                int defStyle) {
        // get attributes specified in XML
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.FlyingLayout, defStyle, 0);
        try {
            mHelper.setSpeed(a.getFloat(R.styleable.FlyingLayout_speed, DEFAULT_SPEED));
            mHelper.setHorizontalPadding(a.getDimensionPixelSize(
                    R.styleable.FlyingLayout_horizontalPadding,
                    DEFAULT_HORIZONTAL_PADDING));
            mHelper.setVerticalPadding(a.getDimensionPixelSize(
                    R.styleable.FlyingLayout_verticalPadding,
                    DEFAULT_VERTICAL_PADDING));
            mHelper.setTouchEventEnabled(a.getBoolean(
                    R.styleable.FlyingLayout_touchEventEnabled,
                    DEFAULT_TOUCH_EVENT_ENABLED));
            mHelper.setUseContainer(a.getBoolean(R.styleable.FlyingLayout_useContainer,
                    DEFAULT_USE_CONTAINER));
        } finally {
            a.recycle();
        }
    }

    public Helper getHelper() {
        return mHelper;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mHelper.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent ev) {
        return mHelper.onTouchEvent(ev);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        mHelper.onLayout(changed, left, top, right, bottom);
    }

//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        mHelper.onMeasure(widthMeasureSpec, heightMeasureSpec);
//    }

    public interface OnFlyingEventListener {

        public void onDragStarted(ViewGroup v);

        /**
         * callback when a moving event is finished.
         *
         * @param v
         */
        public void onDragFinished(ViewGroup v);

        /**
         * callback when click event is happen at outside of contents.
         *
         * @param v
         */
        public void onClickOutside(ViewGroup v);

        /**
         * callback when double click event is happen at outside of contents.
         *
         * @param v
         */
        public void onDoubleClickOutside(ViewGroup v);

        /**
         * callback when long press event is happen at outside of contents.
         *
         * @param v
         */
        public void onLongPressOutside(ViewGroup v);
    }

    public static class Helper {
        /**
         * Sentinel value for no current active pointer. Used by
         * {@link #mActivePointerId}.
         */
        private static final int INVALID_POINTER = -1;
        protected static int DEFAULT_CHILD_GRAVITY = Gravity.TOP | Gravity.START;

        // Hidden Methods
        private final Method mGetPaddingLeftWithForeground; // FrameLayout
        private final Method mGetPaddingTopWithForeground; // FrameLayout
        private final Method mGetPaddingRightWithForeground; // FrameLayout
        private final Method mGetPaddingBottomWithForeground; // FrameLayout
        private final ArrayList<View> mMatchParentChildren = new ArrayList<>(1);
        private final Method mMeasureChildWithMargins; // ViewGroup
        private final Method mGetSuggestedMinimumHeight; // View
        private final Method mGetSuggestedMinimumWidth; // View
        private final Method mSetMeasuredDimension; // View

        private final int mTouchSlop;
        private final Rect mChildRect = new Rect();
        private final Rect mBoundaryRect = new Rect();
        private final GestureDetector mGestureDetector;
        private final TypeEvaluator<Point> mPointEvaluator = new TypeEvaluator<Point>() {
            @Override
            public Point evaluate(float fraction, Point startValue, Point endValue) {
                return new Point(
                        Math.round(startValue.x + (endValue.x - startValue.x) * fraction),
                        Math.round(startValue.y + (endValue.y - startValue.y) * fraction));
            }
        };
        private final TypeEvaluator<PointF> mPointFEvaluator = new TypeEvaluator<PointF>() {
            @Override
            public PointF evaluate(float fraction, PointF startValue, PointF endValue) {
                return new PointF(startValue.x + (endValue.x - startValue.x) * fraction,
                        startValue.y + (endValue.y - startValue.y) * fraction);
            }
        };
        private final ValueAnimator.AnimatorUpdateListener mMoveAnimatorUpdateListener
                = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Point offset = (Point) animation.getAnimatedValue();
                setOffset(offset.x, offset.y);
                mView.requestLayout();
            }
        };
        private final ValueAnimator.AnimatorUpdateListener mChangePivotAnimatorUpdateListener
                = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                PointF pivot = (PointF) animation.getAnimatedValue();
                setChildrenPivot(pivot.x, pivot.y);
                mView.requestLayout();
            }
        };
        private final ValueAnimator.AnimatorUpdateListener mScaleAnimatorUpdateListener
                = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float scale = (Float) animation.getAnimatedValue();
                setScale(scale);
                mView.requestLayout();
            }
        };
        private boolean mUpdatePivotOnTime = true;

        private ArrayList<Rect> mChildrenPaddingBackup;

        /**
         * ID of the active pointer. This is used to retain consistency during
         * drags/flings if multiple pointers are used.
         */
        private int mActivePointerId = INVALID_POINTER;
        /**
         * True if the user is currently dragging this ScrollView around. This is
         * not the same as 'is being flinged', which can be checked by
         * mScroller.isFinished() (flinging begins when the user lifts his finger).
         */
        private boolean mIsBeingDragged = false;
        /**
         * Position of the last motion event.
         */
        private int mLastMotionX;
        private int mLastMotionY;
        private float mSpeed;
        private int mHorizontalPadding;
        private int mVerticalPadding;
        private boolean mTouchEventEnabled;
        private boolean mUseContainer;
        private int mOffsetX;
        private int mOffsetY;
        private float mScale;
        private float mPivotX;
        private float mPivotY;
        private int mResizeMode;
        private OnFlyingEventListener mOnFlyingEventListener = new SimpleOnFlyingEventListener();
        private FrameLayout mView;

        public Helper(FrameLayout view, int frameLayoutHierarchy) {
            mView = view;
            mTouchSlop = ViewConfiguration.get(mView.getContext()).getScaledTouchSlop();
            mGestureDetector = new GestureDetector(mView.getContext(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    if (!insideOfContents(e)) {
                        mOnFlyingEventListener.onDoubleClickOutside(mView);
                        return true;
                    }
                    return false;
                }

                @Override
                public void onLongPress(MotionEvent e) {
                    if (!insideOfContents(e)) {
                        mOnFlyingEventListener.onLongPressOutside(mView);
                    }
                }

                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    if (!insideOfContents(e)) {
                        mOnFlyingEventListener.onClickOutside(mView);
                        return true;
                    }
                    return false;
                }
            });

            mView.setOnHierarchyChangeListener(new OnHierarchyChangeListener() {
                @Override
                public void onChildViewAdded(View parent, View child) {
                    // recalculate
                    mUpdatePivotOnTime = true;
                }

                @Override
                public void onChildViewRemoved(View parent, View child) {
                    // recalculate
                    mUpdatePivotOnTime = true;
                }
            });

            setSpeed(DEFAULT_SPEED);
            setHorizontalPadding(DEFAULT_HORIZONTAL_PADDING);
            setVerticalPadding(DEFAULT_VERTICAL_PADDING);
            setTouchEventEnabled(DEFAULT_TOUCH_EVENT_ENABLED);
            setUseContainer(DEFAULT_USE_CONTAINER);
            setOffset(0, 0);
            setScale(DEFAULT_SCALE);
            setPivot(DEFAULT_PIVOT_X, DEFAULT_PIVOT_Y);
            setResizeMode(DEFAULT_RESIZE_MODE);

            mGetPaddingLeftWithForeground = getHiddenMethod(view, frameLayoutHierarchy,
                    "getPaddingLeftWithForeground");
            mGetPaddingTopWithForeground = getHiddenMethod(view, frameLayoutHierarchy,
                    "getPaddingTopWithForeground");
            mGetPaddingRightWithForeground = getHiddenMethod(view, frameLayoutHierarchy,
                    "getPaddingRightWithForeground");
            mGetPaddingBottomWithForeground = getHiddenMethod(view, frameLayoutHierarchy,
                    "getPaddingBottomWithForeground");
            mMeasureChildWithMargins = getHiddenMethod(view, frameLayoutHierarchy + 1,
                    "measureChildWithMargins", View.class, int.class, int.class, int.class, int.class);
            mGetSuggestedMinimumHeight = getHiddenMethod(view, frameLayoutHierarchy + 2,
                    "getSuggestedMinimumHeight");
            mGetSuggestedMinimumWidth = getHiddenMethod(view, frameLayoutHierarchy + 2,
                    "getSuggestedMinimumWidth");
            mSetMeasuredDimension = getHiddenMethod(view, frameLayoutHierarchy + 2,
                    "setMeasuredDimension", int.class, int.class);
        }

        private Method getHiddenMethod(Object object, int hier, String methodName, Class<?>... parameterTypes) {
            Class<?> cls = object.getClass();
            for (int i = 0; i < hier; ++i) {
                cls = cls.getSuperclass();
            }
            try {
                final Method method = cls.getDeclaredMethod(methodName, parameterTypes);
                if (!method.isAccessible()) {
                    AccessController.doPrivileged(new PrivilegedAction<Object>() {
                        public Object run() {
                            method.setAccessible(true);
                            return null;
                        }
                    });
                }
                return method;
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
        }

        protected final int getPaddingLeftWithForeground() {
            try {
                return (Integer) mGetPaddingLeftWithForeground.invoke(mView);
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        protected final int getPaddingTopWithForeground() {
            try {
                return (Integer) mGetPaddingTopWithForeground.invoke(mView);
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        protected final int getPaddingRightWithForeground() {
            try {
                return (Integer) mGetPaddingRightWithForeground.invoke(mView);
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        protected final int getPaddingBottomWithForeground() {
            try {
                return (Integer) mGetPaddingBottomWithForeground.invoke(mView);
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        protected final void measureChildWithMargins(View child,
                                                     int parentWidthMeasureSpec, int widthUsed,
                                                     int parentHeightMeasureSpec, int heightUsed) {
            try {
                mMeasureChildWithMargins.invoke(mView,
                        child, parentWidthMeasureSpec, widthUsed, parentHeightMeasureSpec, heightUsed);
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        protected final int getSuggestedMinimumHeight() {
            try {
                return (Integer) mGetSuggestedMinimumHeight.invoke(mView);
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        protected final int getSuggestedMinimumWidth() {
            try {
                return (Integer) mGetSuggestedMinimumWidth.invoke(mView);
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        protected final void setMeasuredDimension(int measuredWidth, int measuredHeight) {
            try {
                mSetMeasuredDimension.invoke(mView, measuredWidth, measuredHeight);
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        public ViewGroup getAttachedView() {
            return mView;
        }

        public float getSpeed() {
            return mSpeed;
        }

        public void setSpeed(float speed) {
            mSpeed = speed;
        }

        public int getHorizontalPadding() {
            return mHorizontalPadding;
        }

        public void setHorizontalPadding(int padding) {
            mHorizontalPadding = padding;
        }

        public int getVerticalPadding() {
            return mVerticalPadding;
        }

        public void setVerticalPadding(int padding) {
            mVerticalPadding = padding;
        }

        public boolean getTouchEventEnabled() {
            return mTouchEventEnabled;
        }

        public void setTouchEventEnabled(boolean enable) {
            mTouchEventEnabled = enable;
        }

        public boolean getUseContainer() {
            return mUseContainer;
        }

        public void setUseContainer(boolean use) {
            mUseContainer = use;
        }

        public int getOffsetX() {
            return mOffsetX;
        }

        public void setOffsetX(int offset) {
            mOffsetX = offset;
        }

        public int getOffsetY() {
            return mOffsetY;
        }

        public void setOffsetY(int offset) {
            mOffsetY = offset;
        }

        public void setOffset(int x, int y) {
            mOffsetX = x;
            mOffsetY = y;
        }

        public float getScale() {
            return mScale;
        }

        public void setScale(float scale) {
            // Also called from resize
            if (mUpdatePivotOnTime) {
                performLayoutAdjustment();
                mUpdatePivotOnTime = false;
            }
            switch (mResizeMode) {
                case RESIZE_MODE_SCALE: {
                    mScale = scale;
                    final int count = getUseContainer() ? 1 : mView.getChildCount();
                    for (int i = 0; i < count; ++i) {
                        final View child = mView.getChildAt(i);
                        child.setScaleX(scale);
                        child.setScaleY(scale);
                    }
                    break;
                }
                case RESIZE_MODE_PADDING:
                    resize(scale, false);
                    break;
            }
        }

        public float getPivotX() {
            return mPivotX;
        }

        public float getPivotY() {
            return mPivotY;
        }

        public void setPivot(float x, float y) {
            mPivotX = x;
            mPivotY = y;
            mUpdatePivotOnTime = true;
        }

        public void setPivotX(float x) {
            setPivot(x, mPivotY);
        }

        public void setPivotY(float y) {
            setPivot(mPivotX, y);
        }

        private void setChildrenPivot(float x, float y) {
            final int count = getUseContainer() ? 1 : mView.getChildCount();
            for (int i = 0; i < count; ++i) {
                final View child = mView.getChildAt(i);
                child.setPivotX(x);
                child.setPivotY(y);
            }
        }

        public void setResizeMode(int mode) {
            mResizeMode = mode;
        }

        public int getResizeMode() {
            return mResizeMode;
        }

        public void performLayoutAdjustment() {
            performLayoutAdjustment(false);
        }

        public void performLayoutAdjustment(boolean animation) {
            if (mView.getChildCount() == 0) {
                return;
            }
            final View view = getUseContainer() ? mView.getChildAt(0) : mView;
            final float pivotX = view.getWidth() * mPivotX;
            final float pivotY = view.getHeight() * mPivotY;
            if (animation) {
                final View child = mView.getChildAt(0);
                final PointF start = new PointF(child.getPivotX(), child.getPivotY());
                final PointF end = new PointF(pivotX, pivotY);
                final ValueAnimator animator = ValueAnimator.ofObject(mPointFEvaluator, start, end);
                animator.addUpdateListener(mChangePivotAnimatorUpdateListener);
                animator.start();
            } else {
                setChildrenPivot(pivotX, pivotY);
            }
        }

        public boolean onInterceptTouchEvent(MotionEvent ev) {
            /*
             * This method JUST determines whether we want to intercept the motion.
             * If we return true, onMotionEvent will be called and we do the actual
             * scrolling there.
             */

            /*
             * Shortcut the most recurring case: the user is in the dragging state
             * and he is moving his finger. We want to intercept this motion.
             */
            final int action = ev.getAction();
            if ((action == MotionEvent.ACTION_MOVE) && mIsBeingDragged) {
                return true;
            }

            boolean dragLocally = false;

            switch (action & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_MOVE: {
                    /*
                     * mIsBeingDragged == false, otherwise the shortcut would have
                     * caught it. Check whether the user has moved far enough from his
                     * original down touch.
                     */

                    /*
                     * Locally do absolute value. mLastMotionY is set to the y value of
                     * the down event.
                     */
                    final int activePointerId = mActivePointerId;
                    if (activePointerId == INVALID_POINTER) {
                        // If we don't have a valid id, the touch down wasn't on
                        // content.
                        break;
                    }

                    final int pointerIndex = ev.findPointerIndex(activePointerId);
                    if (pointerIndex == -1) {
                        Log.e(TAG, "Invalid pointerId=" + activePointerId
                                + " in onInterceptTouchEvent");
                        break;
                    }

                    final int x = (int) ev.getX(pointerIndex);
                    final int deltaX = x - mLastMotionX;
                    final int y = (int) ev.getY(pointerIndex);
                    final int deltaY = y - mLastMotionY;
                    if (mTouchEventEnabled) {
                        final double delta = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                        if (delta > mTouchSlop) {
                            final ViewParent parent = mView.getParent();
                            if (parent != null) {
                                parent.requestDisallowInterceptTouchEvent(true);
                            }
                            dragLocally = true;
                        }
                    }
                    break;
                }

                case MotionEvent.ACTION_DOWN: {
                    final int x = (int) ev.getX();
                    final int y = (int) ev.getY();
                    /*
                     * Remember location of down touch. ACTION_DOWN always refers to
                     * pointer index 0.
                     */
                    mLastMotionX = x;
                    mLastMotionY = y;
                    mActivePointerId = ev.getPointerId(0);
                    break;
                }

                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    // always does not intercept

                    /* Release the drag */
                    mIsBeingDragged = false;
                    mActivePointerId = INVALID_POINTER;
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    onSecondaryPointerUp(ev);
                    break;
            }
            /*
             * The only time we want to intercept motion events is if we are in the
             * drag mode.
             */
            return mIsBeingDragged || dragLocally;
        }

        public boolean onTouchEvent(@NonNull MotionEvent ev) {
            final int action = ev.getAction();
            switch (action & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN: {
                    if (mView.getChildCount() == 0) {
                        return false;
                    }

                    // Remember where the motion event started
                    mLastMotionX = (int) ev.getX();
                    mLastMotionY = (int) ev.getY();
                    mActivePointerId = ev.getPointerId(0);
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    final int activePointerIndex = ev
                            .findPointerIndex(mActivePointerId);
                    if (activePointerIndex == -1) {
                        Log.e(TAG, "Invalid pointerId=" + mActivePointerId + " in onTouchEvent");
                        break;
                    }

                    final int x = (int) ev.getX(activePointerIndex);
                    final int deltaX = x - mLastMotionX;
                    final int y = (int) ev.getY(activePointerIndex);
                    final int deltaY = y - mLastMotionY;
                    if (!mIsBeingDragged) {
                        if (mTouchEventEnabled) {
                            final double delta = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                            if (delta > mTouchSlop) {
                                final ViewParent parent = mView.getParent();
                                if (parent != null) {
                                    parent.requestDisallowInterceptTouchEvent(true);
                                }
                                mIsBeingDragged = true;
                                mOnFlyingEventListener.onDragStarted(mView);
                            }
                        }
                    }
                    if (mIsBeingDragged) {
                        // Scroll to follow the motion event

                        move(deltaX, deltaY, false);
                        mLastMotionX = x;
                        mLastMotionY = y;
                    }
                    break;
                }
                case MotionEvent.ACTION_UP: {
                    if (mIsBeingDragged) {
                        mOnFlyingEventListener.onDragFinished(mView);
                        mActivePointerId = INVALID_POINTER;
                        mIsBeingDragged = false;
                    }
                    break;
                }
                case MotionEvent.ACTION_CANCEL:
                    if (mIsBeingDragged && mView.getChildCount() > 0) {
                        mOnFlyingEventListener.onDragFinished(mView);
                        mActivePointerId = INVALID_POINTER;
                        mIsBeingDragged = false;
                    }
                    break;
                case MotionEvent.ACTION_POINTER_DOWN: {
                    final int index = ev.getActionIndex();
                    mLastMotionX = (int) ev.getX(index);
                    mLastMotionY = (int) ev.getY(index);
                    mActivePointerId = ev.getPointerId(index);
                    break;
                }
                case MotionEvent.ACTION_POINTER_UP:
                    onSecondaryPointerUp(ev);
                    mLastMotionX = (int) ev.getX(ev.findPointerIndex(mActivePointerId));
                    mLastMotionY = (int) ev.getY(ev.findPointerIndex(mActivePointerId));
                    break;
            }

            final boolean consume = consumeTouchEvent();
            if (consume) {
                mGestureDetector.onTouchEvent(ev);
                return true;
            }
            return false;
        }

        private boolean consumeTouchEvent() {
            return mIsBeingDragged || mTouchEventEnabled || isResized() || !staysHome();
        }

        private void onSecondaryPointerUp(MotionEvent ev) {
            final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
            final int pointerId = ev.getPointerId(pointerIndex);
            if (pointerId == mActivePointerId) {
                // This was our active pointer going up. Choose a new
                // active pointer and adjust accordingly.
                // TODO: Make this decision more intelligent.
                final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                mLastMotionX = (int) ev.getX(newPointerIndex);
                mLastMotionY = (int) ev.getY(newPointerIndex);
                mActivePointerId = ev.getPointerId(newPointerIndex);
            }
        }

        @SuppressLint("NewApi")
        public void onLayout(boolean changed, int left, int top, int right, int bottom) {
            final boolean forceLeftGravity = false;
            final int count = mView.getChildCount();

            final int parentLeft = getPaddingLeftWithForeground();
            final int parentRight = right - left - getPaddingRightWithForeground();

            final int parentTop = getPaddingTopWithForeground();
            final int parentBottom = bottom - top - getPaddingBottomWithForeground();

            final int oldHeight = mBoundaryRect.height();
            mBoundaryRect.setEmpty();
            for (int i = 0; i < count; i++) {
                final View child = mView.getChildAt(i);
                if (child.getVisibility() != GONE) {
                    final LayoutParams lp = (LayoutParams) child.getLayoutParams();

                    final int width = child.getMeasuredWidth();
                    final int height = child.getMeasuredHeight();

                    int childLeft;
                    int childTop;

                    int gravity = lp.gravity;
                    if (gravity == -1) {
                        gravity = DEFAULT_CHILD_GRAVITY;
                    }

                    final int layoutDirection =
                            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) ?
                                    mView.getLayoutDirection() : 0;
                    final int absoluteGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection);
                    final int verticalGravity = gravity & Gravity.VERTICAL_GRAVITY_MASK;

                    switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
                        case Gravity.CENTER_HORIZONTAL:
                            childLeft = parentLeft + (parentRight - parentLeft - width) / 2
                                    + lp.leftMargin - lp.rightMargin;
                            break;
                        case Gravity.RIGHT:
                            if (!forceLeftGravity) {
                                childLeft = parentRight - width - lp.rightMargin;
                                break;
                            }
                        case Gravity.LEFT:
                        default:
                            childLeft = parentLeft + lp.leftMargin;
                    }

                    switch (verticalGravity) {
                        case Gravity.TOP:
                            childTop = parentTop + lp.topMargin;
                            break;
                        case Gravity.CENTER_VERTICAL:
                            childTop = parentTop + (parentBottom - parentTop - height) / 2
                                    + lp.topMargin - lp.bottomMargin;
                            break;
                        case Gravity.BOTTOM:
                            childTop = parentBottom - height - lp.bottomMargin;
                            break;
                        default:
                            childTop = parentTop + lp.topMargin;
                    }

                    mChildRect.set(childLeft, childTop, childLeft + width, childTop + height);
                    if (!getUseContainer() || i == 0) {
                        mBoundaryRect.union(mChildRect);
                        // after union
                        mChildRect.offset(mOffsetX, mOffsetY);
                    }
                    child.layout(mChildRect.left, mChildRect.top, mChildRect.right, mChildRect.bottom);
                }
            }
            if (!isResized()) {
                mBoundaryRect.offset(mOffsetX, mOffsetY);
            } else {
                final float dx = mBoundaryRect.width() * mPivotX * (1 - mScale) + mOffsetX;
                final float dy = mBoundaryRect.height() * mPivotY * (1 - mScale) + mOffsetY;
                mBoundaryRect.set(
                        Math.round(mBoundaryRect.left * mScale + dx),
                        Math.round(mBoundaryRect.top * mScale + dy),
                        Math.round(mBoundaryRect.right * mScale + dx),
                        Math.round(mBoundaryRect.bottom * mScale + dy));
            }
            if (oldHeight != mBoundaryRect.height()) {
                performLayoutAdjustment();
            }
        }

        protected int clamp(int src, int limit) {
            if (src > limit) {
                return limit;
            } else if (src < -limit) {
                return -limit;
            }
            return src;
        }

        public void move(int deltaX, int deltaY, boolean animation) {
            deltaX = Math.round(deltaX * mSpeed);
            deltaY = Math.round(deltaY * mSpeed);
            moveWithoutSpeed(deltaX, deltaY, animation);
        }

        public void moveWithoutSpeed(int deltaX, int deltaY, boolean animation) {
            final int hLimit = mView.getWidth() - getHorizontalPadding();
            final int vLimit = mView.getHeight() - getVerticalPadding();
            final int newX = clamp(mOffsetX + deltaX, hLimit);
            final int newY = clamp(mOffsetY + deltaY, vLimit);
            if (!animation) {
                setOffset(newX, newY);
                mView.requestLayout();
            } else {
                final Point start = new Point(mOffsetX, mOffsetY);
                final Point end = new Point(newX, newY);
                final ValueAnimator move = ValueAnimator.ofObject(mPointEvaluator, start, end);
                move.addUpdateListener(mMoveAnimatorUpdateListener);
                move.start();
            }
        }

        public void goHome(boolean animation) {
            moveWithoutSpeed(-mOffsetX, -mOffsetY, animation);
        }

        public boolean staysHome() {
            return mOffsetX == 0 && mOffsetY == 0;
        }

        public void rotate() {
            mOffsetX = Math.round(mOffsetX * 1f / mView.getWidth() * mView.getHeight());
            mOffsetY = Math.round(mOffsetY * 1f / mView.getHeight() * mView.getWidth());
        }

        private boolean insideOfContents(MotionEvent ev) {
            final int x = (int) ev.getX();
            final int y = (int) ev.getY();

            return mBoundaryRect.contains(x, y);
        }

        private void backupChildrenPadding() {
            final int count = mView.getChildCount();
            mChildrenPaddingBackup = Lists.newArrayListWithCapacity(count);
            for (int i = 0; i < count; ++i) {
                final View child = mView.getChildAt(i);
                final Rect paddings = new Rect();
                paddings.set(child.getPaddingLeft(), child.getPaddingTop(),
                        child.getPaddingRight(), child.getPaddingBottom());
                mChildrenPaddingBackup.add(paddings);
            }
        }

        public void resize(float scale, boolean animation) {
//            Log.d(TAG, "padding: " + mView.getPaddingLeft() + ", " + mView.getPaddingTop()
//                    + ", " + mView.getPaddingRight() + ", " + mView.getPaddingBottom());
            switch (mResizeMode) {
                case RESIZE_MODE_SCALE: {
                    if (animation) {
                        final ValueAnimator scaleDown = ValueAnimator.ofFloat(mScale, scale);
                        scaleDown.addUpdateListener(mScaleAnimatorUpdateListener);
                        scaleDown.start();
                    } else {
                        setScale(scale);
                        mView.requestLayout();
                    }
                    break;
                }
                case RESIZE_MODE_PADDING: {
                    if (mChildrenPaddingBackup == null ||
                            mChildrenPaddingBackup.size() != mView.getChildCount()){
                        backupChildrenPadding();
                    }
                    final int count = getUseContainer() ? 1 : mView.getChildCount();
                    mScale = scale;
                    if (scale == DEFAULT_SCALE) {
                        // reset
                        for (int i = 0; i < count; ++i) {
                            final View child = mView.getChildAt(i);
                            final Rect paddings = mChildrenPaddingBackup.get(i);
                            child.setPadding(paddings.left, paddings.top,
                                    paddings.right, paddings.bottom);
                        }
                    } else {
                        for (int i = 0; i < count; ++i) {
                            final View child = mView.getChildAt(i);
                            float dw = child.getWidth() * (1 - scale);
                            float dh = child.getHeight() * (1 - scale);
                            int paddingLeft = Math.round(dw * mPivotX);
                            int paddingRight = Math.round(dw * (1 - mPivotX));
                            int paddingTop = Math.round(dh * mPivotY);
                            int paddingBottom = Math.round(dh * (1 - mPivotY));
                            child.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
                        }
                    }
                    break;
                }
            }
        }

        public boolean isResized() {
            switch (mResizeMode) {
                case RESIZE_MODE_PADDING: {
                    View child = mView.getChildAt(0);
                    if (child == null) {
                        return false;
                    }
                    if (mChildrenPaddingBackup == null ||
                            mChildrenPaddingBackup.size() != mView.getChildCount()){
                        backupChildrenPadding();
                    }
                    Rect paddings = mChildrenPaddingBackup.get(0);
                    return child.getPaddingLeft() != paddings.left ||
                            child.getPaddingTop() != paddings.top ||
                            child.getPaddingRight() != paddings.right ||
                            child.getPaddingBottom() != paddings.bottom;
                }
                case RESIZE_MODE_SCALE:
                default:
                    return mScale != DEFAULT_SCALE;
            }
        }

        protected Rect getBoundaryRect() {
            return mBoundaryRect;
        }

        public OnFlyingEventListener getOnFlyingEventListener() {
            return mOnFlyingEventListener;
        }

        public void setOnFlyingEventListener(OnFlyingEventListener listener) {
            mOnFlyingEventListener = listener;
        }

        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int count = mView.getChildCount();

            final boolean measureMatchParentChildren =
                    MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY ||
                            MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY;
            mMatchParentChildren.clear();

            widthMeasureSpec = MeasureSpec.makeMeasureSpec(
                    Math.round(MeasureSpec.getSize(widthMeasureSpec) * 0.7f),
                    MeasureSpec.getMode(widthMeasureSpec));
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(
                    Math.round(MeasureSpec.getSize(heightMeasureSpec) * 0.7f),
                    MeasureSpec.getMode(heightMeasureSpec));

            int maxHeight = 0;
            int maxWidth = 0;
            int childState = 0;

            for (int i = 0; i < count; i++) {
                final View child = mView.getChildAt(i);
                if (mView.getMeasureAllChildren() || child.getVisibility() != GONE) {
                    measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
                    final FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) child.getLayoutParams();
                    maxWidth = Math.max(maxWidth,
                            child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin);
                    maxHeight = Math.max(maxHeight,
                            child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin);
                    childState = combineMeasuredStates(childState, child.getMeasuredState());
                    if (measureMatchParentChildren) {
                        if (lp.width == LayoutParams.MATCH_PARENT ||
                                lp.height == LayoutParams.MATCH_PARENT) {
                            mMatchParentChildren.add(child);
                        }
                    }
                }
            }

            // Account for padding too
            maxWidth += getPaddingLeftWithForeground() + getPaddingRightWithForeground();
            maxHeight += getPaddingTopWithForeground() + getPaddingBottomWithForeground();

            // Check against our minimum height and width
            maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
            maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());

            // Check against our foreground's minimum height and width
            final Drawable drawable = mView.getForeground();
            if (drawable != null) {
                maxHeight = Math.max(maxHeight, drawable.getMinimumHeight());
                maxWidth = Math.max(maxWidth, drawable.getMinimumWidth());
            }

            setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
                    resolveSizeAndState(maxHeight, heightMeasureSpec,
                            childState << MEASURED_HEIGHT_STATE_SHIFT));

            count = mMatchParentChildren.size();
            if (count > 1) {
                for (int i = 0; i < count; i++) {
                    final View child = mMatchParentChildren.get(i);

                    final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
                    int childWidthMeasureSpec;
                    int childHeightMeasureSpec;

                    //
                    // RESIZE
                    //
                    float scale = 1f;
                    if (lp.width == LayoutParams.MATCH_PARENT
                            && lp.height == LayoutParams.MATCH_PARENT) {
                        scale = 0.7f;
                    }
                    Log.d(TAG, "scale=" + scale);

                    if (lp.width == LayoutParams.MATCH_PARENT) {
                        childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(Math.round(
                                        (mView.getMeasuredWidth() -
                                                getPaddingLeftWithForeground() - getPaddingRightWithForeground() -
                                                lp.leftMargin - lp.rightMargin) * scale),
                                MeasureSpec.EXACTLY);
                    } else {
                        childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec,
                                getPaddingLeftWithForeground() + getPaddingRightWithForeground() +
                                        lp.leftMargin + lp.rightMargin,
                                lp.width);
                    }

                    if (lp.height == LayoutParams.MATCH_PARENT) {
                        childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(Math.round(
                                        (mView.getMeasuredHeight() -
                                                getPaddingTopWithForeground() - getPaddingBottomWithForeground() -
                                                lp.topMargin - lp.bottomMargin) * scale),
                                MeasureSpec.EXACTLY);
                    } else {
                        childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec,
                                getPaddingTopWithForeground() + getPaddingBottomWithForeground() +
                                        lp.topMargin + lp.bottomMargin,
                                lp.height);
                    }

                    child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
                }
            }
        }

    }

    public static class SimpleOnFlyingEventListener implements OnFlyingEventListener {
        @Override
        public void onDragStarted(ViewGroup v) {
            // nothing to do
        }

        @Override
        public void onDragFinished(ViewGroup v) {
            // nothing to do
        }

        @Override
        public void onClickOutside(ViewGroup v) {
            // nothing to do
        }

        @Override
        public void onDoubleClickOutside(ViewGroup v) {
            // nothing to do
        }

        @Override
        public void onLongPressOutside(ViewGroup v) {
            // nothing to do
        }
    }
}