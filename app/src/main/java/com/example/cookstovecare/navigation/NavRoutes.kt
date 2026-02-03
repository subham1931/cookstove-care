package com.example.cookstovecare.navigation

/**
 * Navigation routes for the app.
 * Uses type-safe route format with optional arguments.
 */
object NavRoutes {
    const val DASHBOARD = "dashboard"
    const val CREATE_TASK = "create_task"
    const val TASK_ACTION_SELECTION = "task_action_selection/{taskId}"
    const val TASK_DETAIL = "task_detail/{taskId}"
    const val REPAIR_FORM = "repair_form/{taskId}"
    const val REPLACEMENT_FORM = "replacement_form/{taskId}"
    const val EDIT_TASK = "edit_task/{taskId}"

    fun taskActionSelection(taskId: Long) = "task_action_selection/$taskId"
    fun editTask(taskId: Long) = "edit_task/$taskId"
    fun taskDetail(taskId: Long) = "task_detail/$taskId"
    fun repairForm(taskId: Long) = "repair_form/$taskId"
    fun replacementForm(taskId: Long) = "replacement_form/$taskId"
}
