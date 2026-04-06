package com.ramesh.scp_project

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramesh.scp_project.core.ui.SearchScreen

class MainActivity : ComponentActivity() {

    private val viewModel by viewModels<MainViewModel> {
        MainViewModel.factory(application)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onPermissionStateChanged(
            isGranted = granted,
            shouldShowRationale = shouldShowMediaPermissionRationale()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel.onPermissionStateChanged(
            isGranted = hasMediaPermission(),
            shouldShowRationale = shouldShowMediaPermissionRationale()
        )

        setContent {
            val uiState = viewModel.uiState.collectAsStateWithLifecycle()

            SearchScreen(
                state = uiState.value,
                onQueryChanged = viewModel::onQueryChanged,
                onSearch = viewModel::submitSearch,
                onIndexRequest = viewModel::refreshIndex,
                onGrantPermission = ::requestMediaPermission,
                onOpenSettings = ::openAppSettings,
                onDismissError = viewModel::dismissError
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onPermissionStateChanged(
            isGranted = hasMediaPermission(),
            shouldShowRationale = shouldShowMediaPermissionRationale()
        )
    }

    private fun requestMediaPermission() {
        viewModel.onPermissionRequestStarted()
        permissionLauncher.launch(mediaPermission())
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )
        startActivity(intent)
    }

    private fun hasMediaPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            mediaPermission()
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun shouldShowMediaPermissionRationale(): Boolean {
        return shouldShowRequestPermissionRationale(mediaPermission())
    }

    private fun mediaPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }
}
