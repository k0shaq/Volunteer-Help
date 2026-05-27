package com.example.volunteerhelp.model

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val username: String = "",
    val usernameLowercase: String = "",
    val role: String = UserRole.DONOR.name,
    val avatarUrl: String? = null,
    val coverImageUrl: String? = null,
    val bio: String = "",
    val city: String = "",
    val region: String = "",
    val rating: Int = 0,
    val isVerified: Boolean = false,
    val verifiedAt: Long = 0L,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val closedCampaignsCount: Int = 0,
    val totalHelpAmount: Double = 0.0,
    val createdAt: Long = 0L
)
