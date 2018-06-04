package cn.zhougy0717.xposed.niwatori;

import android.util.Log;
import android.widget.FrameLayout;

public class ReceiverManager {
    private FrameLayout mDecorView;
    private IReceiver mActionReceiver;
    private IReceiver mSettingsLoadedReceiver;
    private FlyingHelper mHelper;
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
        mHelper = ModActivity.getHelper(mDecorView);
        if (mHelper != null && mHelper.getSettings().smallScreenPersistent) {
            if (mHelper.getSettings().screenResized) {
                mHelper.performAction(NFW.ACTION_FORCE_SMALL_SCREEN);
            }
            else if (mHelper.isResized()){
                mHelper.performAction(NFW.ACTION_RESET);
            }
        }
    }

    public void unregisterReceiver(){
        mActionReceiver.unregister();
        mSettingsLoadedReceiver.unregister();
        mHelper = ModActivity.getHelper(mDecorView);
//        if (mHelper!=null && mHelper.getSettings().autoReset) {
        if (mHelper!=null && !mHelper.getSettings().smallScreenPersistent) {
            // NOTE: When fire actions from shortcut (ActionActivity), it causes onPause and onResume events
            // because through an Activity. So shouldn't reset automatically.
            mHelper.resetState(true);
        }
    }
}
