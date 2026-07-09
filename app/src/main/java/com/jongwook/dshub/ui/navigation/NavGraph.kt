package com.jongwook.dshub.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jongwook.dshub.data.model.TechSupport
import com.jongwook.dshub.ui.screens.DetailScreen
import com.jongwook.dshub.ui.screens.FormScreen
import com.jongwook.dshub.ui.screens.HomeScreen
import com.jongwook.dshub.ui.screens.SettingsScreen
import com.jongwook.dshub.ui.viewmodel.MainViewModel

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Detail : Screen("detail")
    data object Form : Screen("form")
    data object Settings : Screen("settings")
}

@Composable
fun MainNavGraph(viewModel: MainViewModel, onSignOut: () -> Unit) {
    val navController = rememberNavController()

    // remember로 유지해야 재구성(recomposition) 시 선택 항목이 사라지지 않음
    var selectedEntry by remember { mutableStateOf<TechSupport?>(null) }

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToDetail = { entry ->
                    selectedEntry = entry
                    navController.navigate(Screen.Detail.route)
                },
                onNavigateToForm = {
                    selectedEntry = null
                    navController.navigate(Screen.Form.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Detail.route) {
            // 수정/단계 변경 후 목록이 갱신되면 상세화면도 최신 데이터를 표시
            val entries by viewModel.entries.collectAsState()
            val liveEntry = selectedEntry?.let { sel ->
                entries.find { it.rowIndex == sel.rowIndex } ?: sel
            }
            liveEntry?.let { entry ->
                DetailScreen(
                    entry = entry,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { entryToEdit ->
                        selectedEntry = entryToEdit
                        navController.navigate(Screen.Form.route)
                    }
                )
            }
        }

        composable(Screen.Form.route) {
            FormScreen(
                initialEntry = selectedEntry,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onSignOut = {
                    onSignOut()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0)
                    }
                }
            )
        }
    }
}
