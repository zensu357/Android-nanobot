package com.example.nanobot.navigation

import androidx.lifecycle.ViewModel
import com.example.nanobot.core.preferences.OnboardingStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

@HiltViewModel
class AppNavGraphViewModel @Inject constructor(
    onboardingStore: OnboardingStore
) : ViewModel() {
    val isOnboardingCompleted: Flow<Boolean> = onboardingStore.isCompleted
}
