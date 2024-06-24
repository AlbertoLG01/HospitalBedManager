package com.example.hospitalbedmanager.dataclasses

import com.google.firebase.Timestamp

data class Bed(
    val number: Int = -1,
    val patientAssociated: String = "",
    val consultationAssociated: Int = -1,
    var isOccupied: Boolean = false,
    val assignmentDate: Timestamp = Timestamp.now()
)
