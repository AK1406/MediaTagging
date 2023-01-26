package com.example.mediatagging.ui.login

import android.animation.ValueAnimator
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.mediatagging.R
import com.example.mediatagging.databinding.ActivitySignInBinding
import com.example.mediatagging.ui.home.DashboardActivity
import com.example.mediatagging.utils.StorageManagerKeys
import com.example.mediatagging.utils.StorageManagerUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DatabaseReference

class SignInActivity : AppCompatActivity() {
    lateinit var binding: ActivitySignInBinding
    private var auth: FirebaseAuth? = null
    lateinit var gso: GoogleSignInOptions
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var myRef: DatabaseReference
    val TAG = "SignInActivity"
    private val RC_SIGN_IN = 9001
    private var user: FirebaseUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
    }

    fun initView() {
        googleSignIn()
    }

    override fun onResume() {
        super.onResume()
        binding.laUploading.repeatCount = ValueAnimator.INFINITE
        binding.laUploading.playAnimation()
    }

    override fun onPause() {
        super.onPause()
        binding.laUploading.cancelAnimation()
    }

    fun googleSignIn() {
        /**Google sign in **/
        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) //web client id containing api
            .requestEmail()
            .build()
        this.mGoogleSignInClient =
            GoogleSignIn.getClient(this, gso) // passing google sign in option to client
        binding.googleSignIn.setOnClickListener {
            signIn()
        }

        //   FirebaseApp.initializeApp(this) //initializing firebase app
        /**getting instance of firebase auth so that we can compare email and password to login**/
        auth = FirebaseAuth.getInstance()
    }


    private fun signIn() {
        val signInIntent: Intent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account: GoogleSignInAccount? = task.getResult(ApiException::class.java)
                if (account != null) {
                    user = FirebaseAuth.getInstance().currentUser
                    val intent = Intent(this, DashboardActivity::class.java)
                    intent.putExtra("user", user?.photoUrl.toString())
                    intent.putExtra("email", user?.email.toString())
                    intent.putExtra("name", user?.displayName.toString())
                    Log.i(TAG, "profileUrl: ${user?.photoUrl.toString()}")
                    startActivity(intent)
                    Toast.makeText(this, "Google Sign in Succeeded", Toast.LENGTH_LONG).show()
                    firebaseAuthWithGoogle(account!!)
                }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google Sign in Failed $e", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun saveUserInfo() {
        user = FirebaseAuth.getInstance().currentUser
        StorageManagerUtil.getInstance(this).savePreference(
            StorageManagerKeys.USER_PROFILE.name, user?.photoUrl.toString()
        )
        StorageManagerUtil.getInstance(this).savePreference(
            StorageManagerKeys.USER_EMAIL.name, user?.email.toString()
        )
        StorageManagerUtil.getInstance(this).savePreference(
            StorageManagerKeys.USER_NAME.name, user?.displayName.toString()
        )
        StorageManagerUtil.getInstance(this).savePreference(
            StorageManagerKeys.USER_UID.name, user?.uid
        )
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {

        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        auth!!.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth!!.currentUser
                    Toast.makeText(
                        this,
                        "Authentication successful :" + user!!.email,
                        Toast.LENGTH_LONG
                    ).show()

                } else {
                    Toast.makeText(
                        this,
                        "Authentication failed:" + task.exception!!,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }


}