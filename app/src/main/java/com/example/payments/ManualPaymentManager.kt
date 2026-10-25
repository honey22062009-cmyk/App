package com.example.payments

import android.content.Context
import android.provider.Settings
import com.example.data.preferences.SettingsPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import kotlinx.coroutines.launch

enum class ManualPlan(
    val id: String,
    val label: String,
    val amountRupees: Int,
    val durationDays: Int
) {
    MONTHLY("monthly", "Monthly", 39, 30),
    YEARLY("yearly", "Yearly", 599, 365);

    companion object {
        fun fromId(id: String?): ManualPlan? = entries.firstOrNull { it.id == id }
    }
}

data class PaymentRequest(
    val id: String,
    val userUid: String,
    val planId: String,
    val amountRupees: Int,
    val transactionId: String,
    val deviceHash: String,
    val status: String,
    val createdAtMillis: Long,
    val expiresAtMillis: Long?
) {
    val plan: ManualPlan?
        get() = ManualPlan.fromId(planId)
}

class ManualPaymentManager(
    context: Context,
    private val settings: SettingsPreferences
) {
    companion object {
        const val UPI_ID = "honey22062009@oksbi"
        private const val COLLECTION = "payment_requests"
        private const val ADMINS = "admins"
        private const val STATUS_PENDING = "pending"
        private const val STATUS_APPROVED = "approved"
        private const val STATUS_REJECTED = "rejected"
    }

    private val appContext = context.applicationContext
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private var userListener: ListenerRegistration? = null
    private var ownerListener: ListenerRegistration? = null
    private val secureRandom = SecureRandom()

    fun newRequestId(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return buildString {
            append("SB-")
            repeat(8) {
                append(alphabet[secureRandom.nextInt(alphabet.length)])
            }
        }
    }

    fun ensureUserSession(onResult: (Result<String>) -> Unit) {
        val current = auth.currentUser
        if (current != null) {
            onResult(Result.success(current.uid))
            return
        }
        auth.signInAnonymously()
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid.isNullOrBlank()) {
                    onResult(Result.failure(IllegalStateException("Firebase did not return a user ID.")))
                } else {
                    onResult(Result.success(uid))
                }
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun submitPaymentRequest(
        requestId: String,
        plan: ManualPlan,
        transactionId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val cleanedTransaction = transactionId.trim().replace("\\s+".toRegex(), "")
        if (cleanedTransaction.length !in 6..64) {
            onResult(Result.failure(IllegalArgumentException("Enter the UPI transaction ID / UTR shown by your payment app.")))
            return
        }

        ensureUserSession { authResult ->
            authResult.fold(
                onSuccess = { uid ->
                    val payload = hashMapOf<String, Any>(
                        "requestId" to requestId,
                        "userUid" to uid,
                        "planId" to plan.id,
                        "amountRupees" to plan.amountRupees,
                        "durationDays" to plan.durationDays,
                        "transactionId" to cleanedTransaction,
                        "deviceHash" to deviceHash(),
                        "status" to STATUS_PENDING,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "createdAtClientMillis" to System.currentTimeMillis()
                    )

                    firestore.collection(COLLECTION)
                        .document(requestId)
                        .set(payload)
                        .addOnSuccessListener {
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                settings.setLastPaymentRequestId(requestId)
                            }
                            onResult(Result.success(Unit))
                        }
                        .addOnFailureListener { onResult(Result.failure(it)) }
                },
                onFailure = { onResult(Result.failure(it)) }
            )
        }
    }

    fun observeUserRequest(
        requestId: String,
        onUpdate: (PaymentRequest?) -> Unit,
        onError: (Throwable) -> Unit = {}
    ) {
        userListener?.remove()
        if (requestId.isBlank()) {
            onUpdate(null)
            return
        }

        ensureUserSession { authResult ->
            if (authResult.isFailure) {
                onError(authResult.exceptionOrNull() ?: IllegalStateException("Unable to connect to Firebase."))
                return@ensureUserSession
            }

            userListener = firestore.collection(COLLECTION)
                .document(requestId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        onError(error)
                        return@addSnapshotListener
                    }
                    if (snapshot == null || !snapshot.exists()) {
                        onUpdate(null)
                        return@addSnapshotListener
                    }

                    val request = snapshot.toPaymentRequest()
                    onUpdate(request)

                    if (
                        request != null &&
                        request.status == STATUS_APPROVED &&
                        request.deviceHash == deviceHash() &&
                        request.expiresAtMillis != null
                    ) {
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            settings.setManualProEntitlement(
                                requestId = request.id,
                                expiresAtMillis = request.expiresAtMillis
                            )
                        }
                    }
                }
        }
    }

    fun ownerSignIn(email: String, password: String, onResult: (Result<Unit>) -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            onResult(Result.failure(IllegalArgumentException("Enter the owner email and password.")))
            return
        }

        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid.isNullOrBlank()) {
                    onResult(Result.failure(IllegalStateException("Owner account did not return a user ID.")))
                    return@addOnSuccessListener
                }

                firestore.collection(ADMINS).document(uid).get()
                    .addOnSuccessListener { admin ->
                        if (admin.exists()) {
                            onResult(Result.success(Unit))
                        } else {
                            auth.signOut()
                            onResult(Result.failure(IllegalStateException("This Firebase account is not marked as a Strict Blocker owner.")))
                        }
                    }
                    .addOnFailureListener {
                        auth.signOut()
                        onResult(Result.failure(it))
                    }
            }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun observePendingOwnerRequests(
        onUpdate: (List<PaymentRequest>) -> Unit,
        onError: (Throwable) -> Unit = {}
    ) {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            onError(IllegalStateException("Owner is not signed in."))
            return
        }

        firestore.collection(ADMINS).document(uid).get()
            .addOnSuccessListener { admin ->
                if (!admin.exists()) {
                    onError(IllegalStateException("This account is not an owner."))
                    return@addOnSuccessListener
                }

                ownerListener?.remove()
                ownerListener = firestore.collection(COLLECTION)
                    .whereEqualTo("status", STATUS_PENDING)
                    .addSnapshotListener { snapshots, error ->
                        if (error != null) {
                            onError(error)
                            return@addSnapshotListener
                        }
                        val requests = snapshots?.documents
                            ?.mapNotNull { it.toPaymentRequest() }
                            ?.sortedByDescending { it.createdAtMillis }
                            .orEmpty()
                        onUpdate(requests)
                    }
            }
            .addOnFailureListener { onError(it) }
    }

    fun approve(request: PaymentRequest, onResult: (Result<Unit>) -> Unit) {
        val plan = request.plan
        if (plan == null) {
            onResult(Result.failure(IllegalArgumentException("Unknown plan.")))
            return
        }

        val expiresAt = System.currentTimeMillis() + plan.durationDays * 24L * 60L * 60L * 1000L
        firestore.collection(COLLECTION)
            .document(request.id)
            .update(
                mapOf(
                    "status" to STATUS_APPROVED,
                    "approvedAt" to FieldValue.serverTimestamp(),
                    "expiresAtMillis" to expiresAt
                )
            )
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun reject(request: PaymentRequest, onResult: (Result<Unit>) -> Unit) {
        firestore.collection(COLLECTION)
            .document(request.id)
            .update(
                mapOf(
                    "status" to STATUS_REJECTED,
                    "reviewedAt" to FieldValue.serverTimestamp()
                )
            )
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun ownerSignOut() {
        ownerListener?.remove()
        ownerListener = null
        auth.signOut()
    }

    fun close() {
        userListener?.remove()
        ownerListener?.remove()
        userListener = null
        ownerListener = null
    }

    private fun deviceHash(): String {
        val androidId = Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
            .orEmpty()
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(("${appContext.packageName}:$androidId").toByteArray())
        return bytes.joinToString("") { "%02x".format(Locale.US, it) }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toPaymentRequest(): PaymentRequest? {
        val requestId = getString("requestId") ?: id
        val userUid = getString("userUid") ?: return null
        val planId = getString("planId") ?: return null
        val amount = getLong("amountRupees")?.toInt() ?: return null
        val transactionId = getString("transactionId") ?: return null
        val deviceHash = getString("deviceHash") ?: return null
        val status = getString("status") ?: STATUS_PENDING
        val createdAtMillis = getTimestamp("createdAt")?.toDate()?.time
            ?: getLong("createdAtClientMillis")
            ?: 0L
        val expiresAt = getLong("expiresAtMillis")

        return PaymentRequest(
            id = requestId,
            userUid = userUid,
            planId = planId,
            amountRupees = amount,
            transactionId = transactionId,
            deviceHash = deviceHash,
            status = status,
            createdAtMillis = createdAtMillis,
            expiresAtMillis = expiresAt
        )
    }
}
