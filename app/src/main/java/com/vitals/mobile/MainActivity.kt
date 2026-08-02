package com.vitals.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.vitals.mobile.core.designsystem.VitalsAppTheme
import com.vitals.mobile.core.navigation.VitalsApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VitalsAppTheme {
                VitalsApp()
            }
        }
    }
}
