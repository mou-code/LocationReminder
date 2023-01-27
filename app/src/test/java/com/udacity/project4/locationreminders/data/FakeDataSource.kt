package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

class FakeDataSource : ReminderDataSource {

    private val reminder_List: LinkedHashMap<String, ReminderDTO> = LinkedHashMap()

    private var ReturnsError = false
    fun setReturnsError(value: Boolean) {
        ReturnsError = value
    }

    /**
     * Get the reminders list from the local db
     * @return Result the holds a Success with all the reminders or an Error object with the error message
     */
    override suspend fun getReminders(): Result<List<ReminderDTO>> {

            if (ReturnsError) {
                return Result.Error("Error retrieving reminders")
            }

        return try {
            Result.Success(reminder_List.values.toList())
        } catch (ex: Exception) {
            Result.Error("Error retrieving reminders")
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        /** Implementation for save Reminders  */
        reminder_List[reminder.id] = reminder
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        /** Implementation for get Reminder  */
        if (ReturnsError) return Result.Error("ERROR: Failed to get Reminder")
        val reminder = reminder_List[id]
        if (reminder != null) {
            return Result.Success(reminder)
        }
        return Result.Error("Reminder not found!")

    }

    override suspend fun deleteAllReminders() {
        /** Implementation for deleteAll Reminders  */
        reminder_List.clear()
    }

}