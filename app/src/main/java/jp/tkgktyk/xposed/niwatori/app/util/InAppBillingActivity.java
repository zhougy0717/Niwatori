package jp.tkgktyk.xposed.niwatori.app.util;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.common.base.Objects;
import com.google.common.collect.Sets;

import java.util.Set;

import jp.tkgktyk.xposed.niwatori.BuildConfig;
import jp.tkgktyk.xposed.niwatori.R;

/**
 * Created by tkgktyk on 2015/02/13.
 */
public abstract class InAppBillingActivity extends Activity {
    private static final String TAG = InAppBillingActivity.class.getSimpleName();
    // (arbitrary) request code for the purchase flow
    private static final int RC_REQUEST = 10001;
    private final Set<String> mSkuList = Sets.newHashSet();
    private IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener
            = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            // if we were disposed of in the meantime, quit.
            if (!isBillingSupported()) {
                return;
            }

            if (result.isFailure()) {
                complain("Error purchasing: " + result);
                return;
            }

            for (String sku : mSkuList) {
                if (Objects.equal(purchase.getSku(), sku)) {
                    // bought the premium settings!
                    mOnBillingEventListener.onUpdatePurchase(sku, true);
                }
            }
        }
    };
    private IabHelper mHelper;
    private OnBillingEventListener mOnBillingEventListener;

    protected void addSku(String sku) {
        mSkuList.add(sku);
    }

    protected void removeSku(String sku) {
        mSkuList.remove(sku);
    }

    protected void setOnBillingEventListener(OnBillingEventListener listener) {
        mOnBillingEventListener = listener;
    }

    protected abstract String getBase64EncodedPublicKey();

    protected boolean isBillingSupported() {
        return mHelper != null;
    }

    public void purchase(String sku) {
        if (isBillingSupported()) {
            mHelper.launchPurchaseFlow(this, sku, RC_REQUEST, mPurchaseFinishedListener);
        } else {
            Toast.makeText(this, R.string.billing_is_not_supported, Toast.LENGTH_SHORT).show();
        }
    }

    private void releaseBilling() {
        if (isBillingSupported()) {
            mHelper.dispose();
            mHelper = null;
        }
    }

    private void complain(String message) {
        Log.e(TAG, "**** In-app Billing Error: " + message);
    }

    protected abstract void initBilling();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initBilling();

        mHelper = new IabHelper(this, getBase64EncodedPublicKey());
        mHelper.enableDebugLogging(BuildConfig.DEBUG);
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    Log.d(TAG, "In-app Billing setup failed: " + result);
                    releaseBilling();
                    mOnBillingEventListener.onBillingIsUnavailable();
                } else {
                    Log.d(TAG, "In-app Billing is set up OK");
                }
                // Have we been disposed of in the meantime? If so, quit.
                if (!isBillingSupported()) {
                    return;
                }
                // Listener that's called when we finish querying the items and subscriptions we own
                mHelper.queryInventoryAsync(new IabHelper.QueryInventoryFinishedListener() {
                    public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
                        // Have we been disposed of in the meantime? If so, quit.
                        if (!isBillingSupported()) {
                            return;
                        }

                        // Is it a failure?
                        if (result.isFailure()) {
                            complain("Failed to query inventory: " + result);
                            return;
                        }

                        /*
                         * Check for items we own. Notice that for each purchase, we check
                         * the developer payload to see if it's correct! See
                         * verifyDeveloperPayload().
                         */
                        for (String sku : mSkuList) {
                            mOnBillingEventListener.onUpdatePurchase(sku, inventory.hasPurchase(sku));
                        }
                    }
                });
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releaseBilling();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!(isBillingSupported() && mHelper.handleActivityResult(requestCode, resultCode, data))) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public interface OnBillingEventListener {
        void onUpdatePurchase(String sku, boolean purchased);

        void onBillingIsUnavailable();
    }
}
