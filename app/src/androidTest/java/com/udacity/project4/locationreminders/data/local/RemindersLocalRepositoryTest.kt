package com.udacity.project4.locationreminders.data.local

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var application: Application
    private lateinit var database: RemindersDatabase
    private lateinit var reminderDTO: ReminderDTO
    private lateinit var remindersLocalRepository: RemindersLocalRepository

    @Before
    fun setupRepository() {
        application = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(application, RemindersDatabase::class.java)
            .allowMainThreadQueries().build()

        reminderDTO = ReminderDTO("gym", "work out", "Abuja", 29.972802910004308, 30.939311794264963)
        remindersLocalRepository = RemindersLocalRepository(database.reminderDao(), Dispatchers.Main)
    }



    @Test
    fun InsertingReminder_Equal() = runBlocking {
        /** GIVEN: a reminder */
        val reminder = reminderDTO
        /** WHEN: saving a reminder */
        remindersLocalRepository.saveReminder(reminder)
        val reminder_dto: Result.Success<ReminderDTO> = remindersLocalRepository.getReminder(reminder.id) as Result.Success
        /** THEN: it should equal the retrieved reminder */
        assertThat(reminder_dto.data, `is`(reminder))
    }

    @Test
    fun DeletingReminders_Error() = runBlocking {
        /** GIVEN: a saved reminder */
        val reminder = reminderDTO
        remindersLocalRepository.saveReminder(reminder)
        /** WHEN: deleting all reminders */
        remindersLocalRepository.deleteAllReminders()
        val result = remindersLocalRepository.getReminder(reminder.id) as Result.Error
        /** THEN: Check for the errors */
        assertThat(result.message, `is`("Reminder not found!"))
    }
}