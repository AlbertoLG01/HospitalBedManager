package com.example.hospitalbedmanager.ui.porter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PorterViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is porter Fragment"
    }
    val text: LiveData<String> = _text
}