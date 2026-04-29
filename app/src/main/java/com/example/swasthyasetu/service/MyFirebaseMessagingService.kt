package com.example.swasthyasetu.service

import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MyFCMService"
        const val ACTION_EMERGENCY_ALERT = "com.swasthyasetu.app.ACTION_EMERGENCY_ALERT"
        const val EXTRA_ALERT_MESSAGE = "alert_message"
        const val EXTRA_ALERT_TYPE = "alert_type"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM token: $token")
        // If we needed to register the token with our backend, it goes here.
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Message received from: ${remoteMessage.from}")

        // Check if the message contains a data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")

            val locationTag = remoteMessage.data["location_tag"]
            val alertType = remoteMessage.data["alert_type"] ?: "warning"
            val messageBody = remoteMessage.data["message"]

            if (messageBody != null) {
                // Fire a LocalBroadcast to notify the active MainActivity
                val intent = Intent(ACTION_EMERGENCY_ALERT).apply {
                    putExtra(EXTRA_ALERT_MESSAGE, messageBody)
                    putExtra(EXTRA_ALERT_TYPE, alertType)
                }

                // If MainActivity is in the foreground, this will instantly trigger the alert banner
                val broadcastSent = LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
                Log.d(TAG, "Local broadcast sent: $broadcastSent")

                // Note: To show a standard System Notification linking back to MainActivity
                // when the app is in the background, we would build a NotificationManager
                // NotificationCompat here. FCM automatically handles "notification" payloads
                // but for "data" payloads we have to build it manually if the app is backgrounded.
            }
        }
    }
}
