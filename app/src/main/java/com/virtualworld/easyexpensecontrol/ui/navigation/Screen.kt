package com.virtualworld.easyexpensecontrol.ui.navigation

sealed class Screen(val route: String) {
    data object DashboardScreen : Screen("dashboard")
    data object HistoryScreen : Screen("history")
    data object AddEditTransactionScreen : Screen("add")
    data object StaticsScreen : Screen("statics")
    data object BudgetScreen : Screen("budget")
    data object AddEditBudgetScreen : Screen("addBudget")
    data object SettingsScreen : Screen("settings")
}
