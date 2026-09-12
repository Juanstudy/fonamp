package com.fonamp.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Slice A scaffold: Hilt entry point. Navigation + bindings land in Slice I. */
@HiltAndroidApp
class FonampApp : Application()
