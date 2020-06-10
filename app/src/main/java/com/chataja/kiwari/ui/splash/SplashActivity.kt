package com.chataja.kiwari.ui.splash

import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.chataja.kiwari.R
import com.chataja.kiwari.models.User
import com.chataja.kiwari.ui.chatroom.ChatRoomActivity
import com.chataja.kiwari.ui.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.jetbrains.anko.startActivity

class SplashActivity: AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler().postDelayed({
            if (auth.currentUser != null) {
                fetchUsers()
            } else {
                startActivity<LoginActivity>()
                finish()
            }
        }, 500)
    }

    private fun fetchUsers() {
        val ref = FirebaseDatabase.getInstance().getReference("/users")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(databaseError: DatabaseError) {
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dataSnapshot.children.forEach {
                    @Suppress("NestedLambdaShadowedImplicitParameter")
                    it.getValue(User::class.java)?.let {
                        if (it.uid != FirebaseAuth.getInstance().uid) {
                            startActivity<ChatRoomActivity>("USER_KEY" to it)
                            finish()
                        }
                    }
                }
            }
        })
    }
}