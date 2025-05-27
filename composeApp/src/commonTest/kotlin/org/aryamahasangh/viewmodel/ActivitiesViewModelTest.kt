package org.aryamahasangh.viewmodel

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.aryamahasangh.OrganisationalActivitiesQuery
import org.aryamahasangh.OrganisationalActivityDetailQuery
import org.aryamahasangh.OrganisationsAndMembersQuery
import org.aryamahasangh.repository.ActivityRepository
import org.aryamahasangh.type.OrganisationActivityInput
import org.aryamahasangh.util.Result
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ActivitiesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val activityRepository = mockk<ActivityRepository>()
    private lateinit var viewModel: ActivitiesViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Mock the initial call in init
        every { activityRepository.getActivities() } returns flowOf(Result.Loading)
        
        viewModel = ActivitiesViewModel(activityRepository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() = runTest {
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertEquals(emptyList(), initialState.activities)
            assertFalse(initialState.isLoading)
            assertNull(initialState.error)
        }
    }

    @Test
    fun `loadActivities updates state correctly on success`() = runTest {
        // Given
        val mockActivities = listOf(
            mockk<OrganisationalActivitiesQuery.OrganisationalActivity> {
                every { id } returns "1"
            }
        )
        every { activityRepository.getActivities() } returns flowOf(
            Result.Loading,
            Result.Success(mockActivities)
        )

        // When
        viewModel.loadActivities()

        // Then
        viewModel.uiState.test {
            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)
            assertNull(loadingState.error)

            val successState = awaitItem()
            assertEquals(mockActivities, successState.activities)
            assertFalse(successState.isLoading)
            assertNull(successState.error)
        }
    }

    @Test
    fun `loadActivities updates state correctly on error`() = runTest {
        // Given
        val errorMessage = "Network error"
        every { activityRepository.getActivities() } returns flowOf(
            Result.Loading,
            Result.Error(errorMessage)
        )

        // When
        viewModel.loadActivities()

        // Then
        viewModel.uiState.test {
            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)

            val errorState = awaitItem()
            assertFalse(errorState.isLoading)
            assertEquals(errorMessage, errorState.error)
            assertEquals(emptyList(), errorState.activities)
        }
    }

    @Test
    fun `deleteActivity removes activity from list on success`() = runTest {
        // Given
        val activityId = "1"
        val mockActivities = listOf(
            mockk<OrganisationalActivitiesQuery.OrganisationalActivity> {
                every { id } returns activityId
            },
            mockk<OrganisationalActivitiesQuery.OrganisationalActivity> {
                every { id } returns "2"
            }
        )
        
        // Set initial state with activities
        every { activityRepository.getActivities() } returns flowOf(Result.Success(mockActivities))
        viewModel.loadActivities()
        
        coEvery { activityRepository.deleteActivity(activityId) } returns Result.Success(true)

        // When
        viewModel.deleteActivity(activityId)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.activities.size)
            assertEquals("2", state.activities.first().id)
            assertNull(state.error)
        }
    }

    @Test
    fun `deleteActivity updates error state on failure`() = runTest {
        // Given
        val activityId = "1"
        val errorMessage = "Delete failed"
        coEvery { activityRepository.deleteActivity(activityId) } returns Result.Error(errorMessage)

        // When
        viewModel.deleteActivity(activityId)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(errorMessage, state.error)
        }
    }

    @Test
    fun `loadActivityDetail updates detail state correctly on success`() = runTest {
        // Given
        val activityId = "1"
        val mockActivity = mockk<OrganisationalActivityDetailQuery.OrganisationalActivity>()
        coEvery { activityRepository.getActivityDetail(activityId) } returns Result.Success(mockActivity)

        // When
        viewModel.loadActivityDetail(activityId)

        // Then
        viewModel.activityDetailUiState.test {
            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)
            assertNull(loadingState.activity)

            val successState = awaitItem()
            assertEquals(mockActivity, successState.activity)
            assertFalse(successState.isLoading)
            assertNull(successState.error)
        }
    }

    @Test
    fun `loadActivityDetail updates detail state correctly on error`() = runTest {
        // Given
        val activityId = "1"
        val errorMessage = "Activity not found"
        coEvery { activityRepository.getActivityDetail(activityId) } returns Result.Error(errorMessage)

        // When
        viewModel.loadActivityDetail(activityId)

        // Then
        viewModel.activityDetailUiState.test {
            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)

            val errorState = awaitItem()
            assertFalse(errorState.isLoading)
            assertEquals(errorMessage, errorState.error)
            assertNull(errorState.activity)
        }
    }

    @Test
    fun `createActivity updates form state correctly on success`() = runTest {
        // Given
        val input = mockk<OrganisationActivityInput>()
        coEvery { activityRepository.createActivity(input) } returns Result.Success(true)
        every { activityRepository.getActivities() } returns flowOf(Result.Success(emptyList()))

        // When
        viewModel.createActivity(input)

        // Then
        viewModel.activityFormSubmissionState.test {
            val submittingState = awaitItem()
            assertTrue(submittingState.isSubmitting)
            assertFalse(submittingState.isSuccess)

            val successState = awaitItem()
            assertFalse(successState.isSubmitting)
            assertTrue(successState.isSuccess)
            assertNull(successState.error)
        }

        // Verify that activities are reloaded
        verify { activityRepository.getActivities() }
    }

    @Test
    fun `createActivity updates form state correctly on error`() = runTest {
        // Given
        val input = mockk<OrganisationActivityInput>()
        val errorMessage = "Creation failed"
        coEvery { activityRepository.createActivity(input) } returns Result.Error(errorMessage)

        // When
        viewModel.createActivity(input)

        // Then
        viewModel.activityFormSubmissionState.test {
            val submittingState = awaitItem()
            assertTrue(submittingState.isSubmitting)

            val errorState = awaitItem()
            assertFalse(errorState.isSubmitting)
            assertFalse(errorState.isSuccess)
            assertEquals(errorMessage, errorState.error)
        }
    }

    @Test
    fun `resetFormSubmissionState resets form state`() = runTest {
        // Given - set some state first
        val input = mockk<OrganisationActivityInput>()
        coEvery { activityRepository.createActivity(input) } returns Result.Error("Error")
        viewModel.createActivity(input)

        // When
        viewModel.resetFormSubmissionState()

        // Then
        viewModel.activityFormSubmissionState.test {
            val resetState = awaitItem()
            assertFalse(resetState.isSubmitting)
            assertFalse(resetState.isSuccess)
            assertNull(resetState.error)
        }
    }

    @Test
    fun `loadOrganisationsAndMembers updates state correctly on success`() = runTest {
        // Given
        val mockOrganisations = listOf(mockk<OrganisationsAndMembersQuery.Organisation>())
        val mockMembers = listOf(mockk<OrganisationsAndMembersQuery.Member>())
        val mockData = mockk<OrganisationsAndMembersQuery.Data> {
            every { organisations } returns mockOrganisations
            every { members } returns mockMembers
        }
        coEvery { activityRepository.getOrganisationsAndMembers() } returns Result.Success(mockData)

        // When
        viewModel.loadOrganisationsAndMembers()

        // Then
        viewModel.organisationsAndMembersState.test {
            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)
            assertEquals(emptyList(), loadingState.organisations)
            assertEquals(emptyList(), loadingState.members)

            val successState = awaitItem()
            assertFalse(successState.isLoading)
            assertEquals(mockOrganisations, successState.organisations)
            assertEquals(mockMembers, successState.members)
            assertNull(successState.error)
        }
    }

    @Test
    fun `loadOrganisationsAndMembers updates state correctly on error`() = runTest {
        // Given
        val errorMessage = "Failed to load data"
        coEvery { activityRepository.getOrganisationsAndMembers() } returns Result.Error(errorMessage)

        // When
        viewModel.loadOrganisationsAndMembers()

        // Then
        viewModel.organisationsAndMembersState.test {
            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)

            val errorState = awaitItem()
            assertFalse(errorState.isLoading)
            assertEquals(errorMessage, errorState.error)
            assertEquals(emptyList(), errorState.organisations)
            assertEquals(emptyList(), errorState.members)
        }
    }

    @Test
    fun `loadOrganisationsAndMembers handles null data correctly`() = runTest {
        // Given
        val mockData = mockk<OrganisationsAndMembersQuery.Data> {
            every { organisations } returns null
            every { members } returns null
        }
        coEvery { activityRepository.getOrganisationsAndMembers() } returns Result.Success(mockData)

        // When
        viewModel.loadOrganisationsAndMembers()

        // Then
        viewModel.organisationsAndMembersState.test {
            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)

            val successState = awaitItem()
            assertFalse(successState.isLoading)
            assertEquals(emptyList(), successState.organisations)
            assertEquals(emptyList(), successState.members)
            assertNull(successState.error)
        }
    }
}