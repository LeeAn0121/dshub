package com.jongwook.dshub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
            } catch (e: ApiException) {
                viewModel.clearRepository()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val requiredScopes = listOf(SheetsScopes.SPREADSHEETS, DriveScopes.DRIVE_METADATA_READONLY)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(SheetsScopes.SPREADSHEETS), Scope(DriveScopes.DRIVE_METADATA_READONLY))
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        credential = GoogleAccountCredential
            .usingOAuth2(this, requiredScopes)
            .setBackOff(ExponentialBackOff())

        val lastAccount = GoogleSignIn.getLastSignedInAccount(this)
        if (lastAccount != null &&
            GoogleSignIn.hasPermissions(lastAccount, Scope(SheetsScopes.SPREADSHEETS))
        ) {
            credential.selectedAccount = lastAccount.account
            viewModel.initRepository(SheetsRepository(this, credential))
        }

        setContent {
            DSHubTheme {
                val isInitialized by viewModel.isInitialized.collectAsState()

                if (isInitialized) {
                    MainNavGraph(
                        viewModel = viewModel,
                        onSignOut = {
                            googleSignInClient.signOut().addOnCompleteListener {
                                viewModel.clearRepository()
                            }
                        }
                    )
                } else {
                    SignInScreen(
                        onSignIn = {
                            signInLauncher.launch(googleSignInClient.signInIntent)
                        }
                    )
                }
            }
        }
    }
}
