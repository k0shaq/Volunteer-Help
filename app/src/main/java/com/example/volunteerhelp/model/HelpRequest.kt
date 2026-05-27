package com.example.volunteerhelp.model

data class HelpRequest(
    val id: String = "",
    val campaignId: String = "",
    val campaignTitle: String = "",
    val donorId: String = "",
    val donorName: String = "",
    val donorUsername: String = "",
    val donorAvatarUrl: String? = null,
    val volunteerId: String = "",
    val type: String = CampaignType.FINANCIAL.name,
    val amount: Double = 0.0,
    val comment: String = "",
    val itemDescription: String = "",
    val screenshotUrl: String? = null,
    val status: String = HelpRequestStatus.PENDING.name,
    val createdAt: Long = 0L
)
