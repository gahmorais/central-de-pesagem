package br.com.backupautomacao.centraldepesagem.printers

import android.hardware.usb.UsbDevice
import br.com.backupautomacao.centraldepesagem.network.MgvProduct

interface Printer {
    fun initializePrinter(device: UsbDevice)
    fun print(product: MgvProduct)
    fun disconnect()
}



/*
import android.hardware.usb.UsbDevice
import br.com.backupautomacao.centraldepesagem.network.MgvProduct

/**
 * Interface comum para impressoras térmicas USB (Epson, Honeywell, etc.).
 */
interface Printer {

    /**
     * Inicializa a impressora com o dispositivo USB fornecido.
     * @param device O dispositivo USB conectado à impressora.
     */
    fun initializePrinter(device: UsbDevice)

    /**
     * Imprime o conteúdo referente ao produto fornecido.
     * @param product O produto com dados a serem impressos.
     */
    fun print(product: MgvProduct)

    /**
     * Encerra a conexão com a impressora, liberando recursos.
     */
    fun disconnect()
}

 */
