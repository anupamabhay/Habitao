package com.habitao.domain.usecase

import com.habitao.domain.model.Placeholder
import com.habitao.domain.repository.PlaceholderRepository

/**
 * Placeholder use case for initial setup.
 * Will be replaced with actual use cases.
 */
class GetPlaceholderUseCase(
    private val repository: PlaceholderRepository
) {
    suspend operator fun invoke(id: String): Placeholder? {
        return repository.getPlaceholder(id)
    }
}
