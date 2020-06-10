package com.chataja.kiwari.ui.chatroom

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.chataja.kiwari.R
import com.chataja.kiwari.models.ChatMessage
import com.chataja.kiwari.models.User
import com.chataja.kiwari.ui.splash.SplashActivity
import com.chataja.kiwari.utils.DateUtils
import com.chataja.kiwari.utils.EditTextListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import com.vanniktech.emoji.EmojiPopup
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_chat_room.*
import kotlinx.android.synthetic.main.chat_from_row.view.*
import kotlinx.android.synthetic.main.chat_to_row.view.*
import org.jetbrains.anko.design.snackbar

class ChatRoomActivity: AppCompatActivity() {

    private lateinit var emojiPopup: EmojiPopup

    private lateinit var currentUser: User

    private val adapter = GroupAdapter<ViewHolder>()

    private val TAG = ChatRoomActivity::class.java.simpleName

    private val firebaseInstance = FirebaseDatabase.getInstance()

    // Bundle Data
    private val toUser: User
        get() = intent.getParcelableExtra("USER_KEY")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_room)

        setUpToolbar()

        fetchCurrentUser()

        setUpEmojiPopup()

        listenForMessages()

        recyclerview_chat_log.adapter = adapter

        btnSend.setOnClickListener { sendMessage() }

        btnEmoji.setOnClickListener { emojiPopup.toggle() }
    }

    private fun setUpToolbar(){
        mainToolbar.title = toUser.name

        Picasso.get().load(toUser.profileImageUrl)
            .resize(80, 80)
            .placeholder(R.drawable.no_image2)
            .into(object: Target{
                override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                }

                override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                }

                override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                    val circularBitmapDrawable = RoundedBitmapDrawableFactory.create(resources, bitmap)
                    circularBitmapDrawable.isCircular = true
                    mainToolbar.logo = circularBitmapDrawable
                }
            })

        setSupportActionBar(mainToolbar)
    }

    private fun fetchCurrentUser() {
        val uid = FirebaseAuth.getInstance().uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(databaseError: DatabaseError) {
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                currentUser = dataSnapshot.getValue(User::class.java)!!
            }
        })
    }

    private fun setUpEmojiPopup() {
        emojiPopup = EmojiPopup.Builder.fromRootView(chat_layout)
                .setOnEmojiPopupShownListener {
                    btnEmoji.setImageResource(R.drawable.ic_outline_keyboard_24)
                }
                .setOnEmojiPopupDismissListener {
                    btnEmoji.setImageResource(R.drawable.ic_outline_insert_emoticon_24)
                }
                .build(etMessage)
    }

    private fun listenForMessages() {
        val fromId = FirebaseAuth.getInstance().uid ?: return
        val toId = toUser.uid
        val ref = FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId")

        etMessage.addTextChangedListener(EditTextListener(btnSend))

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(databaseError: DatabaseError) {
                Log.d(TAG, "database error: " + databaseError.message)
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                Log.d(TAG, "has children: " + dataSnapshot.hasChildren())
            }
        })

        ref.addChildEventListener(object : ChildEventListener {
            override fun onCancelled(databaseError: DatabaseError) {
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                dataSnapshot.getValue(ChatMessage::class.java)?.let {
                    if (it.fromId == FirebaseAuth.getInstance().uid) {
                        adapter.add(ChatFromItem(it.text, currentUser, it.timestamp))
                    } else {
                        adapter.add(ChatToItem(it.text, toUser, it.timestamp))
                    }
                }
                recyclerview_chat_log.scrollToPosition(adapter.itemCount - 1)
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
            }
        })
    }

    private fun sendMessage() {
        val text = etMessage.text.toString()
        if (text.isEmpty()) {
            chat_layout.snackbar(getString(R.string.message_cannot_empty)).show()
            return
        }

        val fromId = FirebaseAuth.getInstance().uid ?: return
        val toId = toUser.uid

        val reference = firebaseInstance.getReference("/user-messages/$fromId/$toId").push()
        val toReference = firebaseInstance.getReference("/user-messages/$toId/$fromId").push()

        val chatMessage = ChatMessage(
            reference.key!!,
            text,
            fromId,
            toId,
            System.currentTimeMillis() / 1000
        )

        reference.setValue(chatMessage).addOnSuccessListener {
            Log.d(TAG, "Saved our chat message: ${reference.key}")
            etMessage.text?.clear()
            recyclerview_chat_log.smoothScrollToPosition(adapter.itemCount - 1)
        }

        toReference.setValue(chatMessage)

        val latestMessageRef = firebaseInstance.getReference("/latest-messages/$fromId/$toId")
        latestMessageRef.setValue(chatMessage)

        val latestMessageToRef = firebaseInstance.getReference("/latest-messages/$toId/$fromId")
        latestMessageToRef.setValue(chatMessage)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_chat, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId ?: 0 == R.id.menu_logout) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.dialog_signout_title))
            builder.setMessage(getString(R.string.dialog_signout_body))
            builder.setPositiveButton(getString(R.string.yes)) { _, _ ->
                val auth = FirebaseAuth.getInstance()
                auth.signOut()

                startActivity(Intent(this, SplashActivity::class.java))
                finish()
            }
            builder.setNegativeButton(getString(R.string.no), null)
            builder.create().show()
        }
        return super.onOptionsItemSelected(item)
    }
}

class ChatFromItem(private val text: String, val user: User, private val timestamp: Long) : Item<ViewHolder>() {

    override fun bind(viewHolder: ViewHolder, position: Int) {

        viewHolder.itemView.textview_to_row.text = text
        viewHolder.itemView.to_msg_time.text = DateUtils.getFormattedTimeChatLog(timestamp)
    }

    override fun getLayout(): Int {
        return R.layout.chat_from_row
    }
}

class ChatToItem(private val text: String, val user: User, private val timestamp: Long) : Item<ViewHolder>() {

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.textview_from_row.text = text
        viewHolder.itemView.from_msg_time.text = DateUtils.getFormattedTimeChatLog(timestamp)
    }

    override fun getLayout(): Int {
        return R.layout.chat_to_row
    }
}