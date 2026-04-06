package com.ramesh.scp_project

import android.app.Application

class ScpApplication : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }
}
