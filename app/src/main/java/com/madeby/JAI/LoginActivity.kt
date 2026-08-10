package com.madeby.JAI

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class LoginActivity : AppCompatActivity() {

    private lateinit var btnGoogleSignIn: MaterialButton
    private lateinit var btnGuest: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
        btnGuest = findViewById(R.id.btnGuest)

        btnGoogleSignIn.setOnClickListener {
            performGoogleSignIn()
        }

        btnGuest.setOnClickListener {
            AuthManager.setGuestMode(this)
            proceedToMain()
        }

        // Handle OAuth Deep-Link return if applicable
        intent?.data?.let { handleDeepLink(it) }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.data?.let { handleDeepLink(it) }
    }

    private fun performGoogleSignIn() {
        val googleClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (googleClientId.isBlank()) {
            Toast.makeText(this, "Google Web Client ID missing in config.", Toast.LENGTH_LONG).show()
            return
        }

        val credentialManager = CredentialManager.create(this)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(googleClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = this@LoginActivity
                )
                val credential = result.credential
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val displayName = googleIdTokenCredential.displayName
                val id = googleIdTokenCredential.id

                // Exchange Google ID Token with Supabase Auth API
                exchangeTokenWithSupabase(idToken, displayName ?: id)

            } catch (e: GetCredentialException) {
                Log.e("LoginActivity", "Credential Manager failed", e)
                // Fallback to browser OAuth flow if Credential Manager fails or user cancels
                launchBrowserOAuth()
            } catch (e: Exception) {
                Log.e("LoginActivity", "Google Sign-In failed", e)
                Toast.makeText(this@LoginActivity, "Sign-In cancelled or failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun launchBrowserOAuth() {
        val supabaseUrl = BuildConfig.SUPABASE_URL
        if (supabaseUrl.isBlank()) {
            Toast.makeText(this, "Supabase URL is missing.", Toast.LENGTH_LONG).show()
            return
        }
        val redirectUri = "studytimer://login-callback"
        val authUrl = "$supabaseUrl/auth/v1/authorize?provider=google&redirect_to=$redirectUri"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
        startActivity(intent)
    }

    private fun exchangeTokenWithSupabase(idToken: String, displayName: String) {
        val supabaseUrl = BuildConfig.SUPABASE_URL
        val anonKey = BuildConfig.SUPABASE_ANON_KEY

        if (supabaseUrl.isBlank() || anonKey.isBlank()) {
            // If Supabase API config is missing, save Google identity directly locally
            AuthManager.saveUserSession(this, displayName, displayName, idToken)
            proceedToMain()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("$supabaseUrl/auth/v1/token?grant_type=id_token")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("apikey", anonKey)
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val payload = JSONObject().apply {
                    put("provider", "google")
                    put("id_token", idToken)
                }

                conn.outputStream.use { os ->
                    os.write(payload.toString().toByteArray(Charsets.UTF_8))
                }

                val responseCode = conn.responseCode
                if (responseCode in 200..299) {
                    val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(responseStr)
                    val accessToken = json.optString("access_token")
                    val userObj = json.optJSONObject("user")
                    val email = userObj?.optString("email") ?: displayName
                    val userId = userObj?.optString("id") ?: email

                    withContext(Dispatchers.Main) {
                        AuthManager.saveUserSession(this@LoginActivity, email, displayName, accessToken, userId)
                        Toast.makeText(this@LoginActivity, "Welcome, $displayName!", Toast.LENGTH_SHORT).show()
                    }

                    // Restore user's cloud data if present
                    CloudSyncManager.restoreDataFromCloud(this@LoginActivity)

                    withContext(Dispatchers.Main) {
                        proceedToMain()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        // Save local session on API error fallback
                        AuthManager.saveUserSession(this@LoginActivity, displayName, displayName, idToken)
                        proceedToMain()
                    }
                }
            } catch (e: Exception) {
                Log.e("LoginActivity", "Supabase token exchange error", e)
                withContext(Dispatchers.Main) {
                    AuthManager.saveUserSession(this@LoginActivity, displayName, displayName, idToken)
                    proceedToMain()
                }
            }
        }
    }

    private fun handleDeepLink(uri: Uri) {
        if (uri.scheme == "studytimer" && uri.host == "login-callback") {
            val fragment = uri.fragment ?: uri.query ?: ""
            var accessToken: String? = null
            if (fragment.contains("access_token=")) {
                val params = fragment.split("&")
                for (p in params) {
                    if (p.startsWith("access_token=")) accessToken = p.substringAfter("access_token=")
                }
            }
            AuthManager.saveUserSession(this, "Google User", "Study User", accessToken)
            Toast.makeText(this, "Signed in successfully!", Toast.LENGTH_SHORT).show()
            proceedToMain()
        }
    }

    private fun proceedToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
