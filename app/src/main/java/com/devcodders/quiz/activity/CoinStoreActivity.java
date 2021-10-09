
package com.devcodders.quiz.activity;


import android.annotation.SuppressLint;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;

import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;

import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetailsParams;
import com.devcodders.quiz.Constant;
import com.devcodders.quiz.R;
import com.devcodders.quiz.helper.Security;
import com.devcodders.quiz.helper.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


import static com.android.billingclient.api.BillingClient.SkuType.INAPP;

public class CoinStoreActivity extends AppCompatActivity implements PurchasesUpdatedListener {
    RecyclerView recyclerView;
    public Toolbar toolbar;
    private BillingClient billingClient;
    String coinPurchase;
    public ArrayList<InApp> purchaseIds;
    ItemAdapter itemAdapter;
    public static String PRODUCT_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_coinstore);
        getAllWidgets();
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle(getString(R.string.coinstore));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        billingClient = BillingClient.newBuilder(this).enablePendingPurchases().setListener(this).build();
        itemAdapter = new ItemAdapter(setPurchaseIds());
        recyclerView.setAdapter(itemAdapter);


    }

    public void getAllWidgets() {
        toolbar = findViewById(R.id.toolBar);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(getApplicationContext(), 2));
    }

    public ArrayList<InApp> setPurchaseIds() {
        purchaseIds = new ArrayList<>();
        purchaseIds.add(new InApp("android.test.purchased", "100"));
        purchaseIds.add(new InApp("android.test.purchased", "500"));
        purchaseIds.add(new InApp("android.test.purchased", "1000"));
        purchaseIds.add(new InApp("android.test.purchased", "5000"));
        return purchaseIds;
    }

    public void BillingAssign(String id, String coins) {
        if (billingClient.isReady()) {
            PRODUCT_ID = id;
            initiatePurchase(id, coins);
        }
        //else reconnect service
        else {
            billingClient = BillingClient.newBuilder(CoinStoreActivity.this).enablePendingPurchases().setListener(CoinStoreActivity.this).build();
            billingClient.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        initiatePurchase(id, coins);
                        PRODUCT_ID = id;
                    } else {
                        Toast.makeText(getApplicationContext(), "Error " + billingResult.getDebugMessage(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onBillingServiceDisconnected() {
                }
            });
        }

    }

    private void initiatePurchase(final String PRODUCT_ID, String coins) {
        List<String> skuList = new ArrayList<>();
        skuList.add(PRODUCT_ID);
        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
        params.setSkusList(skuList).setType(INAPP);
        coinPurchase = coins;
        billingClient.querySkuDetailsAsync(params.build(),
                (billingResult, skuDetailsList) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        if (skuDetailsList != null && skuDetailsList.size() > 0) {
                            BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                                    .setSkuDetails(skuDetailsList.get(0))
                                    .build();
                            billingClient.launchBillingFlow(CoinStoreActivity.this, flowParams);
                        } else {
                            //try to add item/product id "c1" "c2" "c3" inside managed product in google play console
                            Toast.makeText(getApplicationContext(), "Purchase Item " + PRODUCT_ID + " not Found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), " Error " + billingResult.getDebugMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        billingClient.endConnection();
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> purchases) {

        //if item newly purchased
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            handlePurchases(purchases, coinPurchase);
        }
        //if item already purchased then check and reflect changes
        else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {

            billingClient.queryPurchasesAsync(INAPP, (billingResult1, list) -> handlePurchases(list, coinPurchase));

        }
        //if purchase cancelled
        else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            Toast.makeText(getApplicationContext(), "Purchase Canceled", Toast.LENGTH_SHORT).show();
        }
        // Handle any other error messages
        else {
            Toast.makeText(getApplicationContext(), "Error " + billingResult.getDebugMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    void handlePurchases(List<Purchase> purchases, String coins) {

     for (Purchase purchase : purchases) {
            //if item is purchased
            if (purchase.getSkus().contains(PRODUCT_ID) && purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                if (!verifyValidSignature(purchase.getOriginalJson(), purchase.getSignature())) {
                    // Invalid purchase
                    // show error to user
                    Toast.makeText(getApplicationContext(), "Error : Invalid Purchase", Toast.LENGTH_SHORT).show();
                    return;
                }else {
                    Utils.UpdateCoin(getApplicationContext(), "+" + coins);
                }

                // else purchase is valid
                //if item is purchased and not consumed
                if (!purchase.isAcknowledged()) {
                    ConsumeParams consumeParams = ConsumeParams.newBuilder()
                            .setPurchaseToken(purchase.getPurchaseToken())
                            .build();

                    billingClient.consumeAsync(consumeParams, consumeListener);
                }
            }
            //if purchase is pending
            else if (purchase.getSkus().contains(PRODUCT_ID) && purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
                Toast.makeText(getApplicationContext(),
                        "Purchase is Pending. Please complete Transaction", Toast.LENGTH_SHORT).show();
            }
            //if purchase is refunded or unknown
            else if (purchase.getSkus().contains(PRODUCT_ID) && purchase.getPurchaseState() == Purchase.PurchaseState.UNSPECIFIED_STATE) {
                Toast.makeText(getApplicationContext(), "Purchase Status Unknown", Toast.LENGTH_SHORT).show();
            }
        }
    }


    ConsumeResponseListener consumeListener = (billingResult, purchaseToken) -> {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {

            Toast.makeText(getApplicationContext(), "Item Consumed", Toast.LENGTH_SHORT).show();

        }
    };

    /**
     * Verifies that the purchase was signed correctly for this developer's public key.
     * &lt;p>Note: It's strongly recommended to perform such check on your backend since hackers can
     * replace this method with "constant true" if they decompile/rebuild your app.
     * &lt;/p>
     */
    private boolean verifyValidSignature(String signedData, String signature) {
        try {
            //for old playconsole
            // To get key go to Developer Console > Select your app > Development Tools > Services & APIs.
            //for new play console
            //To get key go to Developer Console > Select your app > Monetize > Monetization setup

            String base64Key = "Add your Base64Key!!";
            return Security.verifyPurchase(base64Key, signedData, signature);
        } catch (IOException e) {
            return false;
        }
    }

    public class ItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private ArrayList<InApp> purchaseIds;


        public ItemAdapter(ArrayList<InApp> purchaseIds) {
            this.purchaseIds = purchaseIds;

        }


        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.lyt_in_app_product, parent, false);
            return new ItemRowHolder(v);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder1, int position) {
            ItemRowHolder holder = (ItemRowHolder) holder1;
            final InApp inApp = purchaseIds.get(position);
            holder.lytBg.setBackgroundResource(Constant.gradientBG[position % 4]);
            holder.tvCoins.setText(inApp.getCoins() + getString(R.string._coins));
            holder.btnGet.setOnClickListener(v -> {
                BillingAssign(inApp.getId(), inApp.getCoins());
            });

        }

        @Override
        public int getItemCount() {
            return purchaseIds.size();
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        public class ItemRowHolder extends RecyclerView.ViewHolder {
            TextView tvCoins;
            Button btnGet;
            RelativeLayout lytBg;

            public ItemRowHolder(View itemView) {
                super(itemView);
                tvCoins = itemView.findViewById(R.id.tvCoins);
                btnGet = itemView.findViewById(R.id.btnGet);
                lytBg = itemView.findViewById(R.id.lytBg);

            }
        }
    }

    public class InApp {
        String id, coins;

        public InApp(String id, String coins) {
            this.id = id;
            this.coins = coins;
        }

        public String getId() {
            return id;
        }

        public String getCoins() {
            return coins;
        }
    }
}