package jp.tkgktyk.xposed.niwatori.app;

import android.app.Activity;
import android.os.Bundle;

import jp.tkgktyk.xposed.niwatori.NFW;

/**
 * Created by tkgktyk on 2015/02/13.
 */
public class ActionActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ignore exit animation
        overridePendingTransition(0, 0);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (getIntent() != null) {
            NFW.performAction(this, getIntent().getAction());
        }
    }
}
