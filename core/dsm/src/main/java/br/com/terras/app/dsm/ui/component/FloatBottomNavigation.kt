package br.com.terras.app.dsm.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.theme.DSMTheme

@Composable
fun FloatBottomNavigation(items: List<BottomNavItem>, navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = 9.dp)
            .clip(RoundedCornerShape(18.dp)),
        containerColor = MaterialTheme.colorScheme.primary
    ) {
        items.forEach { item ->
            val isSelected = currentDestination?.hierarchy?.any {
                it.hasRoute(item.destination::class)
            } == true

            NavigationBarItem(
                label = {
                    Text(text = item.title)
                },

                icon = {
                    Icon(
                        painterResource(id = item.icon),
                        contentDescription = item.title
                    )
                },

                selected = isSelected,

                onClick = {
                    navController.navigate(item.destination) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },

                alwaysShowLabel = true
            )
        }
    }
}

@ThemePreviews
@Composable
fun FloatBottomNavigationPreview() {
    val items = listOf(
        BottomNavItem("Home", android.R.drawable.star_on, "Home"),
        BottomNavItem("Search", android.R.drawable.star_off, "Search"),
        BottomNavItem("Feeds", android.R.drawable.sym_action_call, "Feeds")
    )

    DSMTheme {
        FloatBottomNavigation(items = items, navController = rememberNavController())
    }
}