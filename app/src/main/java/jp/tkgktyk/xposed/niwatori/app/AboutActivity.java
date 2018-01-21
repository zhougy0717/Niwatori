package jp.tkgktyk.xposed.niwatori.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import jp.tkgktyk.xposed.niwatori.NFW;
import jp.tkgktyk.xposed.niwatori.R;

/**
 * Created by tkgktyk on 2015/02/25.
 */
public class AboutActivity extends Activity {
    public static final String EXTRA_DONATED = NFW.PREFIX_EXTRA + "DONATED";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        boolean donated = false;
        if (getIntent() != null) {
            donated = getIntent().getBooleanExtra(EXTRA_DONATED, false);
        }
        if (donated) {
            findViewById(R.id.thank_you_for_donation).setVisibility(View.VISIBLE);
        }
    }
}
