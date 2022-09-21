package mobi.lab.keyimportdemo.infrastructure.auth.local

import androidx.annotation.Keep
import se.ansman.kotshi.JsonSerializable

@Keep
@JsonSerializable
data class DbSession(val token: String?)
