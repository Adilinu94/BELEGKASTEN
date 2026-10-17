package de.belegkasten.core.license

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import com.android.billingclient.api.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext context: Context, private val license: LicenseManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutex = Mutex()
    private val publicKey = runCatching {
        val info = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        context.getString(info.metaData.getInt("de.belegkasten.PLAY_PUBLIC_KEY"))
    }.getOrDefault("")
    val message = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)
    private val client = BillingClient.newBuilder(context).setListener(::onPurchasesUpdated)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()).build()

    private suspend fun connect(): Boolean {
        if (client.isReady) return true
        return withTimeoutOrNull(15000) { suspendCancellableCoroutine { continuation ->
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) { if (continuation.isActive) continuation.resume(result.responseCode == BillingClient.BillingResponseCode.OK) }
                override fun onBillingServiceDisconnected() { if (continuation.isActive) continuation.resume(false) }
            })
        } } ?: false
    }

    fun restore() = scope.launch {
        mutex.withLock {
            busy.value = true
            try {
                if (!connect()) { message.value = "Play Store nicht erreichbar. Das Archiv bleibt frei; vorhandener Kauf gilt offline weiter."; return@withLock }
                val (result, purchases) = withTimeout(15000) { suspendCancellableCoroutine<Pair<BillingResult, List<Purchase>>> { c ->
                    client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()) { r, p -> if (c.isActive) c.resume(r to p) }
                } }
                if (result.responseCode != BillingClient.BillingResponseCode.OK) { message.value = "Käufe konnten nicht geprüft werden"; return@withLock }
                var found = false
                for (purchase in purchases) if (accept(purchase)) found = true
                if (found) message.value = "Einmalkauf wiederhergestellt"
                else if (purchases.none { LicenseManager.SKU_LIFETIME in it.products }) {
                    license.clearVerifiedMissingPurchase()
                    message.value = "Kein abgeschlossener Einmalkauf für dieses Play-Konto gefunden. Das Archiv bleibt frei."
                }
            } catch (e: Exception) { if (e is CancellationException && e !is TimeoutCancellationException) throw e; message.value = "Kaufprüfung fehlgeschlagen. Bitte später erneut versuchen." }
            finally { busy.value = false }
        }
    }

    fun purchase(activity: Activity) = scope.launch {
        mutex.withLock {
            busy.value = true
            try {
                if (publicKey.isBlank()) { message.value = "Kauf noch nicht eingerichtet: Play-Console-Prüfschlüssel fehlt. Kein Kaufdialog geöffnet."; return@withLock }
                if (activity.isFinishing || activity.isDestroyed) return@withLock
                if (!connect()) { message.value = "Play Store nicht erreichbar. Bitte Verbindung und Konto prüfen."; return@withLock }
                val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(
                    QueryProductDetailsParams.Product.newBuilder().setProductId(LicenseManager.SKU_LIFETIME).setProductType(BillingClient.ProductType.INAPP).build())).build()
                val (result, products) = withTimeout(15000) { suspendCancellableCoroutine<Pair<BillingResult, List<ProductDetails>>> { c ->
                    client.queryProductDetailsAsync(params) { r, p -> if (c.isActive) c.resume(r to p.productDetailsList) }
                } }
                val product = products.firstOrNull()
                if (result.responseCode != BillingClient.BillingResponseCode.OK || product == null) {
                    message.value = "Produkt noch nicht verfügbar. Play-Console-Konfiguration und Installation über den Testkanal erforderlich."; return@withLock
                }
                val details = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product)
                product.oneTimePurchaseOfferDetailsList?.firstOrNull()?.offerToken?.let(details::setOfferToken)
                val flow = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(details.build())).build()
                val launch = client.launchBillingFlow(activity, flow)
                if (launch.responseCode != BillingClient.BillingResponseCode.OK) message.value = "Kaufdialog konnte nicht geöffnet werden"
            } catch (e: Exception) { if (e is CancellationException && e !is TimeoutCancellationException) throw e; message.value = "Kauf nicht verfügbar. Dein Archiv bleibt nutzbar." }
            finally { busy.value = false }
        }
    }

    private fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) { message.value = "Kauf abgebrochen"; return }
        if (result.responseCode != BillingClient.BillingResponseCode.OK) { message.value = "Kauf fehlgeschlagen; kein neuer Belegzugriff gesperrt"; return }
        scope.launch { mutex.withLock {
            try { purchases.orEmpty().forEach { accept(it) } }
            catch (e: Exception) { if (e is CancellationException && e !is TimeoutCancellationException) throw e; message.value = "Kaufbestätigung fehlgeschlagen. Bitte Käufe wiederherstellen." }
        } }
    }

    private suspend fun accept(purchase: Purchase): Boolean {
        if (LicenseManager.SKU_LIFETIME !in purchase.products) return false
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) { message.value = "Zahlung ausstehend. Freischaltung nach Abschluss."; return false }
        if (!verifyPurchaseSignature(publicKey, purchase.originalJson, purchase.signature)) {
            message.value = "Kaufsignatur konnte nicht geprüft werden. Play-Prüfschlüssel und Testkanal konfigurieren; keine Freischaltung."
            return false
        }
        if (!purchase.isAcknowledged) {
            val result = withTimeout(15000) { suspendCancellableCoroutine<BillingResult> { c ->
                client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()) { if (c.isActive) c.resume(it) }
            } }
            if (result.responseCode != BillingClient.BillingResponseCode.OK) { message.value = "Kauf noch nicht bestätigt. Bitte wiederherstellen."; return false }
        }
        license.markPurchased()
        message.value = "Einmalkauf aktiv"
        return true
    }
}
