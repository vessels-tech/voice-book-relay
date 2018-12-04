package tech.vessels.relay

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.model.Document
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableResult
import kotlinx.android.synthetic.main.activity_dialer.*
import timber.log.Timber

class FirebaseApi {

    companion object {
        /**
         * Get the total number of calls for this user
         *
         * returns 0 if it can't find the number
         *
         * /relay/voicebook/user/+61410237238
         */
        fun getCallCount(userId: String): Task<DocumentSnapshot> {
            val db = FirebaseFirestore.getInstance()
            val ref = db.collection("relay").document("voicebook").collection("user").document(userId)

            return ref.get()
        }

        /**
         * Increment the total number of calls by one
         */
        fun incrementCallCount(userId: String): Task<DocumentSnapshot> {
            val db = FirebaseFirestore.getInstance()
            val ref = db.collection("relay").document("voicebook").collection("user").document(userId)

            val currentCountTask = getCallCount(userId)
            return currentCountTask.addOnCompleteListener{ task: Task<DocumentSnapshot> ->
                if (task.isSuccessful && task.result != null) {
                    var currentCount = task.result?.data?.get("callCount").toString().toInt()
                    currentCount += 1

                    val data = HashMap<String, Any>()
                    data["callCount"] = currentCount
                    ref.set(HashMap(data))
                        .addOnSuccessListener { println("DocumentSnapshot successfully written!") }
                        .addOnFailureListener { e -> println("Error writing document $e") }

                } else {
                    println("Could not get the latest callCount")
                }
            }
        }

//        /**
//         * Trigger the call using the firebase api.
//         */
//        fun triggerCall(userId: String,
//                        botId: String,
//                        unformattedMobile: String,
//                        url: String,
//                        triggerUrl: String,
//                        wait: Double = 10.0): Task<String> {
//            val functions = FirebaseFunctions.getInstance()
//            // Create the arguments to the callable function
//
//            val data = hashMapOf(
//                    "userId" to userId,
//                    "botId" to botId,
//                    "triggerUrl" to triggerUrl,
//                    "unformattedMobile" to unformattedMobile,
//                    "wait" to wait
//            )
//
//            Timber.d("data is $data")
//
//            return functions
//                    .getHttpsCallable(url)
//                    .call(data)
//                    .continueWith { task ->
//                        // This continuation runs on either success or failure, but if the task
//                        // has failed then result will throw an Exception which will be
//                        // propagated down.
//                        val result = task.result?.data as String
//                        result
//                    }
//
//        }
    }
}