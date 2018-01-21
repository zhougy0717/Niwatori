package jp.tkgktyk.xposed.niwatori.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import jp.tkgktyk.xposed.niwatori.NFW;
import jp.tkgktyk.xposed.niwatori.R;

/**
 * Created by tkgktyk on 2015/02/13.
 */
public class AppSelectActivity extends Activity {

    public static String EXTRA_PREF_KEY_ID = NFW.PREFIX_EXTRA + "PREF_KEY_ID";
    public static String EXTRA_TITLE_ID = NFW.PREFIX_EXTRA + "TITLE_ID";

    private class ViewHolder {
        CheckBox onlySelected;
        AppSelectFragment selectableList;
    }

    private ViewHolder mViewHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_select);

        Intent intent = getIntent();
        final String prefKey = getString(intent.getIntExtra(EXTRA_PREF_KEY_ID, 0));
        final String title = getString(intent.getIntExtra(EXTRA_TITLE_ID, R.string.app_select_activity_name));
        setTitle(title);
        if (getActionBar() != null) {
            getActionBar().setTitle(title);
        }

        mViewHolder = new ViewHolder();
        mViewHolder.onlySelected = (CheckBox) findViewById(R.id.only_selected_check);
        mViewHolder.selectableList = (AppSelectFragment) getFragmentManager()
                .findFragmentById(R.id.selectable_list);

        mViewHolder.onlySelected
                .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView,
                                                 boolean isChecked) {
                        mViewHolder.selectableList
                                .setShowOnlySelected(isChecked);
                    }
                });

        mViewHolder.selectableList.setPrefKey(prefKey);
    }
}

