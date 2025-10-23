package com.example.megustalacasuela

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class useractivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var proximitySensor: Sensor? = null
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var tvDistance: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvBluetoothStatus: TextView
    private lateinit var tvBluetoothDevice: TextView
    private lateinit var btnBluetooth: Button
    private lateinit var btnAdmin: Button
    private lateinit var ledRed: View
    private lateinit var ledYellow: View
    private lateinit var ledBlue: View

    // Bluetooth
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var isConnected = false
    private var workerThread: Thread? = null
    private var connectedDevice: BluetoothDevice? = null

    private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // Colores para los LEDs
    private val COLOR_LED_OFF = Color.parseColor("#EEEEEE")
    private val COLOR_RED_ON = Color.parseColor("#FF0000")
    private val COLOR_YELLOW_ON = Color.parseColor("#FFFF00")
    private val COLOR_BLUE_ON = Color.parseColor("#0000FF")

    private val REQUEST_BLUETOOTH_PERMISSIONS = 1

    private var useBluetoothMode = false

    // Umbrales configurables
    private var redThreshold = 0.5f
    private var yellowThreshold = 1.5f

    // Estadísticas
    private var measurementCount = 0
    private var minDistance = Float.MAX_VALUE
    private var maxDistance = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_useractivity)


        sharedPreferences = getSharedPreferences("SensorSettings", Context.MODE_PRIVATE)


        loadSettings()


        tvDistance = findViewById(R.id.tvDistance)
        tvStatus = findViewById(R.id.tvStatus)
        tvBluetoothStatus = findViewById(R.id.tvBluetoothStatus)
        tvBluetoothDevice = findViewById(R.id.tvBluetoothDevice)
        btnBluetooth = findViewById(R.id.btnBluetooth)
        btnAdmin = findViewById(R.id.btnAdmin)
        ledRed = findViewById(R.id.ledRed)
        ledYellow = findViewById(R.id.ledYellow)
        ledBlue = findViewById(R.id.ledBlue)


        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth no disponible en este dispositivo", Toast.LENGTH_LONG).show()
        }


        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        if (proximitySensor == null) {
            tvStatus.text = "Sensor de proximidad no disponible"
            tvStatus.setTextColor(Color.RED)
        }


        btnBluetooth.setOnClickListener {
            if (isConnected) {
                disconnectBluetooth()
            } else {
                checkBluetoothPermissionsAndConnect()
            }
        }

        btnAdmin.setOnClickListener {
            val intent = Intent(this, adminactivity::class.java)
            startActivity(intent)
        }

        turnOffAllLeds()
        updateBluetoothUI()
    }

    override fun onResume() {
        super.onResume()

        loadSettings()


        if (!useBluetoothMode && proximitySensor != null) {
            sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL)
            tvStatus.text = "Sensor Activo"
            tvStatus.setTextColor(Color.parseColor("#4CAF50"))
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isConnected) {
            disconnectBluetooth()
        }
        saveStatistics()
    }

    private fun loadSettings() {
        redThreshold = sharedPreferences.getFloat("redThreshold", 0.5f)
        yellowThreshold = sharedPreferences.getFloat("yellowThreshold", 1.5f)
        measurementCount = sharedPreferences.getInt("measurementCount", 0)
        minDistance = sharedPreferences.getFloat("minDistance", Float.MAX_VALUE)
        maxDistance = sharedPreferences.getFloat("maxDistance", 0f)
    }

    private fun saveStatistics() {
        val editor = sharedPreferences.edit()
        editor.putInt("measurementCount", measurementCount)
        editor.putFloat("minDistance", minDistance)
        editor.putFloat("maxDistance", maxDistance)
        editor.apply()
    }

    private fun updateStatistics(distance: Float) {
        measurementCount++
        if (distance < minDistance) {
            minDistance = distance
        }
        if (distance > maxDistance) {
            maxDistance = distance
        }
    }

    private fun checkBluetoothPermissionsAndConnect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissionsNeeded = mutableListOf<String>()

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN)
            }

            if (permissionsNeeded.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(),
                    REQUEST_BLUETOOTH_PERMISSIONS)
            } else {
                showPairedDevices()
            }
        } else {
            val permissionsNeeded = mutableListOf<String>()

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_ADMIN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }

            if (permissionsNeeded.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(),
                    REQUEST_BLUETOOTH_PERMISSIONS)
            } else {
                showPairedDevices()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                showPairedDevices()
            } else {
                Toast.makeText(this, "Permisos de Bluetooth denegados", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPairedDevices() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso de Bluetooth no concedido", Toast.LENGTH_SHORT).show()
                return
            }

            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices

            if (pairedDevices.isNullOrEmpty()) {
                Toast.makeText(this, "No hay dispositivos emparejados.\n\nEmpareja un dispositivo en:\nConfiguración → Bluetooth",
                    Toast.LENGTH_LONG).show()
                return
            }

            val deviceNames = pairedDevices.map {
                "${it.name ?: "Dispositivo desconocido"}\n${it.address}"
            }.toTypedArray()
            val deviceList = pairedDevices.toList()

            AlertDialog.Builder(this)
                .setTitle("Seleccionar Dispositivo Bluetooth")
                .setItems(deviceNames) { _, which ->
                    connectToDevice(deviceList[which])
                }
                .setNegativeButton("Cancelar", null)
                .show()

        } catch (e: SecurityException) {
            Toast.makeText(this, "Error de permisos: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        Thread {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                    runOnUiThread {
                        Toast.makeText(this, "Permiso de Bluetooth no concedido", Toast.LENGTH_SHORT).show()
                    }
                    return@Thread
                }

                runOnUiThread {
                    tvBluetoothStatus.text = "Bluetooth: Conectando..."
                    tvBluetoothDevice.text = device.name ?: "Dispositivo"
                }

                bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID)
                bluetoothAdapter?.cancelDiscovery()
                bluetoothSocket?.connect()

                inputStream = bluetoothSocket?.inputStream
                outputStream = bluetoothSocket?.outputStream

                connectedDevice = device
                isConnected = true
                useBluetoothMode = true


                sensorManager.unregisterListener(this)

                runOnUiThread {
                    updateBluetoothUI()
                    Toast.makeText(this, "✓ Conectado a ${device.name}", Toast.LENGTH_SHORT).show()
                    tvStatus.text = "Esperando datos del sensor..."
                    tvStatus.setTextColor(Color.parseColor("#4CAF50"))
                }

                beginListenForData()

            } catch (e: IOException) {
                runOnUiThread {
                    Toast.makeText(this, "Error al conectar:\n${e.message}", Toast.LENGTH_LONG).show()
                    updateBluetoothUI()
                }
                try {
                    bluetoothSocket?.close()
                } catch (closeException: IOException) {
                    closeException.printStackTrace()
                }
            }
        }.start()
    }

    private fun beginListenForData() {
        val buffer = ByteArray(1024)
        var bytes: Int

        workerThread = Thread {
            while (isConnected) {
                try {
                    bytes = inputStream?.read(buffer) ?: 0
                    if (bytes > 0) {
                        val readMessage = String(buffer, 0, bytes).trim()
                        runOnUiThread {
                            processBluetoothData(readMessage)
                        }
                    }
                } catch (e: IOException) {
                    isConnected = false
                    runOnUiThread {
                        Toast.makeText(this, "Conexión perdida", Toast.LENGTH_SHORT).show()
                        disconnectBluetooth()
                    }
                    break
                }
            }
        }
        workerThread?.start()
    }

    private fun processBluetoothData(data: String) {
        try {
            val distance = data.toFloatOrNull()

            if (distance != null && distance >= 0) {
                val distanceInMeters = if (distance > 10) distance / 100 else distance

                tvDistance.text = String.format("%.2f", distanceInMeters)
                updateLeds(distanceInMeters)
                updateStatistics(distanceInMeters)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun disconnectBluetooth() {
        try {
            isConnected = false
            useBluetoothMode = false
            workerThread?.interrupt()
            inputStream?.close()
            outputStream?.close()
            bluetoothSocket?.close()

            connectedDevice = null

            updateBluetoothUI()
            turnOffAllLeds()
            tvDistance.text = "0.00"
            tvStatus.text = "Bluetooth desconectado"
            tvStatus.setTextColor(Color.parseColor("#999999"))

            saveStatistics()

            Toast.makeText(this, "Desconectado", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun updateBluetoothUI() {
        if (isConnected && connectedDevice != null) {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED) {
                    tvBluetoothStatus.text = "Bluetooth: Conectado"
                    tvBluetoothDevice.text = connectedDevice?.name ?: "Dispositivo"
                }
            } catch (e: SecurityException) {
                tvBluetoothStatus.text = "Bluetooth: Conectado"
                tvBluetoothDevice.text = "Dispositivo"
            }
            tvBluetoothStatus.setTextColor(Color.parseColor("#4CAF50"))
            btnBluetooth.text = "Desconectar"
            btnBluetooth.setBackgroundColor(Color.parseColor("#F44336"))
        } else {
            tvBluetoothStatus.text = "Bluetooth: Desconectado"
            tvBluetoothDevice.text = "Sin dispositivo"
            tvBluetoothStatus.setTextColor(Color.parseColor("#999999"))
            btnBluetooth.text = "Conectar"
            btnBluetooth.setBackgroundColor(Color.parseColor("#2196F3"))
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!useBluetoothMode) {
            event?.let {
                if (it.sensor.type == Sensor.TYPE_PROXIMITY) {
                    val distance = it.values[0]
                    tvDistance.text = String.format("%.2f", distance)
                    updateLeds(distance)
                    updateStatistics(distance)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun updateLeds(distance: Float) {
        turnOffAllLeds()

        when {
            distance < redThreshold -> {
                ledRed.setBackgroundColor(COLOR_RED_ON)
                tvStatus.text = "ALERTA: Objeto muy cerca"
                tvStatus.setTextColor(COLOR_RED_ON)
            }
            distance < yellowThreshold -> {
                ledYellow.setBackgroundColor(COLOR_YELLOW_ON)
                tvStatus.text = "Precaución: Objeto a distancia media"
                tvStatus.setTextColor(Color.parseColor("#FFA000"))
            }
            else -> {
                ledBlue.setBackgroundColor(COLOR_BLUE_ON)
                tvStatus.text = "Seguro: Objeto lejos"
                tvStatus.setTextColor(COLOR_BLUE_ON)
            }
        }
    }

    private fun turnOffAllLeds() {
        ledRed.setBackgroundColor(COLOR_LED_OFF)
        ledYellow.setBackgroundColor(COLOR_LED_OFF)
        ledBlue.setBackgroundColor(COLOR_LED_OFF)
    }
}