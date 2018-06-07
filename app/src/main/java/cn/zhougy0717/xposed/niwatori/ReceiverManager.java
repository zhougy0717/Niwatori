package cn.zhougy0717.xposed.niwatori;

import android.content.SharedPreferences;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ShareActionProvider;

public class ReceiverManager {
    private FrameLayout mDecorView;
    private IReceiver mActionReceiver;
    private IReceiver mSettingsLoadedReceiver;
//    private FlyingHelper mHelper;
    public ReceiverManager(FrameLayout decorView) {
        mDecorView = decorView;
        mActionReceiver = ActionReceiver.getInstance(mDecorView, NFW.FOCUSED_ACTIVITY_FILTER);
        mSettingsLoadedReceiver = SettingsLoadReceiver.getInstance(mDecorView, NFW.SETTINGS_CHANGED_FILTER);
    }

//        private static Handler getInstance(FrameLayout decorView){
//            Handler handler = (Handler) XposedHelpers.getAdditionalInstanceField(decorView, "HANDLER");
//            if (handler == null) {
//                handler = new Handler(decorView);
//                XposedHelpers.setAdditionalInstanceField(decorView, "HANDLER", handler);
//            }
//            return handler;
//        }

    public void registerReceiver(){
        mActionReceiver.register();
        mSettingsLoadedReceiver.register();
    }

    public void unregisterReceiver(){
        mActionReceiver.unregister();
        mSettingsLoadedReceiver.unregister();
    }
}
