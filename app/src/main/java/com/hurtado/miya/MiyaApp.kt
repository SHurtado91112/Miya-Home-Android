package com.hurtado.miya

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class. Analogue of MiyaApp.swift's `@main App` — the SwiftUI app has no
 * AppDelegate; here Hilt needs this class to root the dependency graph.
 */
@HiltAndroidApp
class MiyaApp : Application()
