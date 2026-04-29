package com.example.swasthyasetu.repository

import com.example.swasthyasetu.model.TimelineEvent
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class HistoryRepository {
    private val db = FirebaseFirestore.getInstance()

    fun getUnifiedHistory(userId: String): Task<List<TimelineEvent>> {
        val chatsTask = db.collection("chats").whereEqualTo("userId", userId).get()
        val vaccinationsTask = db.collection("vaccinations").whereEqualTo("userId", userId).get()
        val symptomsTask = db.collection("symptom_results").whereEqualTo("userId", userId).get()

        return Tasks.whenAllSuccess<QuerySnapshot>(chatsTask, vaccinationsTask, symptomsTask)
            .continueWith { task ->
                val resultList = mutableListOf<TimelineEvent>()
                if (task.isSuccessful) {
                    val snapshots = task.result
                    if (snapshots != null && snapshots.size == 3) {
                        try {
                            val chatDocs = snapshots[0].documents
                            for (doc in chatDocs) {
                                resultList.add(
                                    TimelineEvent(
                                        id = doc.id,
                                        title = "Chat with AI",
                                        date = doc.getLong("timestamp")
                                            ?: System.currentTimeMillis(),
                                        type = "CHAT",
                                        description = "You: ${doc.getString("prompt") ?: "Query"}\nAI: ${
                                            doc.getString(
                                                "response"
                                            ) ?: ""
                                        }"
                                    )
                                )
                            }

                            val vaccineDocs = snapshots[1].documents
                            for (doc in vaccineDocs) {
                                val isCompleted = doc.getBoolean("isCompleted") ?: false
                                resultList.add(
                                    TimelineEvent(
                                        id = doc.id,
                                        title = "Vaccination: ${doc.getString("name") ?: "Vaccine"}",
                                        date = doc.getLong("dueDate") ?: System.currentTimeMillis(),
                                        type = "VACCINATION",
                                        description = "Status: ${if (isCompleted) "Completed" else "Upcoming"}"
                                    )
                                )
                            }

                            val symptomDocs = snapshots[2].documents
                            for (doc in symptomDocs) {
                                resultList.add(
                                    TimelineEvent(
                                        id = doc.id,
                                        title = "Symptom Checker",
                                        date = doc.getLong("timestamp")
                                            ?: System.currentTimeMillis(),
                                        type = "SYMPTOM",
                                        description = "Reported: ${doc.getString("symptoms") ?: "Symptoms Check"}\nResult: ${
                                            doc.getString(
                                                "summary"
                                            ) ?: ""
                                        }"
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("HistoryRepository", "Error unpacking documents: ${e.message}")
                        }
                    }
                }
                resultList
            }
    }
}