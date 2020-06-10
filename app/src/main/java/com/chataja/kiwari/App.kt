package com.chataja.kiwari

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import com.google.firebase.database.FirebaseDatabase
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.google.GoogleEmojiProvider

class App : Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        EmojiManager.install(GoogleEmojiProvider())
        val database = FirebaseDatabase.getInstance()
        database.setPersistenceEnabled(true)
    }
}