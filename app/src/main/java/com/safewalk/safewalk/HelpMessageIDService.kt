package com.safewalk.safewalk

import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService
import timber.log.Timber

class HelpMessageIDService : FirebaseInstanceIdService() {
    override fun onTokenRefresh() {
        super.onTokenRefresh()
        Timber.d("[HelpMessageIDService]: New token: ${FirebaseInstanceId.getInstance().token}")
    }
}