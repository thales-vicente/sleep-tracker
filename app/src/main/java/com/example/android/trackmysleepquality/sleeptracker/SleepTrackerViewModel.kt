/*
 * Copyright 2018, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.trackmysleepquality.sleeptracker

import android.app.Application
import android.provider.SyncStateContract.Helpers.insert
import android.provider.SyncStateContract.Helpers.update
import android.text.method.TextKeyListener.clear
import android.view.animation.Transformation
import androidx.core.view.VelocityTrackerCompat.clear
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.formatNights
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/**
 * ViewModel for SleepTrackerFragment.
 */
class SleepTrackerViewModel(
        val database: SleepDatabaseDao,
        application: Application) : AndroidViewModel(application) {

        private var viewModelJob = Job()

        override fun onCleared() {
                super.onCleared()
                viewModelJob.cancel()
        }

        private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

        private var tonight = MutableLiveData<SleepNight?>()

        val nights = database.getAllNights()

        val nightsString = nights.map { nights ->
                formatNights(nights, application.resources)
        }
        val startButtonVisible = tonight.map {
                null == it
        }
        val stopButtonVisible = tonight.map {
                null != it
        }
        val clearButtonVisible = nights.map {
                it.isNotEmpty()
        }

        private var _showSnackBarEvent = MutableLiveData<Boolean>()

        val showSnackBarEvent: LiveData<Boolean>
                get() = _showSnackBarEvent

        fun doneShowingSnackBar() {
                _showSnackBarEvent.value = false
        }

        private val _navigateToSleepQuality = MutableLiveData<SleepNight?>()
        val navigateToSleepQuality: MutableLiveData<SleepNight?>
                get() = _navigateToSleepQuality

        fun doneNavigating() {
                _navigateToSleepQuality.value = null
        }

        init {
            initializeTonight()
        }

        private fun initializeTonight() {
                uiScope.launch {
                        tonight.value = getTonightFromDataBase()
                }
        }

        private suspend fun getTonightFromDataBase(): SleepNight?{
                return withContext(Dispatchers.IO){
                        var night = database.getTonight()
                        if (night?.endTimeMilli != night?.startTimeMilli){
                                night = null
                        }
                        night
                }
        }
        fun onStartTracking(){
                uiScope.launch {
                        val newNight = SleepNight()
                        insert(newNight)
                        tonight.value = getTonightFromDataBase()
                }
        }

        private suspend fun insert(night: SleepNight){
                withContext(Dispatchers.IO){
                        database.insert(night)
                }
        }

        fun onStopTracking(){
                uiScope.launch {
                        val oldNight = tonight.value ?: return@launch
                        oldNight.endTimeMilli = System.currentTimeMillis()
                        update(oldNight)
                        _navigateToSleepQuality.value = oldNight
                }
        }

        private suspend fun update(night: SleepNight){
                withContext(Dispatchers.IO){
                        database.update(night)
                }
        }

        fun onClear(){
                uiScope.launch {
                        clear()
                        tonight.value = null
                        _showSnackBarEvent.value = true
                }
        }

        suspend fun clear(){
                withContext(Dispatchers.IO){
                        database.clear()
                }
        }

        private val _navigateToSleepDataQuality = MutableLiveData<Long?>()
        val navigateToSleepDataQuality
                get() = _navigateToSleepDataQuality

        fun onSleepNightClicked(id: Long){
                _navigateToSleepDataQuality.value = id
        }

        fun onSleepDataQualityNavigated() {
                _navigateToSleepDataQuality.value = null
        }

}

