package com.example.cookstovecare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.cookstovecare.ui.screen.CookstoveCareNavGraph
import com.example.cookstovecare.ui.theme.CookstovecareTheme

/**
 * Main Activity - hosts the navigation graph.
 * No business logic - only composition.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CookstovecareTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CookstoveCareNavGraph()
                }
            }
        }
    }
}
