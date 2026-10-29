package dev.toufikforyou.questionbank.domain.usecase

import dev.toufikforyou.questionbank.domain.model.UserProfile
import dev.toufikforyou.questionbank.domain.repository.ProfileRepository
import dev.toufikforyou.questionbank.domain.repository.QuestionRepository

/**
 * First-launch onboarding without any login: creates the local profile and seeds the question bank.
 * Safe to call on every launch.
 */
class InitializeAppUseCase(
    private val profileRepository: ProfileRepository,
    private val questionRepository: QuestionRepository,
) {
    suspend operator fun invoke(): Result<UserProfile> = resultOf {
        val profile = profileRepository.getOrCreateProfile()
        questionRepository.ensureSeeded()
        profile
    }
}
