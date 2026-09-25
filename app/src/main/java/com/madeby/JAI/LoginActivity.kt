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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        setContentView(R.layout.activity_login)

        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
        btnGuest = findViewById(R.id.btnGuest)

        attachPressScale(btnGoogleSignIn, 0.97f)
        attachPressScale(btnGuest, 0.97f)

        btnGoogleSignIn.setOnClickListener {
            showTermsConsentDialog()
        }

        btnGuest.setOnClickListener {
            AuthManager.setGuestMode(this)
            proceedToMain()
        }

        val contentRoot = findViewById<View>(R.id.loginContentRoot)
        ViewCompat.setOnApplyWindowInsetsListener(contentRoot) { v, insets ->
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.setPadding(
                v.paddingLeft,
                v.paddingTop,
                v.paddingRight,
                navBars.bottom + 16
            )
            insets
        }

        val tvLegalNotice = findViewById<TextView>(R.id.tvLegalNotice)
        setupLegalNotice(tvLegalNotice)

        // Handle OAuth Deep-Link return if applicable
        intent?.data?.let { handleDeepLink(it) }
    }

    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private fun attachPressScale(view: View, targetScale: Float = 0.97f) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(targetScale).scaleY(targetScale).setDuration(90).start()
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
                }
            }
            false
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.data?.let { handleDeepLink(it) }
    }

    private fun showTermsConsentDialog() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.TransparentBottomSheetDialogTheme)
        val dialogView = layoutInflater.inflate(R.layout.dialog_terms_consent, null)
        dialog.setContentView(dialogView)

        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundColor(Color.TRANSPARENT)
            bottomSheet?.background = null
            if (bottomSheet is android.view.ViewGroup) {
                bottomSheet.clipToOutline = false
            }
        }

        val tvLinks = dialogView.findViewById<TextView>(R.id.tvDialogLegalLinks)
        val cbAccept = dialogView.findViewById<android.widget.CheckBox>(R.id.cbAcceptTerms)
        val btnAgree = dialogView.findViewById<MaterialButton>(R.id.btnAgreeAndContinue)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancelConsent)

        setupDialogLegalLinks(tvLinks)
        attachPressScale(btnAgree, 0.97f)
        attachPressScale(btnCancel, 0.97f)

        btnAgree.setOnClickListener {
            if (!cbAccept.isChecked) {
                Toast.makeText(this, "Please check the box to accept the Terms & Privacy Policy.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val epoch = System.currentTimeMillis()
            AuthManager.recordTermsConsent(this, epochMillis = epoch)
            dialog.dismiss()
            performGoogleSignIn()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupDialogLegalLinks(textView: TextView) {
        val fullText = "Read the complete Terms of Service and Privacy Policy."
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

    private fun completeLoginAndSync(email: String, name: String, accessToken: String?, userId: String, avatarUrl: String? = null) {
        val backupMgr = BackupManager(this)
        val wasGuest = AuthManager.isGuest(this)
        val hasLocalData = backupMgr.hasLocalStudyData()
        val isSameUser = AuthManager.isSameUser(this, userId, email)

        // Always create a pre-auth safety checkpoint before modifying any state
        backupMgr.createPreAuthSafetySnapshot("pre_login_${userId.take(8)}")

        if (!isSameUser && !wasGuest) {
            // Switched from a different signed-in user: reset current memory to avoid leaking previous user profile
            AuthManager.resetLocalUserData(this)
        }

        AuthManager.saveUserSession(this, email, name, accessToken, userId)
        if (!avatarUrl.isNullOrBlank()) {
            AuthManager.saveProfileImageUri(this, avatarUrl)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            // Record consent to Supabase with exact immutable timestamp
            TermsConsentManager.syncConsentToServer(
                this@LoginActivity,
                userId = userId,
                email = email,
                name = name,
                termsVersion = AuthManager.getTermsVersion(this@LoginActivity)
            )

            if (!avatarUrl.isNullOrBlank()) {
                LocalAvatarManager.downloadAndSaveRemoteAvatar(this@LoginActivity, avatarUrl)
            }
            val (remoteMeta, rawRecord) = CloudSyncManager.fetchRemoteMetadata(this@LoginActivity)
            if (remoteMeta != null && remoteMeta.updatedAt > 0L && rawRecord != null) {
                if (wasGuest && hasLocalData) {
                    // Smart non-destructive merge so guest progress merges seamlessly with the account
                    CloudSyncManager.mergeCloudAndLocalData(this@LoginActivity, rawRecord)
                } else {
                    CloudSyncManager.restoreDataFromCloud(this@LoginActivity)
                }
            } else {
                // Fresh cloud account with no existing backup: upload local study data to the account
                CloudSyncManager.syncDataToCloud(this@LoginActivity, force = true)
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(this@LoginActivity, "Welcome, $name!", Toast.LENGTH_SHORT).show()
                proceedToMain()
            }
        }
    }

    private fun exchangeTokenWithSupabase(idToken: String, displayName: String, googleEmail: String) {
        val supabaseUrl = BuildConfig.SUPABASE_URL
        val anonKey = BuildConfig.SUPABASE_ANON_KEY

        if (supabaseUrl.isBlank() || anonKey.isBlank()) {
            completeLoginAndSync(googleEmail, displayName, idToken, googleEmail)
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
                        ?: displayName.takeIf { it.isNotBlank() }
                        ?: googleEmail.substringBefore("@")
                    val email = userObj?.optString("email")?.takeIf { it.isNotBlank() } ?: googleEmail
                    val userId = userObj?.optString("id")?.takeIf { it.isNotBlank() } ?: email
                    val avatarUrl = metaObj?.optString("avatar_url")?.takeIf { it.isNotBlank() }
                        ?: metaObj?.optString("picture")?.takeIf { it.isNotBlank() }
                        ?: metaObj?.optString("avatar")?.takeIf { it.isNotBlank() }

                    withContext(Dispatchers.Main) {
                        completeLoginAndSync(email, fetchedName, accessToken, userId, avatarUrl)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        completeLoginAndSync(googleEmail, displayName, idToken, googleEmail)
                    }
                }
            } catch (e: Exception) {
                Log.e("LoginActivity", "Supabase token exchange error", e)
                withContext(Dispatchers.Main) {
                    completeLoginAndSync(googleEmail, displayName, idToken, googleEmail)
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
            if (!accessToken.isNullOrBlank()) {
                fetchSupabaseProfileAndCompleteLogin(accessToken)
            }
        }
    }

    private fun fetchSupabaseProfileAndCompleteLogin(accessToken: String) {
        val supabaseUrl = BuildConfig.SUPABASE_URL
        val anonKey = BuildConfig.SUPABASE_ANON_KEY

        lifecycleScope.launch(Dispatchers.IO) {
            var userId = ""
            var email = ""
            var name = ""
            var avatarUrl: String? = null

            if (supabaseUrl.isNotBlank() && anonKey.isNotBlank()) {
                try {
                    val url = URL("$supabaseUrl/auth/v1/user")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.setRequestProperty("apikey", anonKey)
                    conn.setRequestProperty("Authorization", "Bearer $accessToken")
                    conn.connectTimeout = 8000
                    conn.readTimeout = 8000

                    if (conn.responseCode in 200..299) {
                        val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                        val userObj = JSONObject(responseStr)
                        userId = userObj.optString("id")
                        email = userObj.optString("email")
                        val meta = userObj.optJSONObject("user_metadata") ?: userObj.optJSONObject("raw_user_meta_data")
                        name = meta?.optString("full_name")?.takeIf { it.isNotBlank() }
                            ?: meta?.optString("name")?.takeIf { it.isNotBlank() }
                            ?: email.substringBefore("@")
                        avatarUrl = meta?.optString("avatar_url")?.takeIf { it.isNotBlank() }
                            ?: meta?.optString("picture")?.takeIf { it.isNotBlank() }
                    }
                } catch (e: Exception) {
                    Log.w("LoginActivity", "Failed to fetch user from Supabase auth endpoint", e)
                }
            }

            // Fallback: parse JWT payload from accessToken if available
            if (userId.isBlank() || email.isBlank()) {
                try {
                    val parts = accessToken.split(".")
                    if (parts.size >= 2) {
                        val payloadBytes = android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING)
                        val jwtObj = JSONObject(String(payloadBytes, Charsets.UTF_8))
                        if (userId.isBlank()) userId = jwtObj.optString("sub")
                        if (email.isBlank()) email = jwtObj.optString("email")
                        if (name.isBlank()) {
                            val meta = jwtObj.optJSONObject("user_metadata")
                            name = meta?.optString("full_name")?.takeIf { it.isNotBlank() }
                                ?: meta?.optString("name")?.takeIf { it.isNotBlank() }
                                ?: email.substringBefore("@")
                        }
                    }
                } catch (_: Exception) {}
            }

            if (email.isBlank()) email = "user_${System.currentTimeMillis()}@studytimer.app"
            if (userId.isBlank()) userId = email
            if (name.isBlank()) name = email.substringBefore("@")

            withContext(Dispatchers.Main) {
                completeLoginAndSync(email, name, accessToken, userId, avatarUrl)
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
