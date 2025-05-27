package org.aryamahasangh.repository

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Error
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.aryamahasangh.AddOrganisationActivityMutation
import org.aryamahasangh.DeleteActivityMutation
import org.aryamahasangh.OrganisationalActivitiesQuery
import org.aryamahasangh.OrganisationalActivityDetailQuery
import org.aryamahasangh.OrganisationsAndMembersQuery
import org.aryamahasangh.type.OrganisationActivityInput
import org.aryamahasangh.util.Result
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ActivityRepositoryTest {

    private val apolloClient = mockk<ApolloClient>()
    private val repository = ActivityRepositoryImpl(apolloClient)

    @Test
    fun `getActivities returns success when API call succeeds`() = runTest {
        // Given
        val mockActivities = listOf(
            mockk<OrganisationalActivitiesQuery.OrganisationalActivity>()
        )
        val mockData = mockk<OrganisationalActivitiesQuery.Data> {
            every { organisationalActivities } returns mockActivities
        }
        val mockResponse = mockk<ApolloResponse<OrganisationalActivitiesQuery.Data>> {
            every { hasErrors() } returns false
            every { data } returns mockData
            every { errors } returns null
        }
        
        coEvery { apolloClient.query(any<OrganisationalActivitiesQuery>()).execute() } returns mockResponse

        // When
        val result = repository.getActivities().first()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockActivities, result.getOrNull())
    }

    @Test
    fun `getActivities returns error when API call fails`() = runTest {
        // Given
        val errorMessage = "Network error"
        val mockError = Error.Builder(message = errorMessage).build()
        val mockResponse = mockk<ApolloResponse<OrganisationalActivitiesQuery.Data>> {
            every { hasErrors() } returns true
            every { errors } returns listOf(mockError)
            every { data } returns null
        }
        
        coEvery { apolloClient.query(any<OrganisationalActivitiesQuery>()).execute() } returns mockResponse

        // When
        val result = repository.getActivities().first()

        // Then
        assertTrue(result.isError)
        assertEquals(errorMessage, result.errorOrNull())
    }

    @Test
    fun `getActivities returns empty list when data is null`() = runTest {
        // Given
        val mockResponse = mockk<ApolloResponse<OrganisationalActivitiesQuery.Data>> {
            every { hasErrors() } returns false
            every { data } returns null
            every { errors } returns null
        }
        
        coEvery { apolloClient.query(any<OrganisationalActivitiesQuery>()).execute() } returns mockResponse

        // When
        val result = repository.getActivities().first()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(emptyList(), result.getOrNull())
    }

    @Test
    fun `getActivityDetail returns success when API call succeeds`() = runTest {
        // Given
        val activityId = "test-id"
        val mockActivity = mockk<OrganisationalActivityDetailQuery.OrganisationalActivity>()
        val mockData = mockk<OrganisationalActivityDetailQuery.Data> {
            every { organisationalActivity } returns mockActivity
        }
        val mockResponse = mockk<ApolloResponse<OrganisationalActivityDetailQuery.Data>> {
            every { hasErrors() } returns false
            every { data } returns mockData
            every { errors } returns null
        }
        
        coEvery { apolloClient.query(any<OrganisationalActivityDetailQuery>()).execute() } returns mockResponse

        // When
        val result = repository.getActivityDetail(activityId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockActivity, result.getOrNull())
    }

    @Test
    fun `getActivityDetail returns error when activity not found`() = runTest {
        // Given
        val activityId = "non-existent-id"
        val mockData = mockk<OrganisationalActivityDetailQuery.Data> {
            every { organisationalActivity } returns null
        }
        val mockResponse = mockk<ApolloResponse<OrganisationalActivityDetailQuery.Data>> {
            every { hasErrors() } returns false
            every { data } returns mockData
            every { errors } returns null
        }
        
        coEvery { apolloClient.query(any<OrganisationalActivityDetailQuery>()).execute() } returns mockResponse

        // When
        val result = repository.getActivityDetail(activityId)

        // Then
        assertTrue(result.isError)
        assertEquals("Activity not found", result.errorOrNull())
    }

    @Test
    fun `deleteActivity returns success when API call succeeds`() = runTest {
        // Given
        val activityId = "test-id"
        val mockResponse = mockk<ApolloResponse<DeleteActivityMutation.Data>> {
            every { hasErrors() } returns false
            every { data } returns mockk()
            every { errors } returns null
        }
        
        coEvery { apolloClient.mutation(any<DeleteActivityMutation>()).execute() } returns mockResponse

        // When
        val result = repository.deleteActivity(activityId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrNull())
    }

    @Test
    fun `deleteActivity returns error when API call fails`() = runTest {
        // Given
        val activityId = "test-id"
        val errorMessage = "Delete failed"
        val mockError = Error.Builder(message = errorMessage).build()
        val mockResponse = mockk<ApolloResponse<DeleteActivityMutation.Data>> {
            every { hasErrors() } returns true
            every { errors } returns listOf(mockError)
            every { data } returns null
        }
        
        coEvery { apolloClient.mutation(any<DeleteActivityMutation>()).execute() } returns mockResponse

        // When
        val result = repository.deleteActivity(activityId)

        // Then
        assertTrue(result.isError)
        assertEquals(errorMessage, result.errorOrNull())
    }

    @Test
    fun `createActivity returns success when API call succeeds`() = runTest {
        // Given
        val input = mockk<OrganisationActivityInput>()
        val mockResponse = mockk<ApolloResponse<AddOrganisationActivityMutation.Data>> {
            every { hasErrors() } returns false
            every { data } returns mockk()
            every { errors } returns null
        }
        
        coEvery { apolloClient.mutation(any<AddOrganisationActivityMutation>()).execute() } returns mockResponse

        // When
        val result = repository.createActivity(input)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrNull())
    }

    @Test
    fun `createActivity returns error when API call fails`() = runTest {
        // Given
        val input = mockk<OrganisationActivityInput>()
        val errorMessage = "Creation failed"
        val mockError = Error.Builder(message = errorMessage).build()
        val mockResponse = mockk<ApolloResponse<AddOrganisationActivityMutation.Data>> {
            every { hasErrors() } returns true
            every { errors } returns listOf(mockError)
            every { data } returns null
        }
        
        coEvery { apolloClient.mutation(any<AddOrganisationActivityMutation>()).execute() } returns mockResponse

        // When
        val result = repository.createActivity(input)

        // Then
        assertTrue(result.isError)
        assertEquals(errorMessage, result.errorOrNull())
    }

    @Test
    fun `getOrganisationsAndMembers returns success when API call succeeds`() = runTest {
        // Given
        val mockData = mockk<OrganisationsAndMembersQuery.Data>()
        val mockResponse = mockk<ApolloResponse<OrganisationsAndMembersQuery.Data>> {
            every { hasErrors() } returns false
            every { data } returns mockData
            every { errors } returns null
        }
        
        coEvery { apolloClient.query(any<OrganisationsAndMembersQuery>()).execute() } returns mockResponse

        // When
        val result = repository.getOrganisationsAndMembers()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockData, result.getOrNull())
    }

    @Test
    fun `getOrganisationsAndMembers returns error when data is null`() = runTest {
        // Given
        val mockResponse = mockk<ApolloResponse<OrganisationsAndMembersQuery.Data>> {
            every { hasErrors() } returns false
            every { data } returns null
            every { errors } returns null
        }
        
        coEvery { apolloClient.query(any<OrganisationsAndMembersQuery>()).execute() } returns mockResponse

        // When
        val result = repository.getOrganisationsAndMembers()

        // Then
        assertTrue(result.isError)
        assertEquals("No data returned", result.errorOrNull())
    }
}