package tech.vessels.relay

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.telecom.Call
import android.util.Log
import android.widget.Toast
import androidx.core.view.isVisible
import com.crashlytics.android.Crashlytics
import com.github.kittinunf.fuel.Fuel
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.common.net.HttpHeaders.AUTHORIZATION
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.activity_call.*
import tech.vessels.relay.FirebaseApi.Companion.incrementCallCount
import tech.vessels.relay.RemoteConfigApi.BOT_ID
import tech.vessels.relay.RemoteConfigApi.TRIGGER_URL_STRING
import tech.vessels.relay.RemoteConfigApi.URL_STRING
import tech.vessels.relay.RemoteConfigApi.WAIT_TIME
import timber.log.Timber
import java.lang.Exception
import java.util.concurrent.TimeUnit

class CallActivity : AppCompatActivity() {

    private val disposables = CompositeDisposable()

    private lateinit var remoteConfig: FirebaseRemoteConfig

    private lateinit var number: String
    private var callCount: Int = 0

    private lateinit var userId: String
    private lateinit var urlString: String
    private lateinit var triggerUrlString: String
    private lateinit var botId: String
    private var waitTime: Double = 10.00

    override fun onCreate(savedInstanceState: Bundle?) {
        Crashlytics.log(Log.DEBUG, "CallActivity.onCreate()", "start")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        Crashlytics.log(Log.DEBUG, "CallActivity.onCreate()", "post layout")
//        Crashlytics.log(Log.DEBUG, "CallActivity.onCreate()", "intent is: " + intent.data)

        //TODO: this might be null
        try {
            Crashlytics.log(Log.DEBUG, "CallActivity.onCreate()", "intent is: " + intent)
            Crashlytics.log(Log.DEBUG, "CallActivity.onCreate()", "intent.data is: " + intent.data)
        } catch (exception: Exception) {
            Crashlytics.log(Log.DEBUG, "CallActivity.catch", exception.message)

        }

//        number = intent.data.schemeSpecificPart
        number = getIntent().getData().schemeSpecificPart;
        Crashlytics.log(Log.DEBUG, "CallActivity.onCreate()", "number is: " + number)



        remoteConfig = RemoteConfigApi.getRemoteConfig()
        RemoteConfigApi.updateRemoteConfig(this, remoteConfig)
        loadValues()
    }

    override fun onStart() {
        super.onStart()
//        number = intent.data.schemeSpecificPart
//        Crashlytics.log(Log.DEBUG, "CallActivity.onStart()", "intent is: " + intent.data)


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


    private fun loadValues() {
        Timber.d("Loading new values");
        urlString = remoteConfig.getString(URL_STRING)
        triggerUrlString = remoteConfig.getString(TRIGGER_URL_STRING)
        botId = remoteConfig.getString(BOT_ID)
        waitTime = remoteConfig.getDouble(WAIT_TIME)
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
        Timber.d("Hanging up call from $number")
        Timber.d("Callcount: $callCount")
        OngoingCall.hangup()

        signInDoStuff()
    }

    private fun signInDoStuff()  {
        println("Signing in and doing stuff")

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(this, "No user found. Can't forward calls.", Toast.LENGTH_SHORT).show()
            return
        }
        val mobile = user.phoneNumber
        if (mobile == null) {
            Toast.makeText(this, "No user mobile found. Cannot forward calls", Toast.LENGTH_SHORT).show()
            return
        }
        userId = mobile

        user.getIdToken(true).addOnCompleteListener{
            task ->
                if (task.isSuccessful) {
                    println("Got token")
                    val token = task.result?.token
                    sendHttpPost(token)
                    incrementCallCount(userId)

                    return@addOnCompleteListener
                }

            Timber.d("Error ${task.exception}")
            Toast.makeText(this, "Error doing stuff", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener{err ->
            Timber.d("Error $err")
            Toast.makeText(this, "Error doing stuff", Toast.LENGTH_SHORT).show()
        }

    }



    /**
     * Send a POST about the missed call
     *
     * POST
     *
     * Body: {
     *      "botId": "voicebook,
            "unformattedMobile": "+6132314237",
            "url": "https://us-central1-tz-phone-book-dev.cloudfunctions.net/twiml/entrypoint",
            "wait": 30,
            "userId": +61234234234,
        }
     */
    fun sendHttpPost(token: String?) {
        val body = "{\"unformattedMobile\":\"$number\",\n \"url\":\"$triggerUrlString\",\n\"wait\": $waitTime,\n \"botId\":\"$botId\",\n\"userId\":\"$userId\"}";
        if (token == null) {
            Toast.makeText(this, "Token could not be found", Toast.LENGTH_SHORT).show()
            return
        }

        Timber.d("sending http post to url $urlString")
        Timber.d("body is $body")
        Fuel.post(urlString)
            .timeout(60000)
            .jsonBody(body)
            .header(AUTHORIZATION to "Bearer $token")
            .responseString { _, _, result ->
            result.fold({ d ->
                Timber.d("Success: $d")
            }, { err ->
                Toast.makeText(this, "Error with request: $err", Toast.LENGTH_SHORT).show()
                Timber.d("Error with request: $err")
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

    }

}
