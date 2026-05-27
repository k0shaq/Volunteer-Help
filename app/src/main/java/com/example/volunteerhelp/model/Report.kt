package com.example.volunteerhelp.model

data class Report(
    val id: String = "",
    val campaignId: String = "",
    val campaignTitle: String = "",
    val volunteerId: String = "",
    val volunteerName: String = "",
    val volunteerUsername: String = "",
    val volunteerAvatarUrl: String? = null,
    val volunteerVerified: Boolean = false,
    val description: String = "",
    val imageUrl: String? = null,
    val createdAt: Long = 0L
)
