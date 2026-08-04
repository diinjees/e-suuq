package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppViewModel
import com.example.ui.ESuuqApp
import com.example.ui.theme.MyApplicationTheme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.example.ui.AppThemeMode
import com.example.network.PocketBaseAuth

class MainActivity : FragmentActivity() {
  
  companion object {
    lateinit var auth: PocketBaseAuth
    lateinit var context: android.content.Context
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    context = applicationContext
    auth = PocketBaseAuth(this)
    
    enableEdgeToEdge()
    setContent {
      val viewModel: AppViewModel = viewModel()
      val themeMode by viewModel.themeMode.collectAsState()
      val useDarkTheme = when (themeMode) {
        AppThemeMode.FOLLOW_SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
      }
      MyApplicationTheme(darkTheme = useDarkTheme) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          ESuuqApp(viewModel = viewModel)
        }
      }
    }
  }
}
