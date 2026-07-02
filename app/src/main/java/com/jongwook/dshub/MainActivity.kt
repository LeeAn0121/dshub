package com.jongwook.dshub

import android.accounts.AccountManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.jongwook.dshub.data.repository.SheetsRepository
import com.jongwook.dshub.ui.navigation.MainNavGraph
import com.jongwook.dshub.ui.screens.SignInScreen
import com.jongwook.dshub.ui.theme.DSHubTheme
import com.jongwook.dshub.ui.viewmodel.MainViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.drive.DriveScopes
import com.google.api.services.sheets.v4.SheetsScopes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var credential: GoogleAccountCredential

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            try {
                val account = GoogleSignIn
                    .getSignedInAccountFromIntent(result.data)
                    .getResult(ApiException::class.java)
                credential.selectedAccount = account.account
                viewModel.initRepository(SheetsRepository(this, credential))
                // 계정 정보 로컬 저장
                lifecycleScope.launch {
                    viewModel.prefsRepo.saveAccount(
                        email       = account.email ?: "",
                        displayName = account.displayName ?: ""
                    )
                }
            } catch (e: ApiException) {
                val msg = "로그인 실패 (코드 ${e.statusCode}): ${e.message}"
                Log.e("DSHub", msg, e)
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                viewModel.clearRepository()
                viewModel.setAuthCheckDone()
            }
        } else {
            Log.w("DSHub", "Sign-in cancelled, resultCode=${result.resultCode}")
            viewModel.setAuthCheckDone()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val requiredScopes = listOf(SheetsScopes.SPREADSHEETS, DriveScopes.DRIVE_METADATA_READONLY)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(SheetsScopes.SPREADSHEETS))
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        credential = GoogleAccountCredential
            .usingOAuth2(this, requiredScopes)
            .setBackOff(ExponentialBackOff())

        // ── 자동 로그인 시도 ──────────────────────────────────────────────────
        attemptAutoLogin()

        setContent {
            DSHubTheme {
                val isInitialized  by viewModel.isInitialized.collectAsState()
                val isAuthChecking by viewModel.isAuthChecking.collectAsState()

                when {
                    isAuthChecking -> {
                        // 자동 로그인 확인 중 — 로딩 스피너
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    isInitialized -> {
                        MainNavGraph(
                            viewModel = viewModel,
                            onSignOut = {
                                googleSignInClient.signOut().addOnCompleteListener {
                                    lifecycleScope.launch {
                                        viewModel.prefsRepo.clearAccount()
                                    }
                                    viewModel.clearRepository()
                                }
                            }
                        )
                    }
                    else -> {
                        SignInScreen(onSignIn = {
                            signInLauncher.launch(googleSignInClient.signInIntent)
                        })
                    }
                }
            }
        }
    }

    private fun attemptAutoLogin() {
        // 1단계: Google Sign-In 무음 로그인 시도
        googleSignInClient.silentSignIn().addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val account = task.result
                Log.d("DSHub", "Silent sign-in OK: ${account?.email}")
                credential.selectedAccount = account?.account
                viewModel.initRepository(SheetsRepository(this, credential))
                lifecycleScope.launch {
                    viewModel.prefsRepo.saveAccount(
                        email       = account?.email ?: "",
                        displayName = account?.displayName ?: ""
                    )
                }
            } else {
                // 2단계: DataStore에 저장된 이메일로 AccountManager에서 계정 복원
                lifecycleScope.launch {
                    val savedEmail = viewModel.prefsRepo.accountEmail.first()
                    val restored = tryRestoreFromAccountManager(savedEmail)
                    if (!restored) {
                        // 3단계: GoogleSignIn 캐시 확인
                        val last = GoogleSignIn.getLastSignedInAccount(this@MainActivity)
                        if (last != null) {
                            Log.d("DSHub", "Restored from getLastSignedInAccount: ${last.email}")
                            credential.selectedAccount = last.account
                            viewModel.initRepository(SheetsRepository(this@MainActivity, credential))
                        } else {
                            Log.d("DSHub", "Auto-login failed — show sign-in screen")
                            viewModel.setAuthCheckDone()
                        }
                    }
                }
            }
        }
    }

    private fun tryRestoreFromAccountManager(savedEmail: String): Boolean {
        if (savedEmail.isBlank()) return false
        val accounts = (getSystemService(ACCOUNT_SERVICE) as AccountManager)
            .getAccountsByType("com.google")
        val found = accounts.find { it.name.equals(savedEmail, ignoreCase = true) }
        return if (found != null) {
            Log.d("DSHub", "Restored account from AccountManager: ${found.name}")
            credential.selectedAccount = found
            viewModel.initRepository(SheetsRepository(this, credential))
            true
        } else false
    }
}
