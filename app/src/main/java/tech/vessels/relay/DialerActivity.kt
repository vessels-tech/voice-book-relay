package tech.vessels.relay
import android.Manifest.permission.CALL_PHONE
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.ActivityCompat.requestPermissions
import android.support.v4.content.PermissionChecker.PERMISSION_GRANTED
import android.support.v4.content.PermissionChecker.checkSelfPermission
import android.support.v7.app.AppCompatActivity
import android.telecom.TelecomManager
import android.telecom.TelecomManager.ACTION_CHANGE_DEFAULT_DIALER
import android.telecom.TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME
import android.widget.Toast
import androidx.core.content.systemService
import androidx.core.net.toUri
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.android.synthetic.main.activity_dialer.*
import tech.vessels.relay.FirebaseApi.Companion.getCallCount
import timber.log.Timber

class DialerActivity : AppCompatActivity() {

    private lateinit var remoteConfig: FirebaseRemoteConfig
    private lateinit var urlString: String
    private lateinit var triggerUrlString: String
    private lateinit var botId: String
    private var waitTime: Double = 10.00
    private lateinit var user: FirebaseUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dialer)

        remoteConfig = RemoteConfigApi.getRemoteConfig()
        RemoteConfigApi.updateRemoteConfig(this, remoteConfig)
        loadValues()
    }

    override fun onStart() {
        super.onStart()
        //Check the login status
        val currentUser = FirebaseAuth.getInstance().currentUser


        if (currentUser != null) {
            user = currentUser
           setUpUser(user)
        } else {
            // No user is signed in
            Timber.d("no user found")
            createSignInIntent()
        }

        offerReplacingDefaultDialer()
    }

//    override fun onResume() {
//        super.onResume()
//
//        setUpUser(user)
//    }

    private fun setUpUser(user: FirebaseUser) {
        // User is signed in
        val mobile = user.phoneNumber
        device_id.text = mobile

        if (mobile != null) {
            val callCountTask = getCallCount(mobile);
            callCountTask.addOnCompleteListener{ task: Task<DocumentSnapshot> ->
                if (task.isSuccessful && task.result != null && task.result?.data?.get("callCount") != null) {
                    counter_label.text = task.result?.data?.get("callCount").toString()
                } else {
                    println("Could not get the latest callCount")
                }
            }
        }
    }

    private fun createSignInIntent() {
        println("Creating sign in intent")
        // Choose authentication providers
        val providers = arrayListOf(
                AuthUI.IdpConfig.PhoneBuilder().build()
        )

        //TODO: handle the callback from this

        // Create and launch sign-in intent
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(),
            RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    user = currentUser
                    setUpUser(user)
                }
            } else {
                Toast.makeText(this, "You must sign in to continue.", Toast.LENGTH_SHORT).show()
                createSignInIntent()
            }
        }
    }


    private fun loadValues() {
        Timber.d("Loading new values");
        urlString = remoteConfig.getString(RemoteConfigApi.URL_STRING)
        triggerUrlString = remoteConfig.getString(RemoteConfigApi.TRIGGER_URL_STRING)
        botId = remoteConfig.getString(RemoteConfigApi.BOT_ID)
        waitTime = remoteConfig.getDouble(RemoteConfigApi.WAIT_TIME)

        Timber.d("Setting url string to ", urlString);
        url.setText(urlString);
    }


    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {

    }

    private fun offerReplacingDefaultDialer() {
        if (systemService<TelecomManager>().defaultDialerPackage != packageName) {
            Intent(ACTION_CHANGE_DEFAULT_DIALER)
                    .putExtra(EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
                    .let(::startActivity)
        }
    }

    companion object {
        const val REQUEST_PERMISSION = 0
        private const val RC_SIGN_IN = 123

    }
}
