package com.safewalk.safewalk

import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
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
        Log.d(TAG, "message received: ${p0!!.data!!.entries}")
        Log.d(TAG, "message received: ${p0!!.notification!!.body}")
        localBroadcastManager.sendBroadcast(
                Intent(helpMessageIntentString)
                        .putExtra("notificationText", p0.notification!!.body)
                        .putExtra("uid_from", p0.data.getValue("uid_from"))
                        .putExtra("name", p0.data.getValue("name"))
                        .putExtra("where", p0.data.getValue("where")))
    }
}