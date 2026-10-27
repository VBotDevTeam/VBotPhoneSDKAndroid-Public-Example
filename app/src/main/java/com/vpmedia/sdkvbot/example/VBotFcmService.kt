package com.vpmedia.sdkvbot.example

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.vpmedia.sdkvbot.client.VBotClient
import com.vpmedia.sdkvbot.client.VBotConfig
import com.vpmedia.sdkvbot.client.VBotEnvironment

class VBotFcmService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "VBotExample"
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "FCM onNewToken: $token")
        savePushToken(this, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val map = HashMap(message.data)
        Log.d(TAG, "FCM onMessageReceived: $map")
        VBotClient(applicationContext).apply {
            if (!isSetup()) setup(VBotConfig(VBotEnvironment.STAGING))
            notificationCall(map)
        }
    }
}
