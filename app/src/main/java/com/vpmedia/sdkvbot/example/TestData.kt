package com.vpmedia.sdkvbot.example

import android.content.Context
import org.json.JSONObject

data class SDKMember(
    val type: String,
    val typeTest: String,
    val ext: String,
    val callName: String,
    val token: String
) {
    fun toJson(): String = JSONObject().apply {
        put("type", type)
        put("typeTest", typeTest)
        put("ext", ext)
        put("callName", callName)
        put("token", token)
    }.toString()

    companion object {
        fun fromJson(json: String): SDKMember {
            val obj = JSONObject(json)
            return SDKMember(
                type = obj.getString("type"),
                typeTest = obj.getString("typeTest"),
                ext = obj.getString("ext"),
                callName = obj.getString("callName"),
                token = obj.getString("token")
            )
        }
    }
}

val TestAccount: List<SDKMember> = listOf(
    SDKMember("android", "vbot", "112", "vbottester 01 Staging", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJWYWx1ZSI6IjIyLTE1NS0zNS0xMzY0In0.OPL0OHrmKDGrWWdKx22Ys4K7fMkqbGetH8b3qCiRxXM"),
    SDKMember("android", "vbot", "113", "vbottester 02 Staging", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJWYWx1ZSI6IjIyLTE1Ny0zNi00MyJ9.lbE3ExHza6t8kcMrT9BPQGmtCUz_l1F3Pk9WX7OrQ3w"),
)

val VBotMemberForCall: List<SDKMember> = listOf(
    SDKMember("android", "vbot", "2630", "vbottester 03", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJWYWx1ZSI6IjU0ODctNjMxMi0zNzUtODA1In0.gIg859oUGCDycy1hCUJXEyDve7j5YGJREIgDWMpxXbc")
)

private const val PREFS_NAME = "vbot_example_prefs"
private const val KEY_SDK_MEMBER = "SDKMember"
private const val KEY_PUSH_TOKEN = "PushToken"

fun findTestMember(ext: String): SDKMember? = TestAccount.firstOrNull { it.ext == ext }

fun saveSDKMember(context: Context, member: SDKMember) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        .putString(KEY_SDK_MEMBER, member.toJson())
        .apply()
}

fun getSDKMember(context: Context): SDKMember? {
    val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_SDK_MEMBER, null) ?: return null
    return try {
        SDKMember.fromJson(json)
    } catch (exc: Exception) {
        null
    }
}

fun deleteSDKMember(context: Context) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        .remove(KEY_SDK_MEMBER)
        .apply()
}

fun savePushToken(context: Context, token: String) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        .putString(KEY_PUSH_TOKEN, token)
        .apply()
}

fun getPushToken(context: Context): String? {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_PUSH_TOKEN, null)
}
