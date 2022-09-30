package br.com.backupautomacao.howworkswithviewmodel

class Constants private constructor() {
    companion object {
        const val INTENT_ACTION_GRANT_USB = "com.android.example.USB_PERMISSION"
        const val INTENT_ACTION_DISCONNECT = BuildConfig.APPLICATION_ID + ".Disconnect"
        const val NOTIFICATION_CHANNEL = BuildConfig.APPLICATION_ID + ".Channel"
        const val INTENT_CLASS_MAIN_ACTIVITY = BuildConfig.APPLICATION_ID + ".MainActivity"
        const val NOTIFY_MANAGER_START_FOREGROUND_SERVICE = 1001
        const val PERMISSION_BLUETOOTH_REQUEST_CODE = 201
        const val PERMISSION_STORAGE_REQUEST_CODE = 200
        const val REQUEST_ENABLE_BT = 1
        const val CONNECTING_MESSAGE = "Conectando..."
        const val MESSAGE_READ = 2
        const val CONNECTING_STATUS = 3
        const val CONNECTED_STATUS = 4
        const val ERROR_MESSAGE = 5
    }
}