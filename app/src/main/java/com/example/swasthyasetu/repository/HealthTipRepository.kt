package com.example.swasthyasetu.repository

import com.example.swasthyasetu.model.HealthTip
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.jvm.java

class HealthTipRepository {
    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        private const val COLLECTION_DAILY_TIPS = "daily_tips"
    }

    suspend fun fetchLatestTip(): HealthTip? {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        return try {
            // 1. Try exact match for today's date
            val todaySnapshot = firestore
                .collection(COLLECTION_DAILY_TIPS)
                .whereEqualTo("date", todayStr)
                .limit(1)
                .get()
                .await()

            if (!todaySnapshot.isEmpty) {
                return todaySnapshot.documents[0].toObject(HealthTip::class.java)
            }

            // 2. Fallback: get the most recent tip by date descending
            val latestSnapshot = firestore
                .collection(COLLECTION_DAILY_TIPS)
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            if (!latestSnapshot.isEmpty) {
                latestSnapshot.documents[0].toObject(HealthTip::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            throw e
        }
    }
}
