package com.aryamahasangh.features.about_us.domain.usecase

import com.aryamahasangh.features.about_us.domain.repository.AboutUsRepository
import com.aryamahasangh.features.about_us.domain.repository.OrganisationName
import com.aryamahasangh.util.Result
import kotlinx.coroutines.flow.Flow

class GetOrganisationNamesUseCase(
  private val repository: AboutUsRepository
) {
  operator fun invoke(): Flow<Result<List<OrganisationName>>> =
    repository.getOrganisationNames()
}
