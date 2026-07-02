package com.jongwook.dshub.data.model

data class TechSupport(
    val rowIndex: Int = -1,
    val sequenceNumber: Int = -1,            // A열 순번 (rowIndex - 3)
    val registrationDate: String = "",
    val stage: String = Stage.RECEIVED.displayName,
    val requestDate: String = "",            // 폼에서 개별 입력, 시트엔 D열에 합산
    val scheduledDate: String = "",
    val completionDate: String = "",
    val category: String = Category.TECH_SUPPORT.displayName,
    val siteName: String = "",
    val assignee: String = "",
    val requestDetails: String = "",
    val processDetails: String = "",
    val notes: String = ""
) {
    // D열에 기록할 "요청일 / 예정일" 문자열
    private fun reqSchedStr(): String {
        val r = requestDate.trim()
        val s = scheduledDate.trim()
        return when {
            r.isNotBlank() && s.isNotBlank() -> "$r / $s"
            r.isNotBlank() -> r
            s.isNotBlank() -> "/ $s"
            else -> ""
        }
    }

    // seqNum: 신규 등록 시 targetRow - 3, 수정 시 기존 sequenceNumber
    fun toRowValues(seqNum: Int = sequenceNumber): List<Any> = listOf(
        if (seqNum < 1) "" else seqNum,
        registrationDate,
        stage,
        reqSchedStr(),
        completionDate,
        category,
        siteName,
        assignee,
        requestDetails,
        processDetails,
        notes
    )

    companion object {
        fun fromRow(rowIndex: Int, row: List<Any?>): TechSupport {
            // D열(index 3) = "요청일 / 예정일" 또는 "요청일" 파싱
            val reqSched = row.getOrNull(3)?.toString() ?: ""
            val parts = reqSched.split(" / ", limit = 2)
            val requestDate = parts.getOrNull(0)?.trim()?.removePrefix("/")?.trim() ?: ""
            val scheduledDate = parts.getOrNull(1)?.trim() ?: ""

            return TechSupport(
                rowIndex = rowIndex,
                sequenceNumber = row.getOrNull(0)?.toString()?.toIntOrNull() ?: (rowIndex - 3),
                registrationDate = row.getOrNull(1)?.toString() ?: "",
                stage = row.getOrNull(2)?.toString() ?: Stage.RECEIVED.displayName,
                requestDate = requestDate,
                scheduledDate = scheduledDate,
                completionDate = row.getOrNull(4)?.toString() ?: "",
                category = row.getOrNull(5)?.toString() ?: Category.TECH_SUPPORT.displayName,
                siteName = row.getOrNull(6)?.toString() ?: "",
                assignee = row.getOrNull(7)?.toString() ?: "",
                requestDetails = row.getOrNull(8)?.toString() ?: "",
                processDetails = row.getOrNull(9)?.toString() ?: "",
                notes = row.getOrNull(10)?.toString() ?: ""
            )
        }

        val HEADER = listOf(
            "순번", "등록일", "진행단계", "요청/예정일", "완료일",
            "구분", "현장명", "담당", "요청사항", "처리내역", "비고"
        )
    }
}
