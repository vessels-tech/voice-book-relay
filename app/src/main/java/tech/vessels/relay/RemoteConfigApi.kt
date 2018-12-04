package tech.vessels.relay

import android.app.Activity
import android.widget.Toast
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import timber.log.Timber

object RemoteConfigApi {

    const val URL_STRING = "url_string"
    const val TRIGGER_URL_STRING = "trigger_url_string"
    const val BOT_ID = "bot_id"
    const val WAIT_TIME = "wait_time"


    fun getRemoteConfig(): FirebaseRemoteConfig {
        var remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build()
        remoteConfig.setConfigSettings(configSettings)
        remoteConfig.setDefaults(R.xml.remote_config_defaults)

        return remoteConfig
    }

    fun updateRemoteConfig(ctx: Activity,  remoteConfig: FirebaseRemoteConfig) {
        val isUsingDeveloperMode = remoteConfig.info.configSettings.isDeveloperModeEnabled
        val cacheExpiration: Long = if (isUsingDeveloperMode) {
            0
        } else {
            3600 // 1 hour in seconds.
        }

        remoteConfig.fetch(cacheExpiration)
        .addOnCompleteListener(ctx) { task ->
            if (task.isSuccessful) {
                Timber.d("Fetched new config");
//                Toast.makeText(ctx, "Fetched new config", Toast.LENGTH_SHORT).show()
                remoteConfig.activateFetched()
            } else {
                Timber.d("Fetch failed");
                Toast.makeText(ctx, "Fetch Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
}