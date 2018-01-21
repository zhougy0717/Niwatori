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
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import jp.tkgktyk.flyinglayout.FlyingLayout;
import jp.tkgktyk.xposed.niwatori.NFW;
import jp.tkgktyk.xposed.niwatori.R;

/**
 * Created by tkgktyk on 2015/03/27.
 */
public class SmallScreenActivity extends Activity {
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
        private static final int MAX_PROGRESS = 100;

        private FlyingLayout mSmallScreenView;
        private ImageView mBoundaryView;
        private TextView mPivotXTextView;
        private TextView mPivotYTextView;
        private TextView mSizeTextView;
        private SeekBar mPivotXSeekBar;
        private SeekBar mPivotYSeekBar;
        private SeekBar mSizeSeekBar;

        public GraphicalSettingsFragment() {
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_small_screen, container, false);

            mSmallScreenView = (FlyingLayout) rootView.findViewById(R.id.smallScreen);
            mBoundaryView = (ImageView) rootView.findViewById(R.id.boundary);
            mPivotXTextView = (TextView) rootView.findViewById(R.id.pivotXText);
            mPivotYTextView = (TextView) rootView.findViewById(R.id.pivotYText);
            mSizeTextView = (TextView) rootView.findViewById(R.id.sizeText);
            mPivotXSeekBar = (SeekBar) rootView.findViewById(R.id.pivotXSeekBar);
            mPivotYSeekBar = (SeekBar) rootView.findViewById(R.id.pivotYSeekBar);
            mSizeSeekBar = (SeekBar) rootView.findViewById(R.id.sizeSeekBar);

            SharedPreferences prefs = NFW.getSharedPreferences(getActivity());

            int pivotX = prefs.getInt(getString(R.string.key_small_screen_pivot_x),
                    getResources().getInteger(R.integer.default_small_screen_pivot_x));
            mPivotXSeekBar.setMax(MAX_PROGRESS);
            mPivotXSeekBar.setProgress(pivotX);
            int pivotY = prefs.getInt(getString(R.string.key_small_screen_pivot_y),
                    getResources().getInteger(R.integer.default_small_screen_pivot_y));
            mPivotYSeekBar.setMax(MAX_PROGRESS);
            mPivotYSeekBar.setProgress(pivotY);
            int size = prefs.getInt(getString(R.string.key_small_screen_size),
                    getResources().getInteger(R.integer.default_small_screen_size));
            mSizeSeekBar.setMax(MAX_PROGRESS - 1);
            mSizeSeekBar.setProgress(size);

            SeekBar.OnSeekBarChangeListener changeListener = new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        updateSmallScreen();
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    save();
                }
            };
            mPivotXSeekBar.setOnSeekBarChangeListener(changeListener);
            mPivotYSeekBar.setOnSeekBarChangeListener(changeListener);
            mSizeSeekBar.setOnSeekBarChangeListener(changeListener);

            final int width = Math.round(getResources().getDimension(R.dimen.boundary_width));
            int color = Color.parseColor(prefs.getString(
                    getString(R.string.key_boundary_color_ss),
                    getString(R.string.default_boundary_color_ss)));
            if (color == Color.TRANSPARENT) {
                color = Color.RED;
            }
            final GradientDrawable boundary = NFW.makeBoundaryDrawable(width, color);
            mBoundaryView.setImageDrawable(boundary);

            mSmallScreenView.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @SuppressWarnings("deprecation")
                        @SuppressLint("NewApi")
                        @Override
                        public void onGlobalLayout() {
                            updateSmallScreen();
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                mSmallScreenView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            } else {
                                mSmallScreenView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                            }
                        }
                    });

            return rootView;
        }

        private void updateSmallScreen() {
            FlyingLayout.Helper helper = mSmallScreenView.getHelper();
            int pivotX = mPivotXSeekBar.getProgress();
            mPivotXTextView.setText(getString(R.string.pivot_x_d1, pivotX));
            int pivotY = mPivotYSeekBar.getProgress();
            mPivotYTextView.setText(getString(R.string.pivot_y_d1, pivotY));
            helper.setPivot(pivotX / 100f, pivotY / 100f);
            int size = mSizeSeekBar.getProgress();
            mSizeTextView.setText(getString(R.string.size_d1, size));
            helper.setScale(size / 100f);

            mSmallScreenView.requestLayout();
        }

        private void save() {
            int pivotX = mPivotXSeekBar.getProgress();
            int pivotY = mPivotYSeekBar.getProgress();
            int size = mSizeSeekBar.getProgress();
            NFW.getSharedPreferences(getActivity()).edit()
                    .putInt(getString(R.string.key_small_screen_pivot_x), pivotX)
                    .putInt(getString(R.string.key_small_screen_pivot_y), pivotY)
                    .putInt(getString(R.string.key_small_screen_size), size)
                    .apply();
        }
    }
}
