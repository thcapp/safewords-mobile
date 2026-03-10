package com.thc.safewords

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.thc.safewords.ui.navigation.SafewordsNavigation
import com.thc.safewords.ui.theme.SafewordsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SafewordsTheme {
                SafewordsNavigation()
            }
        }
    }
}
