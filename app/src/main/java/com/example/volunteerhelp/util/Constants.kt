package com.example.volunteerhelp.util

object Constants {
    const val CLOUDINARY_CLOUD_NAME = "dlabl1umr"
    const val CLOUDINARY_UPLOAD_PRESET = "volunteer_unsigned"
    const val USERS_COLLECTION = "users"
    const val USERNAMES_COLLECTION = "usernames"
    const val FOLLOWS_COLLECTION = "follows"
    const val CAMPAIGNS_COLLECTION = "campaigns"
    const val HELP_REQUESTS_COLLECTION = "helpRequests"
    const val REPORTS_COLLECTION = "reports"
    const val FEED_LIMIT = 80L
    const val USER_SEARCH_LIMIT = 20L
    const val USER_SEARCH_FALLBACK_LIMIT = 80L
    const val DENORMALIZED_UPDATE_LIMIT = 450L
    const val MAX_IMAGE_UPLOAD_BYTES = 8L * 1024L * 1024L
    const val MAX_IMAGE_DIMENSION = 1600
    const val IMAGE_JPEG_QUALITY = 82
}

object FormLimits {
    const val CAMPAIGN_TITLE_MAX = 80
    const val CAMPAIGN_DESCRIPTION_MAX = 1000
    const val REQUISITES_MAX = 500
    const val HELP_REQUEST_COMMENT_MAX = 300
    const val CITY_MAX = 50
    const val BIO_MAX = 300
    const val NAME_MAX = 50
    const val USERNAME_MIN = 3
    const val USERNAME_MAX = 20
    const val MATERIAL_GOAL_MAX = 300
}

object UkraineRegions {
    val values = listOf(
        "Вінницька область",
        "Волинська область",
        "Дніпропетровська область",
        "Донецька область",
        "Житомирська область",
        "Закарпатська область",
        "Запорізька область",
        "Івано-Франківська область",
        "Київська область",
        "Кіровоградська область",
        "Луганська область",
        "Львівська область",
        "Миколаївська область",
        "Одеська область",
        "Полтавська область",
        "Рівненська область",
        "Сумська область",
        "Тернопільська область",
        "Харківська область",
        "Херсонська область",
        "Хмельницька область",
        "Черкаська область",
        "Чернівецька область",
        "Чернігівська область",
        "Автономна Республіка Крим",
        "Не в Україні / Інша країна"
    )
}
