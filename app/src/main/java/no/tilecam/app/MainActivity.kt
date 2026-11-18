package no.tilecam.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import no.tilecam.app.ui.MainViewModel
import no.tilecam.app.ui.TilecamRoot
import no.tilecam.app.ui.theme.TilecamTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory((application as TilecamApp).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TilecamTheme {
                TilecamRoot(viewModel)
            }
        }
    }
}
