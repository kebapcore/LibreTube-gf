package com.github.libretube.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.github.libretube.R
import com.github.libretube.databinding.DialogYoutubeDeviceCodeBinding
import com.github.libretube.helpers.ClipboardHelper
import com.github.libretube.youtube.YouTubeAuthManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class YouTubeDeviceCodeDialog : DialogFragment() {
    private var _binding: DialogYoutubeDeviceCodeBinding? = null
    private val binding get() = _binding!!

    private var job: Job? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogYoutubeDeviceCodeBinding.inflate(LayoutInflater.from(requireContext()))

        binding.ytStatus.text = getString(R.string.account_status_loading)

        binding.ytCopy.setOnClickListener {
            ClipboardHelper.save(requireContext(), binding.ytCode.text.toString())
        }

        job = lifecycleScope.launch {
            runCatching {
                val device = YouTubeAuthManager.startDeviceFlow()
                // UX: keep it familiar (SmartTube style)
                val activateUrl = "https://youtube.com/activate"
                binding.ytUrl.text = getString(R.string.account_activate_url, activateUrl)
                binding.ytCode.text = device.userCode
                binding.ytStatus.text = getString(R.string.account_waiting_for_activation)

                val res = YouTubeAuthManager.completeDeviceFlow(
                    deviceCode = device.deviceCode,
                    pollingIntervalSeconds = device.interval,
                    expiresInSeconds = device.expiresInSeconds,
                    onStatus = { status ->
                        binding.ytStatus.text = getString(R.string.account_polling_status, status)
                    }
                )

                if (!res.accessToken.isNullOrBlank()) {
                    binding.ytStatus.text = getString(R.string.account_signed_in)
                    dismissAllowingStateLoss()
                } else {
                    binding.ytStatus.text = getString(
                        R.string.account_sign_in_failed,
                        res.error ?: "unknown"
                    )
                }
            }.onFailure { ex ->
                binding.ytStatus.text = ex.message ?: getString(R.string.unknown_error)
            }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.account_sign_in)
            .setView(binding.root)
            .setNegativeButton(R.string.cancel) { _, _ -> dismissAllowingStateLoss() }
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        job?.cancel()
        _binding = null
    }
}

