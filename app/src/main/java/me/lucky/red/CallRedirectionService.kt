package me.lucky.red

import android.Manifest
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.CallRedirectionService
import android.telecom.PhoneAccountHandle
import androidx.annotation.RequiresPermission

class CallRedirectionService : CallRedirectionService() {
    companion object {
        private const val PREFIX = "vnd.android.cursor.item/vnd"
        private const val SIGNAL_MIMETYPE = "$PREFIX.org.thoughtcrime.securesms.call"
        private const val TELEGRAM_MIMETYPE = "$PREFIX.org.telegram.messenger.android.call"
        private const val THREEMA_MIMETYPE = "$PREFIX.ch.threema.app.call"
        private const val WHATSAPP_MIMETYPE = "$PREFIX.com.whatsapp.voip.call"
        private const val VIBER_MIMETYPE = "$PREFIX.com.viber.voip.call"
        private val MIMETYPES = mapOf(
            SIGNAL_MIMETYPE to 0,
            TELEGRAM_MIMETYPE to 1,
            THREEMA_MIMETYPE to 2,
            WHATSAPP_MIMETYPE to 48,
            VIBER_MIMETYPE to 49,
        )
        private val FALLBACK_MIMETYPES = arrayOf(
            WHATSAPP_MIMETYPE,
            VIBER_MIMETYPE,
        )
    }

    lateinit var prefs: Preferences
    private lateinit var window: PopupWindow
    private var connectivityManager: ConnectivityManager? = null

    override fun onCreate() {
        super.onCreate()
        init()
    }

    override fun onDestroy() {
        super.onDestroy()
        window.cancel()
    }

    private fun init() {
        prefs = Preferences(this)
        window = PopupWindow(this)
        connectivityManager = getSystemService(ConnectivityManager::class.java)
    }

    override fun onPlaceCall(
        handle: Uri,
        initialPhoneAccount: PhoneAccountHandle,
        allowInteractiveResponse: Boolean,
    ) {
        if (!prefs.isServiceEnabled || !hasInternet() || !allowInteractiveResponse) {
            placeCallUnmodified()
            return
        }
        val records: Array<Record>
        try {
            records = getRecordsFromPhoneNumber(handle.schemeSpecificPart)
        } catch (exc: SecurityException) {
            placeCallUnmodified()
            return
        }
        val record = records.minByOrNull { MIMETYPES[it.mimetype] ?: 0 }
        if (record == null || (record.mimetype in FALLBACK_MIMETYPES && !prefs.isFallbackChecked)) {
            placeCallUnmodified()
            return
        }
        window.show(record.uri, when (record.mimetype) {
            SIGNAL_MIMETYPE -> R.string.destination_signal
            TELEGRAM_MIMETYPE -> R.string.destination_telegram
            THREEMA_MIMETYPE -> R.string.destination_threema
            WHATSAPP_MIMETYPE -> R.string.fallback_destination_whatsapp
            VIBER_MIMETYPE -> R.string.fallback_destination_viber
            else -> return
        })
    }

    @RequiresPermission(Manifest.permission.READ_CONTACTS)
    private fun getContactIdByPhoneNumber(phoneNumber: String): String? {
        var result: String? = null
        val cursor = contentResolver.query(
            Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            ),
            arrayOf(ContactsContract.PhoneLookup._ID),
            null,
            null,
            null,
        )
        cursor?.apply {
            if (moveToFirst())
                result = getString(getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID))
            close()
        }
        return result
    }

    private data class Record(val uri: Uri, val mimetype: String)

    @RequiresPermission(Manifest.permission.READ_CONTACTS)
    private fun getRecordsFromPhoneNumber(phoneNumber: String): Array<Record> {
        val results = mutableSetOf<Record>()
        val contactId = getContactIdByPhoneNumber(phoneNumber) ?: return results.toTypedArray()
        val cursor = contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data._ID, ContactsContract.Data.MIMETYPE),
             "${ContactsContract.Data.CONTACT_ID} = ? AND " +
                     "${ContactsContract.Data.MIMETYPE} IN " +
                     "(${MIMETYPES.keys.joinToString(",") { "?" }})",
            arrayOf(contactId, *MIMETYPES.keys.toTypedArray()),
            null,
        )
        cursor?.apply {
            while (moveToNext())
                results.add(Record(
                    Uri.withAppendedPath(
                        ContactsContract.Data.CONTENT_URI,
                        Uri.encode(getString(getColumnIndexOrThrow(ContactsContract.Data._ID))),
                    ),
                    getString(getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE)),
                ))
            close()
        }
        return results.toTypedArray()
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private fun hasInternet(): Boolean {
        val capabilities = connectivityManager
            ?.getNetworkCapabilities(connectivityManager?.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
