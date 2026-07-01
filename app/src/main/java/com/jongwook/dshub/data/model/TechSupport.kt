package com.jongwook.dshub.data.model

data class TechSupport(
    val rowIndex: Int = -1,
    val registrationDate: String = "",
    val stage: String = Stage.RECEIVED.displayName,
    val requestDate: String = "",
    val scheduledDate: String = "",
    val completionDate: String = "",
    val category: String = Category.TECH_SUPPORT.displayName,
    val siteName: String = "",
    val assignee: String = "",
    val requestDetails: String = "",
    val processDetails: String = "",
    val notes: String = ""
) {
    fun toRowValues(): List<Any> = listOf(
        registrationDate, stage, requestDate, scheduledDate,
        completionDate, category, siteName, assignee,
        requestDetails, processDetails, notes
    )

    companion object {
        fun fromRow(rowIndex: Int, row: List<Any?>): TechSupport = TechSupport(
            rowIndex = rowIndex,
            registrationDate = row.getOrNull(0)?.toString() ?: "",
            stage = row.getOrNull(1)?.toString() ?: Stage.RECEIVED.displayName,
            requestDate = row.getOrNull(2)?.toString() ?: "",
            scheduledDate = row.getOrNull(3)?.toString() ?: "",
            completionDate = row.getOrNull(4)?.toString() ?: "",
            category = row.getOrNull(5)?.toString() ?: Category.TECH_SUPPORT.displayName,
            siteName = row.getOrNull(6)?.toString() ?: "",
            assignee = row.getOrNull(7)?.toString() ?: "",
            requestDetails = row.getOrNull(8)?.toString() ?: "",
            processDetails = row.getOrNull(9)?.toString() ?: "",
            notes = row.getOrNull(10)?.toString() ?: ""
        )

        val HEADER = listOf(
            "등록일", "진행단계", "요청일", "예정일", "완료일",
            "구분", "현장명", "담당", "요청사항", "처리내역", "비고"
        )
    }
}
