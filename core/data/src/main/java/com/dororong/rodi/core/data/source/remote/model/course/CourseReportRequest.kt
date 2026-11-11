package com.dororong.rodi.core.data.source.remote.model.course

import kotlinx.serialization.Serializable

@Serializable
data class CourseReportRequest(
    val courseIssues: List<String>,
    val detail: String? = null,
)
