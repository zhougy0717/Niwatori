package jp.tkgktyk.xposed.niwatori.app;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import jp.tkgktyk.xposed.niwatori.NFW;
import jp.tkgktyk.xposed.niwatori.R;

/**
 * Created by tkgktyk on 2015/02/13.
 */
public class ShortcutActivity extends ListActivity {

    private static final String mActionList[] = {
            NFW.ACTION_MOVABLE_SCREEN,
            NFW.ACTION_PIN_OR_RESET,
            NFW.ACTION_SMALL_SCREEN,
            NFW.ACTION_EXTRA_ACTION,
            NFW.ACTION_SB_EXPAND_NOTIFICATIONS,
            NFW.ACTION_SB_EXPAND_QUICK_SETTINGS,
    };
    private static final int mShortcutNameIdList[] = {
            R.string.action_movable_screen,
            R.string.action_pin_or_reset,
            R.string.action_small_screen,
            R.string.action_extra_action,
            R.string.action_sb_expand_notifications,
            R.string.action_sb_expand_quick_settings,
    };
    private static final int mShortcutIconList[] = {
            R.drawable.ic_action_movable_screen,
            R.drawable.ic_action_slide_down,
            R.drawable.ic_action_small_screen,
            R.drawable.ic_action_extra_action,
            R.drawable.ic_action_expand_notifications,
            R.drawable.ic_action_expand_quick_settings,
    };
    private List<String> mShortcutNameList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mShortcutNameList = new ArrayList<>(mShortcutNameIdList.length);
        for (int id : mShortcutNameIdList) {
            mShortcutNameList.add(getString(id));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, mShortcutNameList);
        setListAdapter(adapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        // create shortcut
        Intent shortcut = new Intent(this, ActionActivity.class);
        shortcut.setAction(mActionList[position]);

        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcut);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, mShortcutNameList.get(position));
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(this, mShortcutIconList[position]));

        setResult(RESULT_OK, intent);
        finish();
    }
}
