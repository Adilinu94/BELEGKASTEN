package de.belegkasten.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// Entry point for Hilt dependency injection. No network client is created here
// on purpose: the app must work fully offline (see AndroidManifest: no INTERNET).
@HiltAndroidApp
class BelegkastenApp : Application()
