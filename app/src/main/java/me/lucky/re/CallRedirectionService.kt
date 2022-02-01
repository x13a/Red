package me.lucky.re

import android.database.Cursor
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.CallRedirectionService
import android.telecom.PhoneAccountHandle

class CallRedirectionService : CallRedirectionService() {
    companion object {
        private const val SIGNAL_MIMETYPE = "vnd.android.cursor.item/vnd.org.thoughtcrime.securesms.call"
        private const val TELEGRAM_MIMETYPE = "vnd.android.cursor.item/vnd.org.telegram.messenger.android.call"
    }

    private lateinit var prefs: Preferences
    private lateinit var dialog: DialogWindow
    private var connectivityManager: ConnectivityManager? = null

    override fun onCreate() {
        super.onCreate()
        prefs = Preferences(this)
        dialog = DialogWindow(this)
        connectivityManager = getSystemService(ConnectivityManager::class.java)
    }

    override fun onPlaceCall(
        handle: Uri,
        initialPhoneAccount: PhoneAccountHandle,
        allowInteractiveResponse: Boolean,
    ) {
        if (!prefs.isServiceEnabled || !hasInternet()) {
            placeCallUnmodified()
            return
        }
        val uri = getUriFromPhoneNumber(handle.schemeSpecificPart)
        if (uri != null) {
            dialog.show(uri)
            return
        }
        placeCallUnmodified()
    }

    private fun getContactIdByPhoneNumber(phoneNumber: String): String? {
        var result: String? = null
        val cursor: Cursor?
        try {
            cursor = contentResolver.query(
                Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(phoneNumber)
                ),
                arrayOf(ContactsContract.PhoneLookup._ID),
                null,
                null,
                null,
            )
        } catch (exc: SecurityException) { return null }
        cursor?.apply {
            if (moveToFirst()) {
                result = getString(getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID))
            }
            close()
        }
        return result
    }

    private fun getUriFromPhoneNumber(phoneNumber: String): Uri? {
        val contactId = getContactIdByPhoneNumber(phoneNumber) ?: return null
        var result: Uri? = null
        val cursor: Cursor?
        try {
            cursor = contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(ContactsContract.Data._ID),
                 "${ContactsContract.Data.CONTACT_ID} = ? AND " +
                         "${ContactsContract.Data.MIMETYPE} IN (?, ?)",
                arrayOf(contactId, SIGNAL_MIMETYPE, TELEGRAM_MIMETYPE),
                null,
            )
        } catch (exc: SecurityException) { return null }
        cursor?.apply {
            if (moveToFirst()) {
                result = Uri.withAppendedPath(
                    ContactsContract.Data.CONTENT_URI,
                    Uri.encode(getString(getColumnIndexOrThrow(ContactsContract.Data._ID))),
                )
            }
            close()
        }
        return result
    }

    private fun hasInternet(): Boolean {
        return connectivityManager
            ?.getNetworkCapabilities(connectivityManager?.activeNetwork)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
    }
}
