package com.pemalang.roaddamage.ui.screens

import androidx.lifecycle.ViewModel
import com.pemalang.roaddamage.data.prefs.UserPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val prefs: UserPrefs
) : ViewModel() {
    suspend fun isOnboardingCompleted(): Boolean {
        return prefs.isOnboardingCompleted()
    }
}
