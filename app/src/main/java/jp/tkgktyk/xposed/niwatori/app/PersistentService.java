/*
 * Copyright 2015 Takagi Katsuyuki
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.tkgktyk.xposed.niwatori.app;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import jp.tkgktyk.xposed.niwatori.NFW;
import jp.tkgktyk.xposed.niwatori.R;

/**
 * Created by tkgktyk on 2015/06/09.
 */
public class PersistentService extends Service {
    public static final String ACTION_SET_RESIZED = NFW.PREFIX_ACTION + "SET_RESIZED";
    public static final String ACTION_UNSET_RESIZED = NFW.PREFIX_ACTION + "UNSET_RESIZED";
    public static final String ACTION_REQUEST_RESIZE = NFW.PREFIX_ACTION + "REQUEST_RESIZE";

    private boolean mResized;

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_SET_RESIZED)) {
                mResized = true;
            } else if (action.equals(ACTION_UNSET_RESIZED)) {
                mResized = false;
            } else if (action.equals(ACTION_REQUEST_RESIZE)) {
                if (mResized) {
                    sendBroadcast(new Intent(NFW.ACTION_FORCE_SMALL_SCREEN));
                } else {
                    sendBroadcast(new Intent(NFW.ACTION_SOFT_RESET));
                }
            }
        }
    };

    private ServiceNotification mServiceNotification;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mServiceNotification = new ServiceNotification(this, R.mipmap.ic_launcher,
                R.string.app_name, SettingsActivity.class);
        mServiceNotification.update(R.string.state_persistent_small_screen);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_SET_RESIZED);
        filter.addAction(ACTION_UNSET_RESIZED);
        filter.addAction(ACTION_REQUEST_RESIZE);
        registerReceiver(mBroadcastReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mServiceNotification.stop();

        unregisterReceiver(mBroadcastReceiver);
    }
}
