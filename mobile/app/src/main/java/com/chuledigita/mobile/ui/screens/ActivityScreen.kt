package com.chuledigita.mobile.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

@Deprecated("Use NotesScreen instead", ReplaceWith("NotesScreen(navController)"))
@Composable
fun ActivityScreen(
    navController: NavHostController
) {
    NotesScreen(navController = navController)
}
