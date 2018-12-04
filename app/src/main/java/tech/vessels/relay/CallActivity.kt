package tech.vessels.relay

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.telecom.Call
import android.widget.Toast
import androidx.core.view.isVisible
import com.github.kittinunf.fuel.Fuel
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.activity_call.*
import java.util.concurrent.TimeUnit

class CallActivity : AppCompatActivity() {

    private val disposables = CompositeDisposable()

    private lateinit var remoteConfig: FirebaseRemoteConfig

    private lateinit var number: String
    private var callCount: Int = 0

    private lateinit var urlString: String
    private lateinit var triggerUrlString: String
    private lateinit var botId: String
    private var waitTime: Double = 10.00

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        remoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build()
        remoteConfig.setConfigSettings(configSettings)
        remoteConfig.setDefaults(R.xml.remote_config_defaults)

        number = intent.data.schemeSpecificPart
        //TODO: load these from remote config
//        urlString = "https://lwilld3.localtunnel.me/tz-phone-book-dev/us-central1/twiml/triggerCallFromRelay?temporaryInsecureAuthKey=xP6mXwOpuJTYzs2Enxi"
//        triggerUrlString = "https://us-central1-tz-phone-book-dev.cloudfunctions.net/twiml/entrypoint"
//        botId = "voicebook"
//        waitTime = 10

//        callCount = 0 //TODO: get from saved data somwhere

        updateRemoteConfig()
        loadValues()
    }

    private fun updateRemoteConfig() {
        val isUsingDeveloperMode = remoteConfig.info.configSettings.isDeveloperModeEnabled
        val cacheExpiration: Long = if (isUsingDeveloperMode) {
            0
        } else {
            3600 // 1 hour in seconds.
        }

        remoteConfig.fetch(cacheExpiration)
        .addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Fetched new config", Toast.LENGTH_SHORT).show()
                remoteConfig.activateFetched()
            } else {
                Toast.makeText(this, "Fetch Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadValues() {
        println("Loading new values")

        urlString = remoteConfig.getString(URL_STRING)
        triggerUrlString = remoteConfig.getString(TRIGGER_URL_STRING)
        botId = remoteConfig.getString(BOT_ID)
        waitTime = remoteConfig.getDouble(WAIT_TIME)
    }

    override fun onStart() {
        super.onStart()

        answer.setOnClickListener {
            OngoingCall.answer()
        }

        hangup.setOnClickListener {
            OngoingCall.hangup()
        }

        OngoingCall.state
            .subscribe(::updateUi)
            .addTo(disposables)

        OngoingCall.state
            .filter { it == Call.STATE_RINGING }
            .subscribe{hangupCall()}
            .addTo(disposables)

        OngoingCall.state
            .filter { it == Call.STATE_DISCONNECTED }
            .delay(1, TimeUnit.SECONDS)
            .firstElement()
            .subscribe { finish() }
            .addTo(disposables)
    }

    @SuppressLint("SetTextI18n")
    private fun updateUi(state: Int) {
        callInfo.text = "${state.asString().toLowerCase().capitalize()}\n$number"

        answer.isVisible = state == Call.STATE_RINGING
        hangup.isVisible = state in listOf(
            Call.STATE_DIALING,
            Call.STATE_RINGING,
            Call.STATE_ACTIVE
        )
    }

    fun hangupCall() {
        callCount += 1
        println("Hanging up call from $number")
        println("Callcount: $callCount")
        OngoingCall.hangup()

        this.sendHttpPost()
    }

    /**
     * Send a POST about the missed call
     *
     * POST
     *
     * Body: {
     *      "botId": "voicebook,
            "unformattedMobile": "+61410237238",
            "url": "https://us-central1-tz-phone-book-dev.cloudfunctions.net/twiml/entrypoint",
            "wait": 30
        }
     */
    fun sendHttpPost() {
        val body = "{\"unformattedMobile\":\"$number\",\n \"url\":\"$triggerUrlString\",\n\"wait\": $waitTime,\n \"botId\":\"$botId\"}";
        println("sending http post to url $urlString")
        println("body is $body")
        Fuel.post(urlString)
            .timeout(60000)
            .jsonBody(body)
            .responseString { request, response, result ->
            result.fold({ d ->
                //TODO: toast
                println("Success: $d")
            }, { err ->
                //TODO: toast
                println("Error with request: $err")
            })
        }
    }

    override fun onStop() {
        super.onStop()
        disposables.clear()
    }

    companion object {
        fun start(context: Context, call: Call) {
            Intent(context, CallActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setData(call.details.handle)
                .let(context::startActivity)
        }

        private const val URL_STRING = "url_string"
        private const val TRIGGER_URL_STRING = "trigger_url_string"
        private const val BOT_ID = "bot_id"
        private const val WAIT_TIME = "wait_time"
    }

}
