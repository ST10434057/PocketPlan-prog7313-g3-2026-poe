package com.example.prog7313poepart1.data.firebase

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class FirebaseRepository(private val context: Context) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    private fun requireUid(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User is not logged in")
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    fun getCurrentUserEmail(): String? {
        return auth.currentUser?.email
    }

    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    private suspend fun Query.getServerThenCache(): QuerySnapshot {
        return try {
            get(Source.SERVER).await()
        } catch (serverError: Exception) {
            try {
                get(Source.CACHE).await()
            } catch (_: Exception) {
                throw serverError
            }
        }
    }

    private suspend fun DocumentReference.getServerThenCache(): DocumentSnapshot {
        return try {
            get(Source.SERVER).await()
        } catch (serverError: Exception) {
            try {
                get(Source.CACHE).await()
            } catch (_: Exception) {
                throw serverError
            }
        }
    }

    private fun isOfflineReadFailure(error: Exception): Boolean {
        val message = error.message.orEmpty()
        return message.contains("offline", ignoreCase = true) ||
                message.contains("unavailable", ignoreCase = true) ||
                message.contains("network", ignoreCase = true)
    }

    suspend fun registerUser(email: String, password: String): String {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid
            ?: throw IllegalStateException("Could not create Firebase user")

        val profile = FirebaseUserProfile(
            uid = uid,
            email = email,
            createdAt = System.currentTimeMillis()
        )

        db.collection("users")
            .document(uid)
            .set(profile)
            .await()

        createDefaultCategories(uid)

        return uid
    }

    suspend fun loginUser(email: String, password: String): String {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid
            ?: throw IllegalStateException("Could not log in")

        // Non-critical: ensure profile exists when online; never block a valid auth
        try { ensureUserProfileExists(uid, email) } catch (_: Exception) {}
        return uid
    }

    suspend fun signInWithGoogle(idToken: String): Pair<String, String> {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val uid = result.user?.uid
            ?: throw IllegalStateException("Google sign-in failed: no user returned")
        val email = result.user?.email ?: ""
        // Non-critical: ensure profile exists when online; never block a valid auth
        try { ensureUserProfileExists(uid, email) } catch (_: Exception) {}
        return Pair(uid, email)
    }

    fun logout() {
        auth.signOut()
    }

    private suspend fun ensureUserProfileExists(uid: String, email: String) {
        val userRef = db.collection("users").document(uid)
        val snapshot = userRef.getServerThenCache()

        if (!snapshot.exists()) {
            val profile = FirebaseUserProfile(
                uid = uid,
                email = email,
                createdAt = System.currentTimeMillis()
            )

            userRef.set(profile).await()
            createDefaultCategories(uid)
        }
    }

    private suspend fun createDefaultCategories(uid: String) {
        val existingCategories = db.collection("users")
            .document(uid)
            .collection("categories")
            .get()
            .await()

        if (!existingCategories.isEmpty) {
            return
        }

        val defaultCategories = listOf(
            "Food",
            "Transport",
            "Book/Study"
        )

        for (categoryName in defaultCategories) {
            addCategoryForUser(uid, categoryName)
        }
    }

    private suspend fun addCategoryForUser(uid: String, name: String): String {
        val categoryRef = db.collection("users")
            .document(uid)
            .collection("categories")
            .document()

        val category = FirebaseCategory(
            id = categoryRef.id,
            name = name.trim(),
            userId = uid,
            createdAt = System.currentTimeMillis()
        )

        categoryRef.set(category).await()
        return categoryRef.id
    }

    suspend fun addCategory(name: String): String {
        val uid = requireUid()
        val cleanName = name.trim()

        if (cleanName.isEmpty()) {
            throw IllegalArgumentException("Category name cannot be empty")
        }

        val existing = db.collection("users")
            .document(uid)
            .collection("categories")
            .whereEqualTo("name", cleanName)
            .get()
            .await()

        if (!existing.isEmpty) {
            throw IllegalArgumentException("Category already exists")
        }

        return addCategoryForUser(uid, cleanName)
    }

    suspend fun getCategories(): List<FirebaseCategory> {
        val uid = requireUid()

        // Ensure default categories exist if none are found
        createDefaultCategories(uid)

        val snapshot = try {
            db.collection("users")
                .document(uid)
                .collection("categories")
                .orderBy("name", Query.Direction.ASCENDING)
                .getServerThenCache()
        } catch (e: Exception) {
            if (isOfflineReadFailure(e)) {
                return emptyList()
            }
            throw e
        }

        return snapshot.documents.mapNotNull { document ->
            document.toObject(FirebaseCategory::class.java)
        }
    }

    suspend fun uploadReceiptImage(imageUri: Uri): String {
        val uid = requireUid()

        val fileName = "receipt_${System.currentTimeMillis()}.jpg"

        val imageRef = storage.reference
            .child("users")
            .child(uid)
            .child("receipts")
            .child(fileName)

        imageRef.putFile(imageUri).await()

        return imageRef.downloadUrl.await().toString()
    }

    suspend fun addExpense(expense: FirebaseExpense, receiptUri: Uri? = null): String {
        val uid = requireUid()

        val expenseRef = db.collection("users")
            .document(uid)
            .collection("expenses")
            .document()

        // Receipt upload is optional — a failure must never block saving the expense
        val receiptUrl = if (receiptUri != null) {
            try {
                uploadReceiptImage(receiptUri)
            } catch (e: Exception) {
                android.util.Log.w("FirebaseRepository", "Receipt upload failed, saving without image: ${e.message}")
                ""
            }
        } else {
            ""
        }

        val finalExpense = expense.copy(
            id = expenseRef.id,
            userId = uid,
            receiptImageUrl = receiptUrl,
            createdAt = System.currentTimeMillis()
        )

        expenseRef.set(finalExpense).await()
        return expenseRef.id
    }

    suspend fun getExpenses(): List<FirebaseExpense> {
        val uid = requireUid()

        val snapshot = try {
            db.collection("users")
                .document(uid)
                .collection("expenses")
                .orderBy("date", Query.Direction.DESCENDING)
                .getServerThenCache()
        } catch (e: Exception) {
            if (isOfflineReadFailure(e)) {
                return emptyList()
            }
            throw e
        }

        return snapshot.documents.mapNotNull { document ->
            document.toObject(FirebaseExpense::class.java)
        }
    }

    suspend fun getExpensesBetween(startDate: String, endDate: String): List<FirebaseExpense> {
        val uid = requireUid()

        val snapshot = try {
            db.collection("users")
                .document(uid)
                .collection("expenses")
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .getServerThenCache()
        } catch (e: Exception) {
            if (isOfflineReadFailure(e)) {
                return emptyList()
            }
            throw e
        }

        return snapshot.documents
            .mapNotNull { document -> document.toObject(FirebaseExpense::class.java) }
            .sortedByDescending { it.date }
    }

    suspend fun saveGoal(goal: FirebaseGoal): String {
        val uid = requireUid()

        if (goal.month.isBlank()) {
            throw IllegalArgumentException("Goal month cannot be empty")
        }

        val goalRef = db.collection("users")
            .document(uid)
            .collection("goals")
            .document(goal.month)

        val finalGoal = goal.copy(
            id = goal.month,
            userId = uid,
            createdAt = System.currentTimeMillis()
        )

        goalRef.set(finalGoal).await()
        return goal.month
    }

    suspend fun getGoal(month: String): FirebaseGoal? {
        val uid = requireUid()

        val snapshot = try {
            db.collection("users")
                .document(uid)
                .collection("goals")
                .document(month)
                .getServerThenCache()
        } catch (e: Exception) {
            if (isOfflineReadFailure(e)) {
                return null
            }
            throw e
        }

        return snapshot.toObject(FirebaseGoal::class.java)
    }

    suspend fun saveReward(reward: FirebaseReward): String {
        val uid = requireUid()

        val rewardId = if (reward.id.isNotBlank()) {
            reward.id
        } else {
            reward.name.lowercase().replace(" ", "_")
        }

        val rewardRef = db.collection("users")
            .document(uid)
            .collection("rewards")
            .document(rewardId)

        val finalReward = reward.copy(
            id = rewardId,
            userId = uid,
            createdAt = System.currentTimeMillis()
        )

        rewardRef.set(finalReward).await()
        return rewardId
    }

    suspend fun getRewards(): List<FirebaseReward> {
        val uid = requireUid()

        val snapshot = try {
            db.collection("users")
                .document(uid)
                .collection("rewards")
                .getServerThenCache()
        } catch (e: Exception) {
            if (isOfflineReadFailure(e)) {
                return emptyList()
            }
            throw e
        }

        return snapshot.documents.mapNotNull { document ->
            document.toObject(FirebaseReward::class.java)
        }
    }

    suspend fun getCategoryTotals(startDate: String, endDate: String): List<FirebaseCategoryTotal> {
        val expenses = getExpensesBetween(startDate, endDate)

        return expenses
            .groupBy { it.categoryName }
            .map { entry ->
                FirebaseCategoryTotal(
                    categoryName = entry.key,
                    total = entry.value.sumOf { expense -> expense.amount }
                )
            }
            .sortedByDescending { it.total }
    }
}
