package com.example.fitme.signup

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.fitme.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.example.fitme.R
import com.example.fitme.ViewModelFactory
import com.example.fitme.api.ResultState
import com.example.fitme.login.LoginActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth
    private val viewModel by viewModels<SignUpViewModel> {
        ViewModelFactory.getInstance(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        setupView()
        setupBinding()
        setupAction()
    }

    private fun setupBinding() {
        binding.signUpToLogin.setOnClickListener {
            val intent = Intent(this@SignUpActivity, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupView() {
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
        supportActionBar?.hide()
    }

    private fun setupAction() {
        binding.signupButton.setOnClickListener {
            signUp()
        }

        binding.googleSignupButton.setOnClickListener {
            signUpGoogle()
        }
    }

    private fun signUp() {
        val name = binding.nameInput.text.toString().trim()
        val email = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString().trim()

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Fullname, email, and password must be filled", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    handleRegister()
                } else {
                    Toast.makeText(this, "${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun handleRegister() {
        val userID = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val email = FirebaseAuth.getInstance().currentUser?.email ?: ""
        val fullName = binding.nameInput.text.toString().trim()

        viewModel.signUp(userID,email,fullName).observe(this){ result ->
            when (result) {
                is ResultState.Success -> {
                    binding.progressBar.visibility = View.INVISIBLE
                    if (result.data.status.equals("success")) {
                        Toast.makeText(this,"Sign Up Successful!", Toast.LENGTH_SHORT).show()
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                        updateUI()
                    } else {
                        Toast.makeText(this, result.data.message, Toast.LENGTH_SHORT).show()
                    }
                    auth.signOut()
                }

                is ResultState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }

                is ResultState.Error -> {
                    binding.progressBar.visibility = View.INVISIBLE
                    Toast.makeText(this, result.error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun handleRegisterGoogle() {
        val userID = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val email = FirebaseAuth.getInstance().currentUser?.email ?: ""
        val fullName = FirebaseAuth.getInstance().currentUser?.displayName ?: ""

        viewModel.signUp(userID,email,fullName).observe(this){ result ->
            when (result) {
                is ResultState.Success -> {
                    binding.progressBar.visibility = View.INVISIBLE
                    if (result.data.message == "success") {
                        Toast.makeText(this, "Sign Up Successful!", Toast.LENGTH_SHORT).show()
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                        updateUI()
                    } else {
                        Toast.makeText(this, result.data.message, Toast.LENGTH_SHORT).show()
                    }
                    val googleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.Builder(
                        GoogleSignInOptions.DEFAULT_SIGN_IN).build())
                    googleSignInClient.signOut()
                }

                is ResultState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }

                is ResultState.Error -> {
                    binding.progressBar.visibility = View.INVISIBLE
                    Toast.makeText(this, result.error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun signUpGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.your_web_client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w(TAG, "Google sign in failed", e)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String?) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")
                    handleRegisterGoogle()
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                }
            }
    }

    private fun updateUI() {
        startActivity(Intent(this@SignUpActivity, LoginActivity::class.java))
        finish()
    }

    companion object {
        private const val TAG = "SignUpActivity"
        private const val RC_SIGN_IN = 9001
    }
}