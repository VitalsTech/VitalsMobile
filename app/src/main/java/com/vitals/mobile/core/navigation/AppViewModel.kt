package com.vitals.mobile.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitals.mobile.core.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    authRepository: AuthRepository,
) : ViewModel() {

    /** `null` while the session is still being read from disk, then reflects the login state. */
    val isLoggedIn: StateFlow<Boolean?> = authRepository.sessionFlow
        .map { it.isLoggedIn }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
