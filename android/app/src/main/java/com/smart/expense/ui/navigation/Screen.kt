package com.smart.expense.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")

    // Main App Navigation
    object Home : Screen("home")
    object Activity : Screen("activity")
    object Budget : Screen("budget")
    object Analytics : Screen("analytics")
    object Profile : Screen("profile")

    // Modals & Subscreens
    object AddEditTransaction : Screen("add_edit_transaction?transactionId={transactionId}") {
        fun createRoute(transactionId: String? = null) =
            if (transactionId != null) "add_edit_transaction?transactionId=$transactionId" else "add_edit_transaction"
    }
    object VoiceQuery : Screen("voice_query")
}

