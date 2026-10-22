package com.example.receiptai

import android.app.Application
import android.content.pm.ApplicationInfo
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.initialize

class ReceiptApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Firebase.initialize(this)
        val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        Firebase.appCheck.installAppCheckProviderFactory(
            if (isDebuggable) DebugAppCheckProviderFactory.getInstance()
            else PlayIntegrityAppCheckProviderFactory.getInstance()
        )
    }
}