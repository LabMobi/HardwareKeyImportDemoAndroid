package mobi.lab.keyimportdemo.app.common

import java.time.OffsetDateTime

fun OffsetDateTime.toEpochMilli() = this.toInstant().toEpochMilli()
