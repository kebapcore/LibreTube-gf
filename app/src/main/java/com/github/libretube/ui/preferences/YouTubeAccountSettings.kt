package com.github.libretube.ui.preferences

import android.os.Bundle
import androidx.preference.Preference
import com.github.libretube.R
import com.github.libretube.ui.base.BasePreferenceFragment
import com.github.libretube.ui.dialogs.YouTubeDeviceCodeDialog
import com.github.libretube.youtube.YouTubeAuthManager

class YouTubeAccountSettings : BasePreferenceFragment() {
    private var refreshUi: (() -> Unit)? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.youtube_account_settings, rootKey)

        val statusPref = findPreference<Preference>("yt_status")
        val signInPref = findPreference<Preference>("yt_sign_in")
        val signOutPref = findPreference<Preference>("yt_sign_out")

        fun doRefreshUi() {
            val signedIn = YouTubeAuthManager.isSignedIn()
            statusPref?.summary = if (signedIn) {
                getString(R.string.account_status_signed_in)
            } else {
                getString(R.string.account_status_signed_out)
            }
            signInPref?.isVisible = !signedIn
            signOutPref?.isVisible = signedIn
        }
        refreshUi = ::doRefreshUi

        signInPref?.setOnPreferenceClickListener {
            YouTubeDeviceCodeDialog().show(childFragmentManager, "yt_device_code")
            true
        }

        signOutPref?.setOnPreferenceClickListener {
            YouTubeAuthManager.signOut()
            doRefreshUi()
            true
        }

        doRefreshUi()
    }

    override fun onResume() {
        super.onResume()
        refreshUi?.invoke()
    }
}

