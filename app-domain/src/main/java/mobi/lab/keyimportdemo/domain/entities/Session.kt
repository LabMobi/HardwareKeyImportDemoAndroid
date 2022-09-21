package mobi.lab.keyimportdemo.domain.entities

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import mobi.lab.keyimportdemo.app.common.isStringEmpty

@Parcelize
data class Session(val token: String?) : Parcelable {

    fun isValid(): Boolean {
        return !isStringEmpty(token)
    }
}
