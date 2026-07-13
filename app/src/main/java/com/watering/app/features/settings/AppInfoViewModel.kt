package com.watering.app.features.settings

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watering.app.core.service.ReviewService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppInfoViewModel @Inject constructor(
    private val reviewService: ReviewService
) : ViewModel() {

    fun requestManualReview(activity: Activity) {
        viewModelScope.launch { reviewService.requestManualReview(activity) }
    }
}
