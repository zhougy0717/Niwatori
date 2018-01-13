package jp.tkgktyk.xposed.niwatori.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.FrameLayout;

import java.util.List;

import de.robv.android.xposed.XposedHelpers;
import jp.tkgktyk.xposed.niwatori.FlyingHelper;
import jp.tkgktyk.xposed.niwatori.NFW;
import jp.tkgktyk.xposed.niwatori.XposedModule;

/**
 * Created by zhougua on 1/12/2018.
 */

public class Receiver extends XposedModule{
    public final static String ACTION_RECEIVER = "ACTION_RECEIVER";
    public final static String SETTINGS_LOAD_RECEIVER = "SETTINGS_LOAD_RECEIVER";

    private static final String FIELD_FLYING_HELPER = NFW.NAME + "_flyingHelper";

    protected static FlyingHelper getHelper(@NonNull FrameLayout decorView) {
        return (FlyingHelper) XposedHelpers.getAdditionalInstanceField(
                decorView, FIELD_FLYING_HELPER);
    }

    protected final FrameLayout mDecorView;
    public Receiver (FrameLayout decorView) {
        mDecorView = decorView;
    }
    protected void register() {
        List<IReceiver> activeReceivers = (List<IReceiver>)XposedHelpers.getAdditionalInstanceField("ACTIVE RECEIVERS", "ACTIVE RECEIVERS");
        activeReceivers.add((IReceiver)this);
        XposedHelpers.setAdditionalInstanceField("ACTIVE RECEIVERS", "ACTIVE REGISTERS", activeReceivers);
    }

//    public static void unregisterAll(){
//        List<IReceiver> activeReceivers = (List<IReceiver>)XposedHelpers.getAdditionalInstanceField("ACTIVE RECEIVERS", "ACTIVE RECEIVERS");
//        if(activeReceivers == null) {
//            return;
//        }
//        Log.e("Ben", "There are " + Integer.toString(activeReceivers.size()) + " receivers");
//        for(IReceiver recv : activeReceivers) {
//            recv.unregister();
//        }
//        activeReceivers.clear();
//    }
}
