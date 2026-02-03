package com.example.cookstovecare.navigation

/**
 * Navigation routes for the app.
 * Uses type-safe route format with optional arguments.
 */
object NavRoutes {
    const val DASHBOARD = "dashboard"
    const val DASHBOARD_EDIT = "dashboard_edit/{taskId}"
    const val TASK_DETAIL = "task_detail/{taskId}"
    const val REPAIR_FORM = "repair_form/{taskId}"
    const val REPLACEMENT_FORM = "replacement_form/{taskId}"

    fun taskDetail(taskId: Long) = "task_detail/$taskId"
    fun dashboardEdit(taskId: Long) = "dashboard_edit/$taskId"
    fun repairForm(taskId: Long) = "repair_form/$taskId"
    fun replacementForm(taskId: Long) = "replacement_form/$taskId"
}
