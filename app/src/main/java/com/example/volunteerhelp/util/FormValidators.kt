package com.example.volunteerhelp.util

object FormValidators {
    fun validateMax(value: String, max: Int, fieldName: String) {
        require(value.length <= max) { "$fieldName має містити не більше $max символів" }
    }

    fun validateName(value: String) {
        require(value.isNotBlank()) { "Ім'я не може бути порожнім" }
        validateMax(value, FormLimits.NAME_MAX, "Ім'я")
    }

    fun validateUsername(value: String) {
        require(value.length in FormLimits.USERNAME_MIN..FormLimits.USERNAME_MAX) {
            "Нікнейм має містити від ${FormLimits.USERNAME_MIN} до ${FormLimits.USERNAME_MAX} символів"
        }
        require(value.matches(Regex("^[A-Za-z0-9._]+$"))) {
            "Нікнейм може містити латинські літери, цифри, крапку та _"
        }
    }

    fun validateCity(value: String) {
        require(value.isNotBlank()) { "Вкажіть місто" }
        validateMax(value, FormLimits.CITY_MAX, "Місто")
    }
}
