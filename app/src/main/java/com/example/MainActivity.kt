package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.data.database.AppDatabase
import com.example.data.repository.SessionRepository
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup local edge-to-edge drawing
        enableEdgeToEdge()

        // Initialize Local Database & Repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = SessionRepository(database.sessionDao())

        // Access private app Shared Preferences to persist user API Keys securely
        val sharedPrefs = getSharedPreferences("doan_coordinator_prefs", Context.MODE_PRIVATE)

        // Bind custom ViewModel with constructor dependencies injection
        val viewModel: MainViewModel by viewModels {
            MainViewModelFactory(repository, sharedPrefs)
        }

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
