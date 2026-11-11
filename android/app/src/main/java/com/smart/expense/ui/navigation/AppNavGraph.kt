package com.smart.expense.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.smart.expense.data.repository.AuthRepository
import com.smart.expense.data.repository.ExpenseRepository
import com.smart.expense.ui.screens.activity.TransactionHistoryScreen
import com.smart.expense.ui.screens.analytics.AnalyticsScreen
import com.smart.expense.ui.screens.auth.ForgotPasswordScreen
import com.smart.expense.ui.screens.auth.LoginScreen
import com.smart.expense.ui.screens.auth.RegisterScreen
import com.smart.expense.ui.screens.budget.BudgetScreen
import com.smart.expense.ui.screens.home.HomeScreen
import com.smart.expense.ui.screens.manual.AddEditTransactionScreen
import com.smart.expense.ui.screens.profile.ProfileScreen
import com.smart.expense.ui.screens.splash.SplashScreen
import com.smart.expense.ui.screens.voice.VoiceAssistantQueryScreen
import com.smart.expense.ui.theme.ForestGreenPrimary

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun AppNavGraph(
    authRepository: AuthRepository,
    expenseRepository: ExpenseRepository
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem(Screen.Home.route, "Home", Icons.Filled.Home, Icons.Outlined.Home),
        BottomNavItem(Screen.Activity.route, "Activity", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
        BottomNavItem(Screen.Budget.route, "Budgets", Icons.Filled.PieChart, Icons.Outlined.PieChart),
        BottomNavItem(Screen.Analytics.route, "Insights", Icons.Filled.Analytics, Icons.Outlined.Analytics),
        BottomNavItem(Screen.Profile.route, "Profile", Icons.Filled.Person, Icons.Outlined.Person)
    )

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) },
                            selected = isSelected,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ForestGreenPrimary,
                                selectedTextColor = ForestGreenPrimary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    authRepository = authRepository,
                    expenseRepository = expenseRepository,
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Login.route) {
                LoginScreen(
                    authRepository = authRepository,
                    expenseRepository = expenseRepository,
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                    onNavigateToForgotPassword = { navController.navigate(Screen.ForgotPassword.route) }
                )
            }

            composable(Screen.Register.route) {
                RegisterScreen(
                    authRepository = authRepository,
                    expenseRepository = expenseRepository,
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }

            composable(Screen.ForgotPassword.route) {
                ForgotPasswordScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Bottom Nav Roots
            composable(Screen.Home.route) {
                HomeScreen(
                    authRepository = authRepository,
                    expenseRepository = expenseRepository,
                    onNavigateToActivity = { navController.navigate(Screen.Activity.route) },
                    onNavigateToBudgets = { navController.navigate(Screen.Budget.route) },
                    onNavigateToVoiceQuery = { navController.navigate(Screen.VoiceQuery.route) },
                    onNavigateToAddManual = { navController.navigate(Screen.AddEditTransaction.createRoute()) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
                )
            }

            composable(Screen.Activity.route) {
                TransactionHistoryScreen(
                    authRepository = authRepository,
                    expenseRepository = expenseRepository,
                    onNavigateBack = { navController.navigate(Screen.Home.route) }
                )
            }

            composable(Screen.Budget.route) {
                BudgetScreen(
                    authRepository = authRepository,
                    expenseRepository = expenseRepository,
                    onNavigateBack = { navController.navigate(Screen.Home.route) }
                )
            }

            composable(Screen.Analytics.route) {
                AnalyticsScreen(
                    authRepository = authRepository,
                    expenseRepository = expenseRepository,
                    onNavigateBack = { navController.navigate(Screen.Home.route) }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    authRepository = authRepository,
                    expenseRepository = expenseRepository,
                    onNavigateBack = { navController.navigate(Screen.Home.route) },
                    onLogout = {
                        authRepository.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // Subscreens
            composable(Screen.AddEditTransaction.route) {
                AddEditTransactionScreen(
                    authRepository = authRepository,
                    expenseRepository = expenseRepository,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.VoiceQuery.route) {
                VoiceAssistantQueryScreen(
                    authRepository = authRepository,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

