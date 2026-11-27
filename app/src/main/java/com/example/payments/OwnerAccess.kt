package com.example.payments

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory access granted only by a server-verified admin document for the current UID. */
object OwnerAccess {
    private val access = MutableStateFlow(false)
    val hasAccess = access.asStateFlow()
    private var verifiedUid: String? = null
    private var initialized = false

    fun restore() {
        val auth = FirebaseAuth.getInstance()
        if (!initialized) {
            initialized = true
            auth.addAuthStateListener { current ->
                if (current.currentUser?.uid != verifiedUid) clear()
            }
        }
        verify {}
    }

    fun verify(onResult: (Boolean) -> Unit) {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user == null || user.isAnonymous) {
            clear()
            onResult(false)
            return
        }
        val uid = user.uid
        FirebaseFirestore.getInstance().collection("admins").document(uid)
            .get(Source.SERVER)
            .addOnSuccessListener { document ->
                val valid = document.exists() && auth.currentUser?.uid == uid
                verifiedUid = if (valid) uid else null
                access.value = valid
                onResult(valid)
            }
            .addOnFailureListener {
                clear()
                onResult(false)
            }
    }

    fun clear() {
        verifiedUid = null
        access.value = false
    }
}
