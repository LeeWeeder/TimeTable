package com.leeweeder.timetable.util

import kotlinx.serialization.Serializable

sealed interface Destination {

    sealed interface Dialog : Destination {

        @Serializable
        data class TimeTableSetupDialog(
            val timeTableName: String,
            val isInitialization: Boolean,
            val selectedTimeTableId: Int
        ) : Dialog

        @Serializable
        data class GetTimeTableNameDialog(
            val isInitialization: Boolean = false,
            val selectedTimeTableId: Int
        ) : Dialog
    }

    sealed interface Screen : Destination {

        @Serializable
        data class HomeScreen(val subjectIdToBeEdited: Int? = null, val selectedTimeTableId: Int) :
            Screen

        @Serializable
        data class SubjectsScreen(val timeTableId: Int) : Screen
    }
}

