package com.example.cookstovecare.navigation

/**
 * Navigation routes for the app.
 * Uses type-safe route format with optional arguments.
 */
object NavRoutes {
    const val WELCOME = "welcome"
    const val AUTH = "auth"
    const val SIGN_UP = "sign_up"
    const val DASHBOARD = "dashboard"
    const val FIELD_OFFICER_DASHBOARD = "field_officer_dashboard"
    const val SUPERVISOR_DASHBOARD = "supervisor_dashboard"
    const val TECHNICIAN_DASHBOARD = "technician_dashboard"
    const val DASHBOARD_EDIT = "dashboard_edit/{taskId}"
    const val TASK_DETAIL = "task_detail/{taskId}"
    const val REPAIR_FORM = "repair_form/{taskId}"
    const val REPLACEMENT_FORM = "replacement_form/{taskId}"
    const val ADD_RETURN_FORM = "add_return_form/{taskId}"
    const val ASSIGN_TASK = "assign_task/{taskId}"
    fun assignTask(taskId: Long) = "assign_task/$taskId"
    const val SUPERVISOR_TASK_LIST = "supervisor_task_list"
    const val TECHNICIANS_LIST = "technicians_list"
    const val EDIT_TECHNICIAN = "edit_technician/{technicianId}"
    fun editTechnician(technicianId: Long) = "edit_technician/$technicianId"
    const val CREATE_TECHNICIAN = "create_technician"

    fun taskDetail(taskId: Long) = "task_detail/$taskId"
    fun dashboardEdit(taskId: Long) = "dashboard_edit/$taskId"
    fun repairForm(taskId: Long) = "repair_form/$taskId"
    fun replacementForm(taskId: Long) = "replacement_form/$taskId"
    fun addReturnForm(taskId: Long) = "add_return_form/$taskId"
}
