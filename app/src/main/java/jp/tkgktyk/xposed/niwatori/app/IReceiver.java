package jp.tkgktyk.xposed.niwatori.app;

import android.content.BroadcastReceiver;
import android.widget.FrameLayout;

/**
 * Created by zhougua on 1/12/2018.
 */

public interface IReceiver {
    public BroadcastReceiver create ();
    public void register();
    public void unregister();
}
