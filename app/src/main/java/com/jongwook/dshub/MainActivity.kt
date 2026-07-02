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
        try {
            val account = GoogleSignIn
                .getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            val androidAccount = account.account
            if (androidAccount == null) {
                showSignInError("선택한 Google 계정 정보를 가져오지 못했습니다.")
                return@registerForActivityResult
            }

            credential.selectedAccount = androidAccount
            viewModel.initRepository(SheetsRepository(this, credential))
            lifecycleScope.launch {
                viewModel.prefsRepo.saveAccount(
                    email       = account.email ?: androidAccount.name,
                    displayName = account.displayName ?: ""
                )
            }
        } catch (e: ApiException) {
            showSignInError("로그인 실패 (코드 ${e.statusCode}): ${e.statusMessage ?: e.message}", e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val requiredScopes = listOf(SheetsScopes.SPREADSHEETS, DriveScopes.DRIVE_METADATA_READONLY)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(
                Scope(requiredScopes.first()),
                *requiredScopes.drop(1).map(::Scope).toTypedArray()
            )
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
                                credential.selectedAccount = null
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
        // 1단계: GoogleSignIn 로컬 캐시 확인 (일반 재시작 시 가장 빠르고 신뢰성 높음)
        val last = GoogleSignIn.getLastSignedInAccount(this)
        if (last != null) {
            Log.d("DSHub", "Auto-login via cached account: ${last.email}")
            credential.selectedAccount = last.account
            viewModel.initRepository(SheetsRepository(this, credential))
            return
        }
        // 2단계: DataStore 저장 이메일 + AccountManager 로 복원
        lifecycleScope.launch {
            val savedEmail = viewModel.prefsRepo.accountEmail.first()
            if (!tryRestoreFromAccountManager(savedEmail)) {
                Log.d("DSHub", "Auto-login failed — show sign-in screen")
                viewModel.setAuthCheckDone()
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

    private fun showSignInError(message: String, error: Throwable? = null) {
        if (error == null) {
            Log.e("DSHub", message)
        } else {
            Log.e("DSHub", message, error)
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        viewModel.clearRepository()
        viewModel.setAuthCheckDone()
    }
}
