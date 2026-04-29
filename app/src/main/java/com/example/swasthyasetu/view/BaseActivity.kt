package com.example.swasthyasetu.view

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.example.swasthyasetu.util.LocaleHelper

open class BaseActivity : AppCompatActivity() {

    open val requireAuthentication: Boolean = true

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onStart() {
        super.onStart()
        if (requireAuthentication && com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) {
            val intent = android.content.Intent(this, LoginActivity::class.java)
            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
