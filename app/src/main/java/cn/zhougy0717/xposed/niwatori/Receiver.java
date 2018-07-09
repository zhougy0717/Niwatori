package cn.zhougy0717.xposed.niwatori;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.FrameLayout;

import java.util.List;

import de.robv.android.xposed.XposedHelpers;
import cn.zhougy0717.xposed.niwatori.FlyingHelper;
import cn.zhougy0717.xposed.niwatori.NFW;
import cn.zhougy0717.xposed.niwatori.XposedModule;

/**
 * Created by zhougua on 1/12/2018.
 */

public class Receiver extends XposedModule implements IReceiver {
    private static final String FIELD_FLYING_HELPER = NFW.NAME + "_flyingHelper";

    private boolean mRegistered = false;
    protected BroadcastReceiver mReceiver;

    protected static FlyingHelper getHelper(@NonNull FrameLayout decorView) {
        return (FlyingHelper) XposedHelpers.getAdditionalInstanceField(
                decorView, FIELD_FLYING_HELPER);
    }

    protected final FrameLayout mDecorView;
    protected IntentFilter mFilter;
    public Receiver (FrameLayout decorView) {
        this(decorView, null);
    }
    public Receiver (FrameLayout decorView, IntentFilter filter) {
        mDecorView = decorView;
        mFilter = filter;
    }

    @Override
    public BroadcastReceiver create() {
        return null;
    }

    @Override
    public void register(){
        if (!mRegistered) {
            mDecorView.getContext().registerReceiver(mReceiver, mFilter);
            mRegistered = true;
        }
    }

    @Override
    public void unregister(){
        if (mRegistered) {
            mDecorView.getContext().unregisterReceiver(mReceiver);
//            getHelper(mDecorView).resetState(true);
//            ModActivity.getHelper(mDecorView).resetState(true);
            mRegistered = false;
        }

    }


    @Override
    public void setFilter(IntentFilter filter) {
        mFilter = filter;
        Log.e("Ben", "filter set to: " + filter);
    }
}
