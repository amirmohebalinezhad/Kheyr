package com.kheyr.sms.thread

data class ScheduledSmsRequest(val recipient: String, val body: String, val simId: Int?, val sendAtMillis: Long) {
    val isDue: Boolean get() = System.currentTimeMillis() >= sendAtMillis
}
