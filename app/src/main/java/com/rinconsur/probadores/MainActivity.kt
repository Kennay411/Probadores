package com.rinconsur.probadores

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.rinconsur.probadores.navigation.AppNavigation
import com.rinconsur.probadores.ui.theme.RinconSurTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RinconSurTheme {
                AppNavigation()
            }
        }
    }
}
