package com.example.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.example.data.preferences.SettingsPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProPlan(
    val productId: String,
    val title: String,
    val price: String,
    val productType: String,
    val offerToken: String?,
    val productDetails: ProductDetails
)

class ProBillingManager(
    context: Context,
    private val settings: SettingsPreferences
) : PurchasesUpdatedListener {

    companion object {
        const val MONTHLY_PRODUCT_ID = "strict_blocker_pro_monthly"
        const val YEARLY_PRODUCT_ID = "strict_blocker_pro_yearly"
        const val LIFETIME_PRODUCT_ID = "strict_blocker_pro_lifetime"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _plans = MutableStateFlow<List<ProPlan>>(emptyList())
    val plans: StateFlow<List<ProPlan>> = _plans.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val billingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .enableAutoServiceReconnection()
        .build()

    init {
        connect()
    }

    private fun connect() {
        if (billingClient.isReady) {
            _isReady.value = true
            refreshProducts()
            restorePurchases()
            return
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                _isReady.value =
                    billingResult.responseCode == BillingClient.BillingResponseCode.OK
                if (_isReady.value) {
                    refreshProducts()
                    restorePurchases()
                } else {
                    _message.value = billingResult.debugMessage.ifBlank {
                        "Google Play Billing is unavailable."
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                _isReady.value = false
            }
        })
    }

    fun refreshProducts() {
        if (!billingClient.isReady) return

        val products = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(MONTHLY_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(YEARLY_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(LIFETIME_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(products)
            .build()

        billingClient.queryProductDetailsAsync(params) { result, detailsResult ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                _message.value = result.debugMessage
                return@queryProductDetailsAsync
            }

            _plans.value = detailsResult.productDetailsList.mapNotNull { details ->
                val productType = details.productType
                val subscriptionOffer = details.subscriptionOfferDetails?.firstOrNull()
                val oneTimeOffer =
                    details.oneTimePurchaseOfferDetailsList?.firstOrNull()
                        ?: details.oneTimePurchaseOfferDetails

                val price = when (productType) {
                    BillingClient.ProductType.SUBS ->
                        subscriptionOffer?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice
                    BillingClient.ProductType.INAPP -> oneTimeOffer?.formattedPrice
                    else -> null
                } ?: return@mapNotNull null

                val offerToken = when (productType) {
                    BillingClient.ProductType.SUBS -> subscriptionOffer?.offerToken
                    BillingClient.ProductType.INAPP -> oneTimeOffer?.offerToken
                    else -> null
                }

                ProPlan(
                    productId = details.productId,
                    title = when (details.productId) {
                        YEARLY_PRODUCT_ID -> "Yearly"
                        LIFETIME_PRODUCT_ID -> "Lifetime"
                        else -> "Monthly"
                    },
                    price = price,
                    productType = productType,
                    offerToken = offerToken,
                    productDetails = details
                )
            }.sortedBy { plan ->
                when (plan.productId) {
                    YEARLY_PRODUCT_ID -> 0
                    LIFETIME_PRODUCT_ID -> 1
                    else -> 2
                }
            }
        }
    }

    fun launchPurchase(activity: Activity, plan: ProPlan) {
        val productBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(plan.productDetails)
        if (!plan.offerToken.isNullOrBlank()) {
            productBuilder.setOfferToken(plan.offerToken)
        }
        val productParams = productBuilder.build()

        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()

        val result = billingClient.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _message.value = result.debugMessage.ifBlank {
                "Unable to start purchase."
            }
        }
    }

    fun restorePurchases() {
        if (!billingClient.isReady) return

        val subscriptionParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(subscriptionParams) { subscriptionResult, subscriptions ->
            if (subscriptionResult.responseCode != BillingClient.BillingResponseCode.OK) {
                _message.value = subscriptionResult.debugMessage
            }

            val inAppParams = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()

            billingClient.queryPurchasesAsync(inAppParams) { inAppResult, oneTimePurchases ->
                if (inAppResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    _message.value = inAppResult.debugMessage
                }

                val active = buildList {
                    if (subscriptionResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        addAll(subscriptions)
                    }
                    if (inAppResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        addAll(oneTimePurchases)
                    }
                }
                processPurchases(active)
            }
        }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> processPurchases(purchases.orEmpty())
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            else -> {
                _message.value = billingResult.debugMessage.ifBlank {
                    "Purchase could not be completed."
                }
            }
        }
    }

    private fun processPurchases(purchases: List<Purchase>) {
        val validProducts = setOf(MONTHLY_PRODUCT_ID, YEARLY_PRODUCT_ID, LIFETIME_PRODUCT_ID)
        val activePurchases = purchases.filter { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                purchase.products.any { it in validProducts }
        }

        scope.launch {
            settings.setProEntitled(activePurchases.isNotEmpty())
        }

        activePurchases.forEach { purchase ->
            if (!purchase.isAcknowledged) {
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient.acknowledgePurchase(params) { result ->
                    if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                        _message.value = result.debugMessage
                    }
                }
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun close() {
        billingClient.endConnection()
    }
}
