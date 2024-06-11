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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.com.terras.app.dsm.ui.annotation.ThemePreviews
import br.com.terras.app.dsm.ui.theme.DSMTheme

@Composable
fun FloatBottomNavigation(items: List<BottomNavItem>, navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = 9.dp)
            .clip(RoundedCornerShape(18.dp)),
        containerColor = MaterialTheme.colorScheme.primary
    ) {
        items.forEach { item ->
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

                selected = currentRoute == item.route,

                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
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
        BottomNavItem("Home", android.R.drawable.star_off, "Home"),
        BottomNavItem("Home", android.R.drawable.sym_action_call, "Home")
    )

    DSMTheme {
        FloatBottomNavigation(items = items, navController = rememberNavController())
    }
}