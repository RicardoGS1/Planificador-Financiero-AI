package com.virtualworld.easyexpensecontrol.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.virtualworld.easyexpensecontrol.R
import com.virtualworld.easyexpensecontrol.core.SetStatusBarColor
import com.virtualworld.easyexpensecontrol.ui.screens.AddEditDetailBudgetView
import com.virtualworld.easyexpensecontrol.ui.screens.AddEditDetailTransactionView
import com.virtualworld.easyexpensecontrol.ui.screens.BudgetScreen
import com.virtualworld.easyexpensecontrol.ui.screens.BudgetHistoryScreen
import com.virtualworld.easyexpensecontrol.ui.screens.DashboardScreen
import com.virtualworld.easyexpensecontrol.ui.screens.HistoryScreen
import com.virtualworld.easyexpensecontrol.ui.screens.SettingsScreen
import com.virtualworld.easyexpensecontrol.ui.screens.SplashScreen
import com.virtualworld.easyexpensecontrol.ui.screens.StaticsScreen
import com.virtualworld.easyexpensecontrol.viewmodel.BudgetViewModel
import com.virtualworld.easyexpensecontrol.viewmodel.CategoryViewModel
import com.virtualworld.easyexpensecontrol.ads.InterstitialAdHelper
import com.virtualworld.easyexpensecontrol.viewmodel.TransactionViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun Navigation(
    transactionViewModel: TransactionViewModel = koinViewModel(),
    categoryViewModel: CategoryViewModel = koinViewModel(),
    budgetViewModel: BudgetViewModel = koinViewModel(),
    navController: NavHostController,
    onPlaySound: (Int) -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        InterstitialAdHelper.preload(context)
    }

    SetStatusBarColor(
        statusBarColor = colorResource(R.color.app_bar_color),
        navigationBarColor = colorResource(R.color.bold_from_palette)
    )

    NavHost(
        navController = navController,
        startDestination = Screen.SplashScreen.route
    ) {
        composable(route = Screen.SplashScreen.route) {
            SplashScreen(navController = navController)
        }
        composable(route = Screen.DashboardScreen.route) {
            DashboardScreen(navController, transactionViewModel, categoryViewModel)
        }
        composable(route = Screen.BudgetScreen.route) {
            BudgetScreen(
                navController = navController,
                budgetViewModel = budgetViewModel,
                categoryViewModel = categoryViewModel,
                transactionViewModel = transactionViewModel
            )
        }
        composable(route = Screen.BudgetHistoryScreen.route) {
            BudgetHistoryScreen(
                navController = navController,
                budgetViewModel = budgetViewModel,
                categoryViewModel = categoryViewModel
            )
        }
        composable(
            route = Screen.AddEditBudgetScreen.route + "/{id}",
            arguments = listOf(
                navArgument("id") {
                    type = NavType.LongType
                    defaultValue = 0L
                    nullable = false
                }
            )
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            AddEditDetailBudgetView(
                id = id,
                budgetViewModel = budgetViewModel,
                categoryViewModel = categoryViewModel,
                transactionViewModel = transactionViewModel,
                navController = navController
            )
        }
        composable(
            route = Screen.AddEditTransactionScreen.route + "/{id}",
            arguments = listOf(
                navArgument("id") {
                    type = NavType.LongType
                    defaultValue = 0L
                    nullable = false
                }
            )
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            AddEditDetailTransactionView(
                id = id,
                transactionViewModel = transactionViewModel,
                categoryViewModel = categoryViewModel,
                navController = navController
            )
        }
        composable(route = Screen.HistoryScreen.route) {
            HistoryScreen(
                navController = navController,
                transactionViewModel = transactionViewModel,
                categoryViewModel = categoryViewModel,
                budgetViewModel = budgetViewModel,
                onPlaySound = onPlaySound
            )
        }
        composable(route = Screen.StaticsScreen.route) {
            StaticsScreen(
                navController = navController,
                transactionViewModel = transactionViewModel,
                categoryViewModel = categoryViewModel
            )
        }
        composable(route = Screen.SettingsScreen.route) {
            SettingsScreen(navController = navController)
        }
    }
}
