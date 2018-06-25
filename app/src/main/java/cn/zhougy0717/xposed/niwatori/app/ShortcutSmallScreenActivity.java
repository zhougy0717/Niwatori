package cn.zhougy0717.xposed.niwatori.app;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.util.Arrays;
import java.util.Collections;

import cn.zhougy0717.xposed.niwatori.BuildConfig;
import cn.zhougy0717.xposed.niwatori.NFW;
import cn.zhougy0717.xposed.niwatori.R;

/**
 * Created by tkgktyk on 2015/02/13.
 */
public class ShortcutSmallScreenActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent().getAction()
                .equals(Intent.ACTION_CREATE_SHORTCUT)) {

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
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            Intent small_screen = new Intent(this, ActionActivity.class);
//            small_screen.setAction(NFW.ACTION_SMALL_SCREEN);
//
//            ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
//
//            ShortcutInfo shortcut = new ShortcutInfo.Builder(this, "id1")
//                    .setShortLabel("Small Screen")
//                    .setLongLabel("Shrink the screen")
//                    .setIcon(Icon.createWithResource(this, R.drawable.ic_action_small_screen))
//                    .setIntent(small_screen)
//                    .build();
//
//            shortcutManager.setDynamicShortcuts(Arrays.asList(shortcut));
//
//            ShortcutManager mShortcutManager =
//                    getSystemService(ShortcutManager.class);

//            if (mShortcutManager.isRequestPinShortcutSupported()) {
//                // Assumes there's already a shortcut with the ID "my-shortcut".
//                // The shortcut must be enabled.
//                ShortcutInfo pinShortcutInfo =
//                        new ShortcutInfo.Builder(this, "small_screen").build();
//
//                // Create the PendingIntent object only if your app needs to be notified
//                // that the user allowed the shortcut to be pinned. Note that, if the
//                // pinning operation fails, your app isn't notified. We assume here that the
//                // app has implemented a method called createShortcutResultIntent() that
//                // returns a broadcast intent.
//                Intent pinnedShortcutCallbackIntent =
//                        mShortcutManager.createShortcutResultIntent(pinShortcutInfo);
//
//                // Configure the intent so that your app's broadcast receiver gets
//                // the callback successfully.
//                PendingIntent successCallback = PendingIntent.getBroadcast(this, 0,
//                        pinnedShortcutCallbackIntent, 0);
//
//                mShortcutManager.requestPinShortcut(pinShortcutInfo,
//                        successCallback.getIntentSender());
//            }
//        }
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
