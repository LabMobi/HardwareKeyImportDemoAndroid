package mobi.lab.keyimportdemo.debug

import android.content.Context
import mobi.lab.keyimportdemo.common.debug.DebugActions

class DevDebugActions : DebugActions {
    override fun launchDebugActivity(context: Context) {
        return context.startActivity(DebugActivity.getIntent(context))
    }
}
