package mobi.lab.keyimportdemo.infrastructure.common.platform

import android.content.Context
import android.util.Log
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule
import mobi.lab.keyimportdemo.infrastructure.BuildConfig

@GlideModule
class MyGlideModule : AppGlideModule() {

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        builder.setLogLevel(if (BuildConfig.DEBUG) Log.VERBOSE else Log.ERROR)
    }
}
