package com.pemalang.roaddamage.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pemalang.roaddamage.data.prefs.UserPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: UserPrefs
) : ViewModel() {
    fun completeOnboarding(onCompleted: () -> Unit) {
        viewModelScope.launch {
            prefs.setOnboardingCompleted(true)
            onCompleted()
        }
    }
}
