package com.madeby.JAI

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.widget.TextView
import androidx.browser.customtabs.CustomTabsIntent
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

        val tvLegalNotice = findViewById<TextView>(R.id.tvLegalNotice)
        setupLegalNotice(tvLegalNotice)

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
                val displayName = googleIdTokenCredential.displayName ?: googleIdTokenCredential.id
                val googleEmail = googleIdTokenCredential.id

                // Exchange Google ID Token with Supabase Auth API
                exchangeTokenWithSupabase(idToken, displayName, googleEmail)

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

    private fun exchangeTokenWithSupabase(idToken: String, displayName: String, googleEmail: String) {
        val supabaseUrl = BuildConfig.SUPABASE_URL
        val anonKey = BuildConfig.SUPABASE_ANON_KEY

        if (supabaseUrl.isBlank() || anonKey.isBlank()) {
            // If Supabase API config is missing, save Google identity directly locally
            AuthManager.saveUserSession(this, googleEmail, displayName, idToken, googleEmail)
            lifecycleScope.launch(Dispatchers.IO) {
                CloudSyncManager.syncDataToCloud(this@LoginActivity)
                withContext(Dispatchers.Main) {
                    proceedToMain()
                }
            }
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
                    val metaObj = userObj?.optJSONObject("user_metadata") ?: userObj?.optJSONObject("raw_user_meta_data")
                    val fetchedName = metaObj?.optString("full_name")?.takeIf { it.isNotBlank() }
                        ?: metaObj?.optString("name")?.takeIf { it.isNotBlank() }
                        ?: displayName
                    val email = userObj?.optString("email")?.takeIf { it.isNotBlank() } ?: googleEmail
                    val userId = userObj?.optString("id")?.takeIf { it.isNotBlank() } ?: email

                    withContext(Dispatchers.Main) {
                        AuthManager.saveUserSession(this@LoginActivity, email, fetchedName, accessToken, userId)
                        Toast.makeText(this@LoginActivity, "Welcome, $fetchedName!", Toast.LENGTH_SHORT).show()
                    }

                    // Safely check remote metadata first before blindly overwriting cloud data
                    val (remoteMeta, _) = CloudSyncManager.fetchRemoteMetadata(this@LoginActivity)
                    if (remoteMeta != null && remoteMeta.updatedAt > 0L) {
                        CloudSyncManager.restoreDataFromCloud(this@LoginActivity)
                    } else {
                        CloudSyncManager.syncDataToCloud(this@LoginActivity, force = true)
                    }

                    withContext(Dispatchers.Main) {
                        proceedToMain()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        // Save local session on API fallback
                        AuthManager.saveUserSession(this@LoginActivity, googleEmail, displayName, idToken, googleEmail)
                        Toast.makeText(this@LoginActivity, "Welcome, $displayName!", Toast.LENGTH_SHORT).show()
                    }
                    val (remoteMeta, _) = CloudSyncManager.fetchRemoteMetadata(this@LoginActivity)
                    if (remoteMeta != null && remoteMeta.updatedAt > 0L) {
                        CloudSyncManager.restoreDataFromCloud(this@LoginActivity)
                    } else {
                        CloudSyncManager.syncDataToCloud(this@LoginActivity, force = true)
                    }
                    withContext(Dispatchers.Main) {
                        proceedToMain()
                    }
                }
            } catch (e: Exception) {
                Log.e("LoginActivity", "Supabase token exchange error", e)
                withContext(Dispatchers.Main) {
                    AuthManager.saveUserSession(this@LoginActivity, googleEmail, displayName, idToken, googleEmail)
                    Toast.makeText(this@LoginActivity, "Welcome, $displayName!", Toast.LENGTH_SHORT).show()
                }
                CloudSyncManager.syncDataToCloud(this@LoginActivity)
                withContext(Dispatchers.Main) {
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
            lifecycleScope.launch(Dispatchers.IO) {
                CloudSyncManager.restoreDataFromCloud(this@LoginActivity)
                withContext(Dispatchers.Main) {
                    proceedToMain()
                }
            }
        }
    }

    private fun setupLegalNotice(textView: TextView) {
        val fullText = "By continuing, you agree to our Terms of Service and Privacy Policy."
        val spannable = SpannableStringBuilder(fullText)

        val termsText = "Terms of Service"
        val termsStart = fullText.indexOf(termsText)
        if (termsStart != -1) {
            val termsEnd = termsStart + termsText.length
            spannable.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    openWebUrl("https://get-studytimer.vercel.app/terms.html")
                }
                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = Color.parseColor("#818CF8")
                    ds.isUnderlineText = true
                }
            }, termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        val privacyText = "Privacy Policy"
        val privacyStart = fullText.indexOf(privacyText)
        if (privacyStart != -1) {
            val privacyEnd = privacyStart + privacyText.length
            spannable.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    openWebUrl("https://get-studytimer.vercel.app/privacy.html")
                }
                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = Color.parseColor("#818CF8")
                    ds.isUnderlineText = true
                }
            }, privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        textView.text = spannable
        textView.movementMethod = LinkMovementMethod.getInstance()
        textView.highlightColor = Color.TRANSPARENT
    }

    private fun openWebUrl(url: String) {
        try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            customTabsIntent.launchUrl(this, Uri.parse(url))
        } catch (_: Exception) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (_: Exception) {
                Toast.makeText(this, "Could not open browser", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun proceedToMain() {
        AuthManager.setOnboardingCompleted(this)
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
