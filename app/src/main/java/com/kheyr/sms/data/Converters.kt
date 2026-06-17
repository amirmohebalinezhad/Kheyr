package com.kheyr.sms.data

import androidx.room.TypeConverter
import java.time.Instant

class Converters {
    @TypeConverter fun instantToMillis(value: Instant?): Long? = value?.toEpochMilli()
    @TypeConverter fun millisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter fun directionToString(value: MessageDirection): String = value.name
    @TypeConverter fun stringToDirection(value: String): MessageDirection = MessageDirection.valueOf(value)

    @TypeConverter fun statusToString(value: MessageStatus): String = value.name
    @TypeConverter fun stringToStatus(value: String): MessageStatus = MessageStatus.valueOf(value)
}
