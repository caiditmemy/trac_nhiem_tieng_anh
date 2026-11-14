package vn.edu.qnu.st.nambede.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.delay
import vn.edu.qnu.st.nambede.AppContainer
import vn.edu.qnu.st.nambede.data.User
import vn.edu.qnu.st.nambede.ui.exercises.ExercisesRoute
import vn.edu.qnu.st.nambede.ui.exercises.TakeExerciseRoute
import vn.edu.qnu.st.nambede.ui.home.HomeRoute
import vn.edu.qnu.st.nambede.ui.profile.ProfileRoute

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab("home", "Tổng quan", Icons.Filled.Home),
    Tab("exercises", "Bài tập", Icons.AutoMirrored.Filled.List),
)

/** Short messages ("Đã tham gia lớp ...") shown at the bottom of any screen. */
val LocalSnackbar = staticCompositionLocalOf<SnackbarHostState> { error("No SnackbarHostState provided") }

/** Bottom tabs like the web app's student menu; exercise and account screens open on top with a back arrow. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(container: AppContainer, user: User) {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val tab = TABS.firstOrNull { it.route == route }
    val snackbar = remember { SnackbarHostState() }

    CompositionLocalProvider(LocalSnackbar provides snackbar) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            when {
                                route?.startsWith("exercise/") == true -> "Làm bài tập"
                                route == "profile" -> "Tài khoản"
                                else -> tab?.label ?: "NamBeDe"
                            }
                        )
                    },
                    navigationIcon = {
                        if (tab == null && route != null) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                            }
                        }
                    },
                    actions = {
                        if (tab != null) {
                            IconButton(onClick = { navController.navigate("profile") }) {
                                Icon(Icons.Filled.AccountCircle, contentDescription = "Tài khoản của tôi")
                            }
                        }
                    },
                )
            },
            bottomBar = {
                if (tab != null) {
                    NavigationBar {
                        TABS.forEach { item ->
                            NavigationBarItem(
                                selected = item == tab,
                                onClick = { navController.openTab(item.route) },
                                icon = { Icon(item.icon, contentDescription = null) },
                                label = { Text(item.label) },
                            )
                        }
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            NavHost(navController, startDestination = "home", modifier = Modifier.padding(padding)) {
                composable("home") { HomeRoute(container, user, onOpenExercises = { navController.openTab("exercises") }) }
                composable("exercises") { ExercisesRoute(container, onOpen = { id -> navController.navigate("exercise/$id") }) }
                composable("exercise/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { backStack ->
                    TakeExerciseRoute(container, backStack.arguments?.getLong("id") ?: 0L)
                }
                composable("profile") { ProfileRoute(container, user) }
            }
        }
    }
}

/** Switch tabs without stacking them up, keeping each tab's scroll position. */
private fun NavHostController.openTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
