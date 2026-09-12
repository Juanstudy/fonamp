package com.fonamp.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text

/** Slice A scaffold: proves Compose + M3 compile in :app. Real NavHost lands in Slice I. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Text("Fonamp scaffold") }
    }
}
