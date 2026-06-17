package com.kheyr.sms.notifications

data class RingtoneSettings(val globalRingtoneUri: String?, val threadRingtoneUri: String?)
object RingtoneOverrideResolver {
    fun resolve(settings: RingtoneSettings): String? = settings.threadRingtoneUri ?: settings.globalRingtoneUri
}
