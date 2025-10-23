package com.example.megustalacasuela

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import java.nio.charset.Charset
import java.util.*

class BLEManager(
    private val context: Context,
    private val onData: (String, String) -> Unit
) {
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter
    private val bleScanner: BluetoothLeScanner? = bluetoothAdapter.bluetoothLeScanner
    private var gatt: BluetoothGatt? = null

    private val genericServiceUUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef0")

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (bleScanner == null) {
            Toast.makeText(context, "Bluetooth Low Energy no disponible", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(context, "Buscando dispositivos BLE...", Toast.LENGTH_SHORT).show()
        bleScanner.startScan(scanCallback)

        Handler(Looper.getMainLooper()).postDelayed({
            bleScanner.stopScan(scanCallback)
        }, 10000)
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                val name = device.name ?: "(sin nombre)"
                Log.d("BLE", "Dispositivo encontrado: $name")

                if (name.startsWith("Sensor") || name.startsWith("MedIQ") || name.startsWith("HC") || name.startsWith("IR")) {
                    bleScanner?.stopScan(this)
                    Toast.makeText(context, "Conectando a $name...", Toast.LENGTH_SHORT).show()
                    gatt = device.connectGatt(context, false, gattCallback)
                }
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("BLE", "Conectado a ${gatt.device.name}")
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.w("BLE", "Desconectado de ${gatt.device.name}")
                Toast.makeText(context, "Dispositivo desconectado", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val services = gatt.services
            services.forEach { service ->
                Log.d("BLE", "Servicio UUID: ${service.uuid}")
                service.characteristics.forEach { characteristic ->
                    Log.d("BLE", "Característica UUID: ${characteristic.uuid}")
                    enableNotifications(gatt, characteristic)
                    readCharacteristic(gatt, characteristic)
                }
            }
        }

        private fun readCharacteristic(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            TODO("Not yet implemented")
        }

        private fun enableNotifications(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            TODO("Not yet implemented")
        }




        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            processCharacteristic(characteristic)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            processCharacteristic(characteristic)
        }

        private fun processCharacteristic(characteristic: BluetoothGattCharacteristic) {
            val uuid = characteristic.uuid.toString()
            val valueBytes = characteristic.value ?: return
            val stringValue = try {
                String(valueBytes, Charset.defaultCharset()).trim()
            } catch (e: Exception) {
                valueBytes.joinToString(" ") { "%02X".format(it) }
            }

            val label = when {
                uuid.contains("2A6E") -> "Temperatura"
                uuid.contains("2A6F") -> "Humedad"
                uuid.contains("2A19") -> "Distancia"
                uuid.contains("2A57") -> "Presencia"
                uuid.contains("2A77") -> "Luz o Higrómetro"
                else -> "Dato"
            }

            Log.d("BLE", "$label = $stringValue")
            onData(label, stringValue)
        }
    }
}