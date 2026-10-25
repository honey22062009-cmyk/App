package com.example.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
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
    val offerToken: String,
    val productDetails: ProductDetails
)

class ProBillingManager(
    context: Context,
    private val settings: SettingsPreferences
) : PurchasesUpdatedListener {

    companion object {
        const val MONTHLY_PRODUCT_ID = "strict_blocker_pro_monthly"
        const val YEARLY_PRODUCT_ID = "strict_blocker_pro_yearly"
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
        .enablePendingPurchases()
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

        val products = listOf(MONTHLY_PRODUCT_ID, YEARLY_PRODUCT_ID).map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(products)
            .build()

        billingClient.queryProductDetailsAsync(params) { result, detailsResult ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                _message.value = result.debugMessage
                return@queryProductDetailsAsync
            }

            _plans.value = detailsResult.productDetailsList.mapNotNull { details ->
                val offer = details.subscriptionOfferDetails?.firstOrNull() ?: return@mapNotNull null
                val pricingPhase = offer.pricingPhases.pricingPhaseList.lastOrNull()
                    ?: return@mapNotNull null

                ProPlan(
                    productId = details.productId,
                    title = when (details.productId) {
                        YEARLY_PRODUCT_ID -> "Yearly"
                        else -> "Monthly"
                    },
                    price = pricingPhase.formattedPrice,
                    offerToken = offer.offerToken,
                    productDetails = details
                )
            }.sortedBy { it.productId != YEARLY_PRODUCT_ID }
        }
    }

    fun launchPurchase(activity: Activity, plan: ProPlan) {
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(plan.productDetails)
            .setOfferToken(plan.offerToken)
            .build()

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

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                _message.value = result.debugMessage
                return@queryPurchasesAsync
            }
            processPurchases(purchases)
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
        val validProducts = setOf(MONTHLY_PRODUCT_ID, YEARLY_PRODUCT_ID)
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
