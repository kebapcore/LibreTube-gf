package com.github.libretube.ui.preferences

import android.os.Bundle
import androidx.preference.SwitchPreferenceCompat
import com.github.libretube.R
import com.github.libretube.constants.PreferenceKeys
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.ui.base.BasePreferenceFragment

class InstanceSettings : BasePreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.instance_settings, rootKey)

        findPreference<SwitchPreferenceCompat>(PreferenceKeys.FULL_LOCAL_MODE)?.setOnPreferenceChangeListener { _, newValue ->
            // when the full local mode gets enabled, the fetch instance is no longer used and replaced
            // fully by local extraction. thus, the user has to be logged out from the fetch instance
            if (newValue == true) {
                PreferenceHelper.setToken("")
            }
            true
        }
    }

    companion object {
        /**
         * Fragment result key used by login/logout/delete-account dialogs
         * to notify this screen about authentication changes.
         *
         * Kept for compatibility so existing dialogs and navigation continue
         * to compile and work in CI/GitHub Actions.
         */
        const val INSTANCE_DIALOG_REQUEST_KEY = "instance_dialog_request_key"
    }
}
