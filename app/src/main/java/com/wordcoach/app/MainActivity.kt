package com.wordcoach.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wordcoach.app.ui.MainViewModel
import com.wordcoach.app.ui.WordCoachScreen
import com.wordcoach.app.ui.theme.WordCoachTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WordCoachTheme {
                val vm: MainViewModel = viewModel()

                // Permission launcher: start listening once granted.
                val micPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    if (granted) vm.startListening() else vm.onPermissionDenied()
                }

                WordCoachScreen(
                    viewModel = vm,
                    onMicPressed = {
                        val granted = ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        if (granted) {
                            vm.startListening()
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                )
            }
        }
    }
}
