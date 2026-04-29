package com.example.swasthyasetu.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.swasthyasetu.R
import androidx.core.app.NotificationCompat
import com.example.swasthyasetu.view.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.jvm.java

class ReminderBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_MEDICINE_NAME = "extra_medicine_name"
        private const val CHANNEL_ID = "MEDICINE_REMINDERS_CHANNEL"
        const val ACTION_TAKE_MEDICINE = "com.swasthyasetu.app.ACTION_TAKE_MEDICINE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val medicineName = intent.getStringExtra(EXTRA_MEDICINE_NAME) ?: "Your Medicine"

        if (intent.action == ACTION_TAKE_MEDICINE) {
            handleMedicationAdherence(context, intent, medicineName)
            return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_reminders_name),
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val actionIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_TAKE_MEDICINE
            putExtra(EXTRA_MEDICINE_NAME, medicineName)
        }

        val uniqueRequestId = (System.currentTimeMillis() % 50000).toInt()
        val actionPendingIntent = PendingIntent.getBroadcast(
            context, uniqueRequestId, actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context, uniqueRequestId + 1, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("It's time for $medicineName")
            .setContentText("Please take your scheduled dosage now.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(0, context.getString(R.string.notif_action_take_now), actionPendingIntent)

        val uniqueNotificationId = (System.currentTimeMillis() % 100000).toInt()
        actionIntent.putExtra("notification_id", uniqueNotificationId)
        notificationManager.notify(uniqueNotificationId, builder.build())
    }

    private fun handleMedicationAdherence(context: Context, intent: Intent, medicineName: String) {
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid

        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            val timestamp = System.currentTimeMillis()
            val timeString = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))

            val record = hashMapOf(
                "userId" to userId,
                "type" to "MEDICINE",
                "medicineName" to medicineName,
                "description" to "Took $medicineName at $timeString",
                "timestamp" to timestamp
            )

            db.collection("history").add(record)
                .addOnSuccessListener {
                    val notificationId = intent.getIntExtra("notification_id", -1)
                    if (notificationId != -1) {
                        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancel(notificationId)
                    }
                }
        }
    }
}