package com.example.nanobot.navigation

sealed class Destinations(val route: String) {
    data object Onboarding : Destinations("onboarding")
    data object Chat : Destinations("chat")
    data object Sessions : Destinations("sessions")
    data object Memory : Destinations("memory")
    data object Tools : Destinations("tools")
    data object Settings : Destinations("settings")
}
