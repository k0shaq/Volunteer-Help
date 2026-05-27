package com.example.volunteerhelp.model

sealed class FeedItem {
    abstract val sortTime: Long

    data class CampaignItem(val campaign: Campaign) : FeedItem() {
        override val sortTime: Long = campaign.createdAt
    }

    data class ReportItem(val report: Report) : FeedItem() {
        override val sortTime: Long = report.createdAt
    }
}
