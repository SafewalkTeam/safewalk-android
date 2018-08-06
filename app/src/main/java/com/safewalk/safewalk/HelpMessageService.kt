package com.safewalk.safewalk

import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber

class HelpMessageService : FirebaseMessagingService() {

    private val TAG = "[HelpMessageService]"
    private val helpMessageIntentString = "HelpMessageString"
    private val messageValue = "message"
    private lateinit var localBroadcastManager: LocalBroadcastManager

    override fun onCreate() {
        super.onCreate()
        localBroadcastManager = LocalBroadcastManager.getInstance(this)
    }

    override fun onMessageReceived(p0: RemoteMessage?) {
        super.onMessageReceived(p0)
        Timber.d("$TAG message received: ${p0?.data}")
        localBroadcastManager.sendBroadcast(
                Intent(helpMessageIntentString)
                        .putExtra(messageValue, p0!!.data.getValue(messageValue)))
    }
}