package com.example.data.remote

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(
    exception: Throwable,
    operationType: OperationType,
    path: String?
): String {
    val causes = generateSequence(exception) { it.cause }
    val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
        ?: causes.firstOrNull { it !is CancellationException }
        ?: exception

    val currentUser = try {
        Firebase.auth.currentUser
    } catch (_: Exception) {
        null
    }

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", rootError.message ?: rootError.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

fun toArabicFirebaseMessage(error: Throwable): String {
    val msg = error.message.orEmpty()
    val firestoreEx = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
    return when {
        firestoreEx?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "ليس لديك صلاحية تنفيذ هذه العملية أو البيانات تخالف قواعد الأمان"
        firestoreEx?.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
            msg.contains("offline", ignoreCase = true) ->
            "تعذر الاتصال بقاعدة البيانات السحابية، يرجى التحقق من الإنترنت"
        msg.contains("Quota exceeded", ignoreCase = true) ->
            "تم تجاوز الحد اليومي لقراءة البيانات في Firebase مؤقتاً"
        error is IllegalArgumentException || error is IllegalStateException || error is SecurityException ->
            error.message ?: "تعذر إتمام العملية"
        else -> error.localizedMessage ?: "حدث خطأ غير متوقع في الاتصال بالخادم"
    }
}
