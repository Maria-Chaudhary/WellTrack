package com.example.fittrack

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth

@Suppress("DEPRECATION")
class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleFitHelper: GoogleFitHelper
    private val GOOGLE_SIGN_IN_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_FitTrack)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        googleFitHelper = GoogleFitHelper(this)

        val etEmail    = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin   = findViewById<Button>(R.id.btnLogin)
        val btnGoogle  = findViewById<Button>(R.id.btnGoogleSignIn)
        val tvSignup   = findViewById<TextView>(R.id.tvSignup)

        // Email/password login
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    checkGoogleFitPermissions()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Login failed: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        }

        // Google Fit permission (not Google Firebase login)
        btnGoogle.setOnClickListener {
            val scopes = GoogleFitHelper.fitnessOptions.impliedScopes.toTypedArray()
            if (scopes.isNotEmpty()) {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .requestScopes(scopes[0], *scopes.drop(1).toTypedArray())
                    .build()
                val client = GoogleSignIn.getClient(this, gso)
                startActivityForResult(client.signInIntent, GOOGLE_SIGN_IN_CODE)
            }
        }

        tvSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    private fun checkGoogleFitPermissions() {
        if (!googleFitHelper.hasPermissions()) {
            googleFitHelper.requestPermissions(this)
        } else {
            goToMain()
        }
    }

    private fun goToMain() {
        Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(this)
        }
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GOOGLE_SIGN_IN_CODE) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    checkGoogleFitPermissions()
                } else {
                    Toast.makeText(this, "Google sign-in failed (no account)", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign-in failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
