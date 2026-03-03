package com.github.libretube.ui.activities

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import com.github.libretube.databinding.ActivityWelcomeBinding
import com.github.libretube.ui.base.BaseActivity
import com.github.libretube.ui.models.WelcomeViewModel
import com.github.libretube.ui.preferences.BackupRestoreSettings

class WelcomeActivity : BaseActivity() {

    private val viewModel by viewModels<WelcomeViewModel> { WelcomeViewModel.Factory }

    private val restoreFilePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) return@registerForActivityResult
            viewModel.restoreAdvancedBackup(this, uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.okay.setOnClickListener {
            viewModel.onConfirmSettings()
        }

        binding.restore.setOnClickListener {
            restoreFilePicker.launch(BackupRestoreSettings.JSON)
        }

        viewModel.uiState.observe(this) { (_, _, _, _, navigateToMain) ->
            // GF Edition: always full local mode (no Piped selection)
            binding.okay.isEnabled = true
            binding.progress.visibility = android.view.View.GONE
            binding.localModeInfoContainer.visibility = android.view.View.VISIBLE

            navigateToMain?.let {
                val mainActivityIntent = Intent(this, MainActivity::class.java)
                startActivity(mainActivityIntent)
                finish()
                viewModel.onNavigated()
            }
        }
    }

    override fun requestOrientationChange() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
    }
}
