package com.habitao.app

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController(quickActionRoute: String? = null) =
    ComposeUIViewController {
        App(quickActionRoute = quickActionRoute)
    }
