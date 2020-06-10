package com.chataja.kiwari.ui.login

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.chataja.kiwari.R
import com.chataja.kiwari.models.User
import com.chataja.kiwari.ui.chatroom.ChatRoomActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_login.*
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.startActivity

class LoginActivity : AppCompatActivity() {

    private val TAG = LoginActivity::class.java.simpleName

    private val firebaseAuth = FirebaseAuth.getInstance()

    private val firebaseDatabase = FirebaseDatabase.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        btnSignIn.setOnClickListener {
            performLogin()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left)
    }

    private fun performLogin() {
        val email = loginEmail.text.toString()
        val password = loginPass.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            rootView.snackbar(getString(R.string.login_email_password_empty)).show()
            return
        }

        loading.visibility = View.VISIBLE

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (!it.isSuccessful) return@addOnCompleteListener
                Log.d(TAG, "Successfully logged in: ${it.result!!.user.uid}")

                fetchUsers()
            }
            .addOnFailureListener {
                rootView.snackbar(it.message.toString()).show()

                loading.visibility = View.GONE
            }
    }

    private fun fetchUsers() {
        val ref = firebaseDatabase.getReference("/users")
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