package com.example.customerscanner

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var pendingIntent: PendingIntent
    private lateinit var nfcIntentFilter: Array<IntentFilter>

    private val expectedUIDs = mutableStateListOf<String>()
    private val scannedUIDs = mutableStateListOf<String>()
    private var currentReceiptId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize NFC
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else 0
        pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)
        nfcIntentFilter = arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))

        // Compose UI
        setContent {
            MaterialTheme {
                AppUI()
            }
        }
    }

    @Composable
    fun AppUI() {
        var receiptId by remember { mutableStateOf(TextFieldValue("")) }
        var resultText by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = receiptId,
                onValueChange = { receiptId = it },
                label = { Text("Enter Receipt ID") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (receiptId.text.isNotEmpty()) {
                        currentReceiptId = receiptId.text.trim()
                        fetchUIDsFromFirebase(currentReceiptId) { success ->
                            resultText = if (success) {
                                "✅ Receipt loaded. Ready to scan via NFC."
                            } else {
                                "❌ No UIDs found or failed to connect to Firebase."
                            }
                        }
                    } else {
                        resultText = "❗ Enter a valid Receipt ID."
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Load Receipt")
            }

            Text("Expected items: ${expectedUIDs.size}")
            Text("Scanned items: ${scannedUIDs.size}")

            Button(
                onClick = {
                    val expectedNormalized = expectedUIDs.map { it.trim().uppercase() }.sorted()
                    val scannedNormalized = scannedUIDs.map { it.trim().uppercase() }.sorted()

                    Log.d("VERIFY", "Expected: $expectedNormalized")
                    Log.d("VERIFY", "Scanned: $scannedNormalized")

                    val isMatch = expectedNormalized == scannedNormalized
                    resultText = if (isMatch) {
                        deactivateUIDs(scannedUIDs)
                        "✅ Items verified. UIDs deactivated."
                    } else {
                        "❌ Mismatch detected!"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Verify Items")
            }

            Text(resultText)
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == NfcAdapter.ACTION_TAG_DISCOVERED) {
            Log.d("NFC_INTENT", "onNewIntent received NFC tag")
            handleNfcIntent(intent)
        }
    }

    private fun handleNfcIntent(intent: Intent) {
        @Suppress("DEPRECATION")
        val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        tag?.id?.let { idBytes ->
            val uid = idBytes.joinToString("") { byte -> "%02X".format(byte) }
            Log.d("NFC_UID", "Scanned UID: $uid")
            if (!scannedUIDs.contains(uid)) {
                scannedUIDs.add(uid)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, nfcIntentFilter, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    private fun fetchUIDsFromFirebase(receiptId: String, callback: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val urlString =
                    "https://rfid-scanner-f38e0-default-rtdb.firebaseio.com/Receipts/$receiptId/uids.json"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                val response = connection.inputStream.bufferedReader().readText()
                Log.d("FETCH_UIDS", "Response: $response")

                val jsonArray = JSONArray(response)
                expectedUIDs.clear()
                for (i in 0 until jsonArray.length()) {
                    expectedUIDs.add(jsonArray.getString(i))
                }

                withContext(Dispatchers.Main) {
                    scannedUIDs.clear()
                    callback(true)
                }
            } catch (e: Exception) {
                Log.e("FETCH_UIDS", "Failed to fetch", e)
                withContext(Dispatchers.Main) {
                    callback(false)
                }
            }
        }
    }

    private fun deactivateUIDs(uids: List<String>) {
        CoroutineScope(Dispatchers.IO).launch {
            for (uid in uids) {
                try {
                    val url = URL("https://rfid-scanner-f38e0-default-rtdb.firebaseio.com/Inventory/$uid.json")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "DELETE"
                    connection.connect()
                    Log.d("DELETE_UID", "Deleted: $uid (code ${connection.responseCode})")
                } catch (e: Exception) {
                    Log.e("DELETE_UID", "Failed to delete $uid", e)
                }
            }
        }
    }
}
