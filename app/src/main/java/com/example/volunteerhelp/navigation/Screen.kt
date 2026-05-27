package com.example.volunteerhelp.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Main : Screen("main")
    data object CampaignDetails : Screen("campaign_details/{campaignId}") {
        fun createRoute(campaignId: String): String = "campaign_details/$campaignId"
    }
    data object CreateCampaign : Screen("create_campaign")
    data object Profile : Screen("profile")
    data object EditProfile : Screen("edit_profile")
    data object SearchProfiles : Screen("search_profiles")
    data object VolunteerVerification : Screen("volunteer_verification")
    data object PublicProfile : Screen("public_profile/{userId}") {
        fun createRoute(userId: String): String = "public_profile/$userId"
    }
    data object HelpForm : Screen("help_form/{campaignId}") {
        fun createRoute(campaignId: String): String = "help_form/$campaignId"
    }
    data object AddReport : Screen("add_report/{campaignId}") {
        fun createRoute(campaignId: String): String = "add_report/$campaignId"
    }
}
