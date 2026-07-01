package com.jongwook.dshub.data.model

enum class Category(val displayName: String) {
    FAULT_HANDLING("장애처리"),
    TECH_SUPPORT("기술지원"),
    RELOCATION("이전설치"),
    NEW_INSTALLATION("신규설치"),
    INSPECTION("점검"),
    REMOVAL("철거"),
    NEW_DEVELOPMENT("신규개발");

    companion object {
        fun fromDisplayName(name: String) = entries.find { it.displayName == name } ?: TECH_SUPPORT
        fun displayNames() = entries.map { it.displayName }
    }
}
