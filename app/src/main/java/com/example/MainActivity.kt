package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.PhotoMasterScreen
import com.example.ui.theme.PhotoMasterTheme
import com.example.viewmodel.PhotoMasterViewModel

class MainActivity : ComponentActivity() {
  private val viewModel: PhotoMasterViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      PhotoMasterTheme {
        PhotoMasterScreen(viewModel = viewModel)
      }
    }
  }
}

