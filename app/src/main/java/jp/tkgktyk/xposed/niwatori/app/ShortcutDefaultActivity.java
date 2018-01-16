package jp.tkgktyk.xposed.niwatori.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import jp.tkgktyk.xposed.niwatori.NFW;
import jp.tkgktyk.xposed.niwatori.R;

/**
 * Created by tkgktyk on 2015/02/13.
 */
public class ShortcutDefaultActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent().getAction()
                .equals(Intent.ACTION_CREATE_SHORTCUT)) {
//            NFW.Settings settings = XposedModule.getSettings();
//            String action = settings.extraAction;

            Intent shortcut = new Intent(this, ActionActivity.class);
            shortcut.setAction(NFW.ACTION_SMALL_SCREEN);

            Intent intent = new Intent();
            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcut);
            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.action_small_screen));
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource.fromContext(this, R.drawable.ic_action_small_screen));

            setResult(RESULT_OK, intent);
        }
        else {
            setResult(RESULT_CANCELED);
        }
        finish();

//        mShortcutNameList = new ArrayList<>(mShortcutNameIdList.length);
//        for (int id : mShortcutNameIdList) {
//            mShortcutNameList.add(getString(id));
//        }
//
//        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
//                android.R.layout.simple_list_item_1, mShortcutNameList);
//        setListAdapter(adapter);
    }


}
