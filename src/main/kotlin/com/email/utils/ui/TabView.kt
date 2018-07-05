package com.email.utils.ui

import android.view.View

abstract class TabView(val view: View, val title: String) {

    init {
        onCreateView()
    }

    abstract fun onCreateView()
}