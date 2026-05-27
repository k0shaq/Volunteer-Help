package com.example.volunteerhelp.model

data class ProfileStats(
    val totalCampaigns: Int = 0,
    val activeCampaigns: Int = 0,
    val completedCampaigns: Int = 0,
    val closedCampaigns: Int = 0,
    val reportsCount: Int = 0,
    val totalRaisedAmount: Double = 0.0,
    val pendingHelpRequestsCount: Int = 0,
    val approvedHelpRequestsCount: Int = 0,
    val approvedHelpCount: Int = 0,
    val pendingHelpCount: Int = 0,
    val rejectedHelpCount: Int = 0,
    val totalDonatedAmount: Double = 0.0,
    val rating: Int = 0,
    val level: String = "",
    val title: String = ""
)
