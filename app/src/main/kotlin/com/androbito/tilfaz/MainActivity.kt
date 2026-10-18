package com.androbito.tilfaz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.androbito.tilfaz.core.designsystem.component.LoadingState
import com.androbito.tilfaz.core.designsystem.component.backKeyForDispatcherOnly
import com.androbito.tilfaz.core.designsystem.theme.TilfazTheme
import com.androbito.tilfaz.navigation.TilfazNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TilfazTheme {
                val session by viewModel.initialSession.collectAsStateWithLifecycle()
                Box(Modifier.fillMaxSize().backKeyForDispatcherOnly()) {
                    if (session == MainViewModel.Session.Loading) {
                        LoadingState()
                    } else {
                        TilfazNavHost(startSignedIn = session == MainViewModel.Session.SignedIn)
                    }
                }
            }
        }
    }
}
