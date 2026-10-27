package com.vpmedia.sdkvbot.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import android.widget.Toast
import com.google.firebase.messaging.FirebaseMessaging
import com.vpmedia.sdkvbot.client.ClientListener
import com.vpmedia.sdkvbot.client.VBotClient
import com.vpmedia.sdkvbot.client.VBotConfig
import com.vpmedia.sdkvbot.client.VBotEnvironment
import com.vpmedia.sdkvbot.domain.pojo.mo.Hotline
import com.vpmedia.sdkvbot.en.AccountRegistrationState
import com.vpmedia.sdkvbot.en.CallState
import com.vpmedia.sdkvbot.en.VBotEndCallReason
import com.vpmedia.sdkvbot.example.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "VBotExample"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var client: VBotClient

    private var hotlines: List<Hotline> = emptyList()
    private var selectedHotline: Hotline? = null

    private var selectedMember: SDKMember? = null
    private var memberToCall: SDKMember? = null

    private val environments = VBotEnvironment.entries.toTypedArray()

    private val listener = object : ClientListener() {
        override fun onAccountRegistrationState(status: AccountRegistrationState, reason: String) {
            Log.d(TAG, "onAccountRegistrationState: $status ${if (reason.isNotBlank()) "($reason)" else ""}")
        }

        override fun onUserConnected(displayName: String) {
            Log.d(TAG, "onUserConnected: $displayName")
        }

        override fun onCallState(state: CallState) {
            Log.d(TAG, "onCallState: $state")
            runOnUiThread {
                binding.tvCallState.text = "Call state: $state"
                val callInProgress = state in setOf(
                    CallState.Incoming, CallState.Calling, CallState.Early,
                    CallState.Connecting, CallState.Confirmed
                )
                binding.callControls.visibility = if (callInProgress) android.view.View.VISIBLE else android.view.View.GONE
            }
        }

        override fun onCallEnded(reason: VBotEndCallReason) {
            Log.d(TAG, "onCallEnded: $reason (${reason.code})")
        }

        override fun onNetworkUnreachable() {
            Log.d(TAG, "onNetworkUnreachable")
        }

        override fun onExternalCallId(externalCallId: String) {
            Log.d(TAG, "onExternalCallId: $externalCallId")
        }

        override fun onCallMuteStateChanged(muted: Boolean) {
            Log.d(TAG, "onCallMuteStateChanged: $muted")
        }

        override fun onErrorCode(erCode: Int, message: String) {
            Log.d(TAG, "onErrorCode: $erCode - $message")
        }
    }

    private val selectMemberLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data ?: return@registerForActivityResult
        val json = data.getStringExtra(SelectMemberActivity.EXTRA_SELECTED_MEMBER) ?: return@registerForActivityResult
        val toCall = data.getBooleanExtra(SelectMemberActivity.EXTRA_TO_CALL, false)
        val member = SDKMember.fromJson(json)
        if (toCall) {
            memberToCall = member
            binding.btnSelectMemberToCall.text = "Gọi cho: ${member.callName}"
            updateCallButtonState()
        } else {
            selectedMember = member
            binding.btnSelectMember.text = "Đã chọn: ${member.callName}"
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val denied = results.filterValues { !it }.keys
        if (denied.isNotEmpty()) {
            Log.d(TAG, "Permissions denied: $denied")
        } else {
            Log.d(TAG, "Permissions granted")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        client = VBotClient(applicationContext)
        client.addListener(listener)

        setupEnvironmentSpinner()
        setupActions()
        requestRuntimePermissionsIfNeeded()
        refreshAccountView()

        val production = VBotEnvironment.PRODUCTION
        binding.spinnerEnvironment.setSelection(environments.indexOf(production))
        if (!client.isSetup()) {
            Log.d(TAG, "Setup SDK with environment: ${production.description} (${production.url})")
            client.setup(VBotConfig(production))
        }

        binding.etPhone.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateCallButtonState()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onDestroy() {
        client.removeListener(listener)
        super.onDestroy()
    }

    private fun setupEnvironmentSpinner() {
        val labels = environments.map { it.description }
        binding.spinnerEnvironment.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            labels
        )
    }

    private fun setupActions() {
        binding.btnSetup.setOnClickListener {
            val env = environments[binding.spinnerEnvironment.selectedItemPosition]
            Log.d(TAG, "Setup SDK with environment: ${env.description} (${env.url})")
            client.setup(VBotConfig(env))
        }

        binding.btnSelectMember.setOnClickListener {
            selectMemberLauncher.launch(SelectMemberActivity.newIntent(this, toCall = false))
        }

        binding.btnLogin.setOnClickListener {
            onLoginButtonTapped()
        }

        binding.btnSelectMemberToCall.setOnClickListener {
            selectMemberLauncher.launch(SelectMemberActivity.newIntent(this, toCall = true))
        }

        binding.btnCall.setOnClickListener {
            makeCall()
        }

        binding.btnGetHotlines.setOnClickListener {
            fetchHotlines(showPicker = false)
        }

        binding.btnSelectHotline.setOnClickListener {
            fetchHotlines(showPicker = true)
        }

        binding.btnAnswer.setOnClickListener {
            Log.d(TAG, "answerCall()")
            client.answerCall()
        }

        binding.btnDecline.setOnClickListener {
            Log.d(TAG, "declineIncomingCall(isBusy=true)")
            client.declineIncomingCall(true)
        }

        binding.btnEndCall.setOnClickListener {
            Log.d(TAG, "endCall()")
            client.endCall()
        }

        binding.btnMute.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "muteCall(enable=$isChecked)")
            client.muteCall(isChecked)
        }

        binding.btnSpeaker.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "onOffSpeaker(enable=$isChecked)")
            client.onOffSpeaker(isChecked)
        }

        binding.btnSendDtmf.setOnClickListener {
            val digits = binding.etDtmf.text.toString().trim()
            Log.d(TAG, "sendDTMF($digits)")
            client.sendDTMF(digits)
        }
    }

    private fun onLoginButtonTapped() {
        if (getSDKMember(this) == null) {
            val member = selectedMember
            if (member == null) {
                AlertDialog.Builder(this)
                    .setTitle("Lỗi")
                    .setMessage("Code: 401\nMessage: Chưa chọn thành viên")
                    .setPositiveButton("Đóng", null)
                    .show()
                return
            }
            saveMember(member)
        } else {
            deleteMember()
        }
    }

    private fun saveMember(member: SDKMember) {
        showProgress()
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                val pushToken = if (task.isSuccessful && !task.result.isNullOrEmpty()) {
                    task.result
                } else {
                    Log.d(TAG, "FCM getToken failed: ${task.exception?.message}")
                    getPushToken(this) ?: ""
                }
                savePushToken(this, pushToken)
                connectMember(member, pushToken)
            }
    }

    private fun connectMember(member: SDKMember, pushToken: String) {
        Log.d(TAG, "connect(token=${member.token}, tokenFirebase=$pushToken)")
        client.connect(member.token, pushToken) { displayName, error ->
            runOnUiThread {
                hideProgress()
                if (error == null) {
                    Log.d(TAG, "connect success: $displayName")
                    saveSDKMember(this, member)
                    refreshAccountView()
                } else {
                    Log.d(TAG, "connect error: ${error.code} - ${error.message}")
                    AlertDialog.Builder(this)
                        .setTitle("Lỗi")
                        .setMessage("Không thể kết nối tài khoản. ${error.message}")
                        .setPositiveButton("Đóng", null)
                        .show()
                }
            }
        }
    }

    private fun deleteMember() {
        showProgress()
        Log.d(TAG, "disconnect()")
        client.disconnect { _, error ->
            runOnUiThread {
                hideProgress()
                if (error != null) {
                    Log.d(TAG, "disconnect error: ${error.code} - ${error.message}")
                }
                deleteSDKMember(this)
                selectedMember = null
                refreshAccountView()
            }
        }
    }

    private fun refreshAccountView() {
        val member = getSDKMember(this)
        if (member != null) {
            binding.tvAccountLabel.text = "Đã kết nối: ${member.callName}"
            binding.tvAccountLabel.setTextColor(android.graphics.Color.parseColor("#009688"))
            binding.btnSelectMemberToCall.isEnabled = true
            binding.btnLogin.text = "Xóa"
            binding.btnLogin.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#F44336"))

            val matched = findTestMember(member.ext)
            if (matched != null) {
                selectedMember = matched
                binding.btnSelectMember.text = "Đã chọn: ${matched.callName}"
                binding.btnSelectMember.isEnabled = false
            }
        } else {
            binding.tvAccountLabel.text = "Chưa chọn tài khoản"
            binding.tvAccountLabel.setTextColor(android.graphics.Color.parseColor("#F44336"))
            binding.btnSelectMember.isEnabled = true
            binding.btnSelectMemberToCall.isEnabled = false
            binding.btnLogin.text = "Lưu"
            binding.btnLogin.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2196F3"))
        }
        updateCallButtonState()
    }

    private fun updateCallButtonState() {
        val phone = binding.etPhone.text.toString().trim()
        binding.btnCall.isEnabled = memberToCall != null || phone.isNotEmpty()
    }

    // LƯU Ý BẢO MẬT: Trong môi trường thực tế, API tạo call-session phải được gọi từ Backend của bạn để bảo vệ API Key.
    // Tuyệt đối không lưu Secret Key trực tiếp trên ứng dụng client.
    private val openApiKey = "YOUR_OPEN_API_KEY"

    private fun fetchCallSession(phoneNumber: String, onResult: (sessionId: String?, error: String?) -> Unit) {
        if (openApiKey == "YOUR_OPEN_API_KEY") {
            onResult(null, "Vui lòng cấu hình openApiKey trong MainActivity.kt hoặc tạo session từ backend")
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://open-api-staging.vbot.vn/v3.0/api/call-sessions")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("X-API-Key", openApiKey)
                    doOutput = true
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val reqBody = JSONObject().apply {
                    put("phone_number", phoneNumber)
                }.toString()

                connection.outputStream.use { os ->
                    os.write(reqBody.toByteArray())
                }

                val responseCode = connection.responseCode
                val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
                val responseText = stream.bufferedReader().use { it.readText() }

                val json = JSONObject(responseText)
                val error = json.optInt("error", -1)
                if (error == 0) {
                    val data = json.optJSONObject("data")
                    val sessionId = data?.optString("session_id")
                    withContext(Dispatchers.Main) {
                        if (!sessionId.isNullOrEmpty()) {
                            onResult(sessionId, null)
                        } else {
                            onResult(null, "Không tìm thấy session_id trong phản hồi")
                        }
                    }
                } else {
                    val message = json.optString("message", "Lỗi tạo call session: $responseCode")
                    withContext(Dispatchers.Main) {
                        onResult(null, message)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(null, e.localizedMessage ?: "Lỗi kết nối máy chủ")
                }
            }
        }
    }

    private fun makeCall() {
        val phoneText = binding.etPhone.text.toString().trim()
        val phone = phoneText.ifEmpty { memberToCall?.ext ?: "" }
        val hotline = selectedHotline?.phoneNumber ?: ""
        val externalCallId = generateExternalCallId()

        if (binding.switchCallSession.isChecked) {
            showProgress()
            binding.btnCall.isEnabled = false
            fetchCallSession(phone) { sessionId, error ->
                hideProgress()
                binding.btnCall.isEnabled = true
                if (error != null) {
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                    return@fetchCallSession
                }
                if (!sessionId.isNullOrEmpty()) {
                    Log.d(TAG, "startOutgoingCall with Call Session: hotline=$hotline, sessionId=$sessionId, externalCallId=$externalCallId")
                    client.startOutgoingCall(hotline, sessionId, externalCallId)
                }
            }
        } else {
            binding.btnCall.isEnabled = false
            Log.d(TAG, "startOutgoingCall(hotline=$hotline, phone=$phone, externalCallId=$externalCallId)")
            client.startOutgoingCall(hotline, phone, externalCallId)
            binding.btnCall.isEnabled = true
        }
    }

    private fun fetchHotlines(showPicker: Boolean) {
        Log.d(TAG, "getHotlines()")
        lifecycleScope.launch {
            val result = client.getHotlines()
            if (result == null) {
                Log.d(TAG, "getHotlines() -> null (see onErrorCode above)")
                return@launch
            }
            hotlines = result
            Log.d(TAG, "getHotlines() -> ${result.size} hotline(s)")
            result.forEach { Log.d(TAG, "  - ${it.name} (${it.phoneNumber})") }
            if (showPicker) {
                showHotlinePicker()
            } else {
                val message = if (result.isEmpty()) {
                    "No hotlines available"
                } else {
                    result.joinToString("\n") { "${it.name} (${it.phoneNumber})" }
                }
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Hotlines")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun showHotlinePicker() {
        if (hotlines.isEmpty()) {
            binding.tvSelectedHotline.text = "Chưa chọn hotline"
            return
        }
        val labels = hotlines.map { "${it.name} (${it.phoneNumber})" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Chọn Hotline")
            .setItems(labels) { _, index ->
                selectedHotline = hotlines[index]
                binding.tvSelectedHotline.text = "Hotline: ${labels[index]}"
                binding.tvSelectedHotline.setTextColor(android.graphics.Color.parseColor("#2196F3"))
            }
            .show()
    }

    private fun requestRuntimePermissionsIfNeeded() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.RECORD_AUDIO)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    private fun generateExternalCallId(length: Int = 32): String {
        val maxLength = length.coerceAtMost(32)
        val allowedCharacters = "abcdefghijklmnopqrstuvwxyz0123456789"
        return (1..maxLength)
            .map { allowedCharacters.random() }
            .joinToString("")
    }
}
