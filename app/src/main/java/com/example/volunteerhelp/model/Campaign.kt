package com.example.volunteerhelp.model

data class Campaign(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val type: String = CampaignType.FINANCIAL.name,
    val category: String = "",
    val targetAmount: Double = 0.0,
    val currentAmount: Double = 0.0,
    val materialGoal: String = "",
    val city: String = "",
    val region: String = "",
    val imageUrl: String? = null,
    val requisites: String = "",
    val volunteerId: String = "",
    val volunteerName: String = "",
    val volunteerUsername: String = "",
    val volunteerAvatarUrl: String? = null,
    val volunteerVerified: Boolean = false,
    val status: String = CampaignStatus.ACTIVE.name,
    val createdAt: Long = 0L
)
