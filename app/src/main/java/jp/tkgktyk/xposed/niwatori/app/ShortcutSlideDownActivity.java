package jp.tkgktyk.xposed.niwatori.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import jp.tkgktyk.xposed.niwatori.NFW;
import jp.tkgktyk.xposed.niwatori.R;

/**
 * Created by tkgktyk on 2015/02/13.
 */
public class ShortcutSlideDownActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent().getAction()
                .equals(Intent.ACTION_CREATE_SHORTCUT)) {

            Intent shortcut = new Intent(this, ActionActivity.class);
            shortcut.setAction(NFW.ACTION_PIN_OR_RESET);

            Intent intent = new Intent();
            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcut);
            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.action_pin_or_reset));
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource.fromContext(this, R.drawable.ic_action_slide_down));

            setResult(RESULT_OK, intent);
        }
        else {
            setResult(RESULT_CANCELED);
        }

        finish();
    }


}
