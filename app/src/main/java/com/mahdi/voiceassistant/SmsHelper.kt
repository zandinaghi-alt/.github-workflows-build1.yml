package com.mahdi.voiceassistant

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.SmsManager

data class SmsMessage(
    val address: String,
    val body: String,
    val date: Long
)

object SmsHelper {

    /**
     * خوندن آخرین N پیامک دریافتی از content provider استاندارد اندروید.
     * نیازمند READ_SMS
     */
    fun getLatestMessages(context: Context, limit: Int = 5): List<SmsMessage> {
        val messages = mutableListOf<SmsMessage>()
        val uri: Uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("address", "body", "date")

        val cursor: Cursor? = context.contentResolver.query(
            uri, projection, null, null, "date DESC LIMIT $limit"
        )

        cursor?.use {
            val addressIdx = it.getColumnIndex("address")
            val bodyIdx = it.getColumnIndex("body")
            val dateIdx = it.getColumnIndex("date")
            while (it.moveToNext()) {
                messages.add(
                    SmsMessage(
                        address = it.getString(addressIdx) ?: "",
                        body = it.getString(bodyIdx) ?: "",
                        date = it.getLong(dateIdx)
                    )
                )
            }
        }
        return messages
    }

    /**
     * فرستادن پیامک. نیازمند SEND_SMS
     */
    fun sendSms(phoneNumber: String, message: String) {
        val smsManager = SmsManager.getDefault()
        val parts = smsManager.divideMessage(message)
        smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
    }

    /**
     * پیدا کردن شماره تلفن از روی اسم مخاطب (برای دستورهایی مثل "به علی پیامک بده")
     * نیازمند READ_CONTACTS
     */
    fun findPhoneNumberByName(context: Context, name: String): String? {
        val resolver: ContentResolver = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$name%")

        resolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                return cursor.getString(numberIdx)
            }
        }
        return null
    }
}
