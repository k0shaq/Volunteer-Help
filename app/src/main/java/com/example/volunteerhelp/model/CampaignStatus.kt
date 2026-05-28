package com.example.volunteerhelp.model

enum class CampaignStatus {
    ACTIVE,
    GOAL_REACHED,
    CLOSED,
    REPORTED,
    /**
     * Legacy Firestore value. New writes should use GOAL_REACHED.
     */
    COMPLETED,
    ;

    companion object {
        fun fromStorage(value: String): CampaignStatus {
            return when (value) {
                GOAL_REACHED.name, COMPLETED.name -> GOAL_REACHED
                CLOSED.name -> CLOSED
                REPORTED.name -> REPORTED
                ACTIVE.name -> ACTIVE
                else -> ACTIVE
            }
        }

        fun normalizedName(value: String): String = fromStorage(value).name

        fun label(value: String): String {
            return when (fromStorage(value)) {
                ACTIVE -> "Активний"
                GOAL_REACHED -> "Ціль досягнуто"
                CLOSED -> "Закрито"
                REPORTED -> "Звіт додано"
                COMPLETED -> "Ціль досягнуто"
            }
        }

        fun canBeClosed(value: String): Boolean {
            return fromStorage(value) in listOf(ACTIVE, GOAL_REACHED)
        }

        fun canReceiveReport(value: String): Boolean {
            return fromStorage(value) in listOf(CLOSED, GOAL_REACHED)
        }
    }
}
