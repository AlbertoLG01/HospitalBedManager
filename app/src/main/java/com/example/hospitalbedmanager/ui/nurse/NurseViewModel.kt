package com.example.hospitalbedmanager.ui.nurse

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class NurseViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is nurse Fragment"
    }
    val text: LiveData<String> = _text
}