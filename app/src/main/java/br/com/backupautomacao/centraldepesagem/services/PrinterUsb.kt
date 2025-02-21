package br.com.backupautomacao.centraldepesagem.services

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.emh.thermalprinter.connection.usb.UsbConnection
import com.emh.thermalprinter.connection.usb.UsbPrintersConnections


class PrinterUsb(private val usbManager: UsbManager) {
    private val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
    private var usbConnection: UsbConnection? = null

    fun connectToDevice(): UsbDevice? {
        if (usbConnection != null) {
            val device = usbConnection?.device
            if (device != null) {
                usbConnection?.connect()
                return device
            } else {
                throw Exception("Não foi possível conectar com dispositivo")
            }
        } else {
            throw Exception("Erro ao conectar")
        }
    }

    fun openConnection(context: Context) {
        usbConnection = UsbPrintersConnections.selectFirstConnected(context)
        if (usbConnection != null) {
            val permissionIntent = PendingIntent.getBroadcast(
                context,
                0,
                Intent(ACTION_USB_PERMISSION),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE
                else 0
            )
            usbManager.requestPermission(usbConnection!!.device, permissionIntent)
        }
    }

    fun requestPermission(device: UsbDevice, context: Context) {
        val permissionIntent = PendingIntent.getBroadcast(
            context, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE
        )
        usbManager.requestPermission(device, permissionIntent)
    }
}