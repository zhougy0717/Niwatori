package jp.tkgktyk.xposed.niwatori;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;

/**
 * Created by tkgktyk on 2015/02/12.
 */
public class InitialPosition {
    public static int DEFAULT_X_PERCENT = 0;
    public static int DEFAULT_Y_PERCENT = 0;

    private int mXp;
    private int mYp;

    public InitialPosition(Context context) {
        SharedPreferences pref = NFW.getSharedPreferences(context);
        mXp = pref.getInt(context.getString(R.string.key_initial_x_percent),
                DEFAULT_X_PERCENT);
        mYp = pref.getInt(context.getString(R.string.key_initial_y_percent),
                DEFAULT_Y_PERCENT);
    }

    public InitialPosition(int xp, int yp) {
        mXp = xp;
        mYp = yp;
    }

    public void save(Context context) {
        NFW.getSharedPreferences(context)
                .edit()
                .putInt(context.getString(R.string.key_initial_x_percent), mXp)
                .putInt(context.getString(R.string.key_initial_y_percent), mYp)
                .apply();
    }

    public int getX(View container) {
        return Math.round(container.getWidth() * mXp / 100f);
    }

    public int getY(View container) {
        return Math.round(container.getHeight() * mYp / 100f);
    }

    public int getXp() {
        return mXp;
    }

    public void setXp(int xp) {
        mXp = xp;
    }

    public void setXp(View container, float x) {
        mXp = Math.round(x / container.getWidth() * 10f) * 10;
    }

    public int getYp() {
        return mYp;
    }

    public void setYp(int yp) {
        mYp = yp;
    }

    public void setYp(View container, float y) {
        mYp = Math.round(y / container.getHeight() * 10f) * 10;
    }
}
