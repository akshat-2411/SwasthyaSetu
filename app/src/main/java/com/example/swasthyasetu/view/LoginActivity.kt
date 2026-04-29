package com.example.swasthyasetu.view

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import com.example.swasthyasetu.R
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.example.swasthyasetu.databinding.ActivityLoginBinding
import com.example.swasthyasetu.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : BaseActivity() {

    override val requireAuthentication: Boolean = false

    private lateinit var binding: ActivityLoginBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private val authViewModel: AuthViewModel by viewModels()

    companion object {
        private const val TAG = "LoginActivity"
    }

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            binding.root.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

            val isCancelled = e.statusCode == com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes.SIGN_IN_CANCELLED
            val message = if (isCancelled) {
                getString(R.string.login_error_cancelled)
            } else {
                getString(R.string.login_failed, e.localizedMessage ?: "Unknown error")
            }

            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.login_error_title))
                .setMessage(message)
                .setPositiveButton(getString(R.string.btn_ok), null)
                .show()

            setLoadingState(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.requestFeature(android.view.Window.FEATURE_ACTIVITY_TRANSITIONS)
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initGoogleSignInClient()
        setupClickListeners()
        observeAuthState()
        runEntranceAnimation()
    }

    private fun runEntranceAnimation() {
        val views = listOf(
            binding.ivLoginLogo,
            binding.tvLoginWelcome,
            binding.tvLoginSubtitle,
            binding.ivIllustration,
            binding.btnGoogleSignIn
        )

        for (v in views) {
            v.alpha = 0f
            v.translationY = 50f
        }

        val animators = views.mapIndexed { index, view ->
            val fade = ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f)
            val slide = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 50f, 0f)
            AnimatorSet().apply {
                playTogether(fade, slide)
                startDelay = (index * 50).toLong()
                duration = 400
                interpolator = DecelerateInterpolator()
            }
        }

        AnimatorSet().apply {
            playTogether(animators)
            start()
        }
    }

    private fun initGoogleSignInClient() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setupClickListeners() {
        binding.btnGoogleSignIn.setOnClickListener { signIn() }
    }

    private fun signIn() {
        setLoadingState(true)
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        authViewModel.signInWithGoogleCredential(credential)
    }

    private fun observeAuthState() {
        authViewModel.authState.observe(this) { state ->
            when (state) {
                is AuthViewModel.AuthState.Loading -> setLoadingState(true)
                is AuthViewModel.AuthState.Success -> {
                    setLoadingState(false)
                    val displayName = state.user.displayName ?: "User"
                    Toast.makeText(this, getString(R.string.login_welcome_user, displayName), Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                is AuthViewModel.AuthState.Error -> {
                    setLoadingState(false)
                    binding.root.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.login_error_title))
                        .setMessage(getString(R.string.login_failed, state.message))
                        .setPositiveButton(getString(R.string.btn_ok), null)
                        .show()
                }
                is AuthViewModel.AuthState.Idle -> setLoadingState(false)
                else -> { /* no-op */ }
            }
        }
    }

    private fun setLoadingState(loading: Boolean) {
        binding.progressLogin.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnGoogleSignIn.isEnabled = !loading
        binding.btnGoogleSignIn.alpha = if (loading) 0.6f else 1.0f
    }
}
