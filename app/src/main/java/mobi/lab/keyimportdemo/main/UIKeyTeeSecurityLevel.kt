package mobi.lab.keyimportdemo.main

sealed class UIKeyTeeSecurityLevel {
    object TeeStrongbox : UIKeyTeeSecurityLevel() {
        override fun toString(): String {
            return "UIKeyTeeSecurityLevel.TeeStrongbox"
        }
    }

    object TeeHardwareNoStrongbox : UIKeyTeeSecurityLevel() {
        override fun toString(): String {
            return "UIKeyTeeSecurityLevel.TeeHardwareNoStrongbox"
        }
    }

    object TeeSoftware : UIKeyTeeSecurityLevel() {
        override fun toString(): String {
            return "UIKeyTeeSecurityLevel.TeeSoftware"
        }
    }

    object Unknown : UIKeyTeeSecurityLevel() {
        override fun toString(): String {
            return "UIKeyTeeSecurityLevel.Unknown"
        }
    }
}
