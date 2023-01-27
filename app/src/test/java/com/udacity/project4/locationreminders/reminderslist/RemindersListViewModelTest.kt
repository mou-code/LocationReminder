package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.google.firebase.FirebaseApp
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    /** Setting up the test for the view model  */
    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var dataSource: FakeDataSource

    private lateinit var remindersListViewModel: RemindersListViewModel

    @Before
    fun setupViewModel() {
        dataSource = FakeDataSource()
        remindersListViewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), dataSource)
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    /** First Test: Check for loading  */
    @Test
    fun check_loading() = mainCoroutineRule.runBlockingTest {
        /** GIVEN: */
        mainCoroutineRule.pauseDispatcher()

        /** WHEN: loading the reminders  */

        remindersListViewModel.loadReminders()

        /** THEN: Check if it's loading  */
        assertThat(
            remindersListViewModel.showLoading.getOrAwaitValue(), `is`(true)
        )

        mainCoroutineRule.resumeDispatcher()

        assertThat(
            remindersListViewModel.showLoading.getOrAwaitValue(), `is`(false)
        )
    }

    /** Second Test: Check for Errors  */
    @Test
    fun shouldReturnError() = mainCoroutineRule.runBlockingTest {
        /** GIVEN: ReturnError=true  */
        dataSource.setReturnsError(true)
        /** WHEN: loading reminders  */
        remindersListViewModel.loadReminders()

        /** THEN: Check if it's the same value  */

        // + Show error message in SnackBar
        MatcherAssert.assertThat(remindersListViewModel.showSnackBar.getOrAwaitValue(), `is`("Error retrieving reminders"))

        // + showNoData is true
        MatcherAssert.assertThat(remindersListViewModel.showNoData.getOrAwaitValue(), `is`(true))
    }
}