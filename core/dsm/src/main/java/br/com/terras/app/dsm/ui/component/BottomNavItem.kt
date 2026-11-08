package br.com.terras.app.dsm.ui.component

data class BottomNavItem(
    val title: String,
    val icon: Int,
    val destination: Any
) {
    val route: String get() = destination.toString()
}