package jp.tkgktyk.xposed.niwatori.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.TextView;

import jp.tkgktyk.flyinglayout.FlyingLayout;
import jp.tkgktyk.xposed.niwatori.InitialPosition;
import jp.tkgktyk.xposed.niwatori.NFW;
import jp.tkgktyk.xposed.niwatori.R;

/**
 * Created by tkgktyk on 2015/02/13.
 */
public class InitialPositionActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new GraphicalSettingsFragment())
                    .commit();
        }
    }

    public static class GraphicalSettingsFragment extends Fragment {
        private FlyingLayout mFlyingLayout;
        private FrameLayout mContainer;
        private TextView mInitXText;
        private TextView mInitYText;

        private InitialPosition mInitialPosition;

        public GraphicalSettingsFragment() {
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_initial_position, container, false);

            mFlyingLayout = (FlyingLayout) rootView.findViewById(R.id.flying);
            mContainer = (FrameLayout) rootView.findViewById(R.id.container);
            mInitXText = (TextView) rootView.findViewById(R.id.initXText);
            mInitYText = (TextView) rootView.findViewById(R.id.initYText);

            mInitialPosition = new InitialPosition(getActivity());

            final SharedPreferences prefs = NFW.getSharedPreferences(getActivity());

            final int width = Math.round(getResources().getDimension(R.dimen.boundary_width));
            final int color = Color.parseColor(prefs.getString(
                    getString(R.string.key_boundary_color_ms),
                    getString(R.string.default_boundary_color_ms)));
            final GradientDrawable drawable = NFW.makeBoundaryDrawable(width, color);
            mContainer.setForeground(drawable);

            final FlyingLayout.Helper helper = mFlyingLayout.getHelper();
            helper.setSpeed(Float.parseFloat(prefs.getString(
                    getString(R.string.key_speed), Float.toString(FlyingLayout.DEFAULT_SPEED))));
            mFlyingLayout.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @SuppressWarnings("deprecation")
                        @SuppressLint("NewApi")
                        @Override
                        public void onGlobalLayout() {
                            updateLayout();
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                mFlyingLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            } else {
                                mFlyingLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                            }
                        }
                    });
            helper.setOnFlyingEventListener(new FlyingLayout.SimpleOnFlyingEventListener() {
                @Override
                public void onDragFinished(ViewGroup v) {
                    mInitialPosition.setXp(mFlyingLayout, helper.getOffsetX());
                    mInitialPosition.setYp(mFlyingLayout, helper.getOffsetY());
                    mInitialPosition.save(mFlyingLayout.getContext());
                    updateLayout();
                }
            });

            return rootView;
        }

        private void updateLayout() {
            mInitXText.setText(getString(R.string.init_x_d1, mInitialPosition.getXp()));
            mInitYText.setText(getString(R.string.init_y_d1, mInitialPosition.getYp()));

            mFlyingLayout.getHelper().setOffset(
                    mInitialPosition.getX(mFlyingLayout),
                    mInitialPosition.getY(mFlyingLayout));
            mFlyingLayout.requestLayout();
        }
    }
}
