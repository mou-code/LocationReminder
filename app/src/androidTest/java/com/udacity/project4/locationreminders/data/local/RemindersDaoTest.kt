package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Test
import java.util.concurrent.Executors

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

/** Testing implementation for the RemindersDao (Inserting and deleting) */
    private lateinit var database: RemindersDatabase
    private lateinit var reminderDTO: ReminderDTO

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun initDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries().build()

        /** Initializing the variables used for testing */
        reminderDTO = ReminderDTO("gym", "work out", "air gym", 29.972802910004308, 30.939311794264963)

    }

    @Test
    fun InsertingReminder_Equal() = runBlocking {
        /** GIVEN: a reminder */
        val reminder = reminderDTO
        /** WHEN: saving a reminder */
        database.reminderDao().saveReminder(reminder)
        val reminder_dto: ReminderDTO? = database.reminderDao().getReminderById(reminder.id)
        /** THEN: it should equal the retrieved reminder */
        assertThat(reminder_dto, `is`(reminder))
    }

    @Test
    fun DeletingReminders_Empty() = runBlocking {
        /** GIVEN: a saved reminder */
        val reminder = reminderDTO
        database.reminderDao().saveReminder(reminder)
        /** WHEN: deleting all reminders */
        database.reminderDao().deleteAllReminders()
        val result = database.reminderDao().getReminderById(reminder.id)
        /** THEN: result should be null */
        assertThat(result, `is`(CoreMatchers.nullValue()))
    }

    @After
    fun closeDb() {
        /** closing the database after testing */
        database.close()
    }
}