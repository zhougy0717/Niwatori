package jp.tkgktyk.xposed.niwatori.app;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.LruCache;

/**
 * Created by tkgktyk on 2015/06/06.
 */
public class IconCache {
    public static final String ACTION_ICON_CACHED = "iconChached";

    private final int mIconSize;
    private final LruCache<String, Bitmap> mMemoryCache;

    public IconCache(Context context) {
        mIconSize = context.getResources().getDimensionPixelSize(android.R.dimen.app_icon_size);

//        int max = (int) (Runtime.getRuntime().maxMemory() / 1024); // KB
//        mMemoryCache = new LruCache<String, Bitmap>(max / 16) {
//            @Override
//            protected int sizeOf(String key, Bitmap value) {
//                return value.getByteCount() / 1024; // KB
//            }
//
//            @Override
//            protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
//                if (!oldValue.isRecycled()) {
//                    oldValue.recycle();
//                }
//            }
//        };

        mMemoryCache = new LruCache<String, Bitmap>(30) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return 1;
            }
        };
    }

    public Bitmap get(String packageName) {
        return mMemoryCache.get(packageName);
    }

    public void put(String packageName, Bitmap bitmap) {
        synchronized (mMemoryCache) {
            if (get(packageName) == null) {
                mMemoryCache.put(packageName, bitmap);
            }
        }
    }

    public void loadAsync(Context context, String packageName) {
        new BitmapWorkerTask(context).execute(packageName);
    }

    public void evict() {
        mMemoryCache.evictAll();
    }

    private class BitmapWorkerTask extends AsyncTask<String, Void, Void> {
        private final Context mContext;

        public BitmapWorkerTask(Context context) {
            mContext = context;
        }

        @Override
        protected Void doInBackground(String... params) {
            String packageName = params[0];
            Drawable drawable = null;
            try {
                drawable = mContext.getPackageManager().getApplicationIcon(packageName);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            if (drawable != null && drawable instanceof BitmapDrawable) {
                put(packageName, resize(((BitmapDrawable) drawable).getBitmap()));
            }
            return null;
        }

        private Bitmap resize(Bitmap bitmap) {
            int size = Math.max(bitmap.getHeight(), bitmap.getWidth());
            if (size <= mIconSize) {
                return bitmap;
            }
            float downScale = ((float) mIconSize) / size;
            Matrix matrix = new Matrix();
            matrix.postScale(downScale, downScale);
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                    bitmap.getHeight(), matrix, true);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            LocalBroadcastManager.getInstance(mContext)
                    .sendBroadcast(new Intent(ACTION_ICON_CACHED));
        }
    }

}
