package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var application: Application
    private lateinit var repository: ReminderDataSource

    @Before
    fun initRepository() {
        stopKoin() // stop the original app koin
        application = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    application,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    application,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(application) }
        }
        // declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        // Get our real repository
        repository = GlobalContext.get().koin.get()

        // clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @After
    fun stopKoinAfterTest() = stopKoin()

    /** Fist Test: Navigating to reminderlist from select location fragment  */
    @Test
    fun ClickOnAddReminder_NavigatesToSaveReminderScreen() {

        /** GIVEN: Reminder List Fragment  */
        val activityScenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val navController = mock(NavController::class.java)
        activityScenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        /** WHEN: Clicking on the FAB  */
        onView(withId(R.id.addReminderFAB)).perform(click())

        /** WHEN: Navigating to SaveReminder Fragment  */
        verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder())
    }

    /** Fist Test: Navigating to reminderlist from select location fragment  */
    @Test
    fun RemindersInDatabase_ShowedByUI() {
        /** GIVEN: ReminderList Fragment with 2 Reminders saved  */
        runBlocking {
            repository.saveReminder(ReminderDTO("testTitle_1", "test_description", "testLocation1", 99.9, 99.9))
            repository.saveReminder(ReminderDTO("testTitle_2", "test_description", "testLocation2", 88.8, 88.8))
        }
        val activityScenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        /** THEN: UI Shows both reminders  */
        onView(ViewMatchers.withText("testTitle_1")).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        onView(ViewMatchers.withText("testTitle_1")).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun RemindersInDatabaseAreEMPTY_ShowedByUI() {
        /** GIVE: An empty Database of reminders  */
        runBlocking {
            repository.deleteAllReminders()
        }
        val activityScenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        /** GIVE: UI shows the noData  */
        onView(withId(R.id.noDataTextView)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
}