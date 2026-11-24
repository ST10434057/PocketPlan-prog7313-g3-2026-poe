package com.example.prog7313poepart1

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.lifecycleScope
import com.example.prog7313poepart1.data.firebase.FirebaseRepository
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var edtEmail: TextInputEditText
    private lateinit var edtPassword: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var btnCreateAccount: Button
    private lateinit var btnGoogleSignIn: Button

    private lateinit var firebaseRepository: FirebaseRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseRepository = FirebaseRepository(this)
        sessionManager = SessionManager(this)

        if (firebaseRepository.isLoggedIn()) {
            val uid = firebaseRepository.getCurrentUserId()
            val email = firebaseRepository.getCurrentUserEmail()
            if (uid != null) sessionManager.saveFirebaseUser(uid, email)
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val mainView: View? = findViewById(R.id.main)
        mainView?.let { v ->
            ViewCompat.setOnApplyWindowInsetsListener(v) { view, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(
                    systemBars.left, systemBars.top,
                    systemBars.right, systemBars.bottom
                )
                insets
            }
        }

        edtEmail = findViewById(R.id.edtEmail)
        edtPassword = findViewById(R.id.edtPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnCreateAccount = findViewById(R.id.btnCreateAccount)
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)

        credentialManager = CredentialManager.create(this)

        btnCreateAccount.setOnClickListener { registerUser() }
        btnLogin.setOnClickListener { loginUser() }
        btnGoogleSignIn.setOnClickListener { startGoogleSignIn() }
    }

    // -------------------------------------------------------------------------
    // Google Sign-In
    // -------------------------------------------------------------------------

    private fun startGoogleSignIn() {
        val serverClientId = getString(R.string.default_web_client_id)
        setButtonsEnabled(false)

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(serverClientId).build())
            .build()

        lifecycleScope.launch {
            try {
                Log.d(TAG, "Requesting Google credential...")
                val result = credentialManager.getCredential(request = request, context = this@MainActivity)
                Log.d(TAG, "Credential received: ${result.credential::class.simpleName}")
                handleGoogleCredential(result.credential)
            } catch (e: GetCredentialCancellationException) {
                Log.d(TAG, "User cancelled Google sign-in")
                setButtonsEnabled(true)
            } catch (e: Exception) {
                Log.e(TAG, "Google sign-in exception", e)
                Toast.makeText(
                    this@MainActivity,
                    "Google sign-in failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                setButtonsEnabled(true)
            }
        }
    }

    private suspend fun handleGoogleCredential(credential: androidx.credentials.Credential) {
        val idToken: String? = when {
            credential is GoogleIdTokenCredential -> {
                Log.d(TAG, "Credential is GoogleIdTokenCredential")
                credential.idToken
            }
            credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                Log.d(TAG, "Credential is CustomCredential/GoogleIdToken")
                GoogleIdTokenCredential.createFrom(credential.data).idToken
            }
            else -> {
                Log.e(TAG, "Unknown credential type: ${credential::class.simpleName}")
                null
            }
        }

        if (idToken.isNullOrBlank()) {
            Log.e(TAG, "ID token is null or blank")
            throw IllegalStateException("Could not extract ID token from Google credential")
        }

        Log.d(TAG, "ID token extracted, signing in with Firebase...")
        val (uid, email) = firebaseRepository.signInWithGoogle(idToken)
        Log.d(TAG, "Firebase sign-in successful, uid=$uid")
        sessionManager.saveFirebaseUser(uid, email)
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }

    companion object {
        private const val TAG = "GoogleSignIn"
    }

    // -------------------------------------------------------------------------
    // Email / password
    // -------------------------------------------------------------------------

    private fun registerUser() {
        val email = edtEmail.text.toString().trim()
        val password = edtPassword.text.toString().trim()
        if (!validateInput(email, password)) return

        setButtonsEnabled(false)

        lifecycleScope.launch {
            try {
                val uid = firebaseRepository.registerUser(email, password)
                sessionManager.saveFirebaseUser(uid, email)
                Toast.makeText(this@MainActivity, "Account created successfully", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@MainActivity, DashboardActivity::class.java))
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Registration failed: ${e.message}", Toast.LENGTH_LONG).show()
                setButtonsEnabled(true)
            }
        }
    }

    private fun loginUser() {
        val email = edtEmail.text.toString().trim()
        val password = edtPassword.text.toString().trim()
        if (!validateInput(email, password)) return

        setButtonsEnabled(false)

        lifecycleScope.launch {
            try {
                val uid = firebaseRepository.loginUser(email, password)
                sessionManager.saveFirebaseUser(uid, email)
                Toast.makeText(this@MainActivity, "Login successful", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@MainActivity, DashboardActivity::class.java))
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
                setButtonsEnabled(true)
            }
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            edtEmail.error = "Enter your email"
            return false
        }
        if (!email.contains("@")) {
            edtEmail.error = "Enter a valid email"
            return false
        }
        if (password.isEmpty()) {
            edtPassword.error = "Enter your password"
            return false
        }
        if (password.length < 6) {
            edtPassword.error = "Password must be at least 6 characters"
            return false
        }
        return true
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        btnLogin.isEnabled = enabled
        btnCreateAccount.isEnabled = enabled
        btnGoogleSignIn.isEnabled = enabled
    }
}
