package com.example.megustalacasuela

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class adminactivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var btnBack: Button
    private lateinit var etRangeRed: EditText
    private lateinit var etRangeYellow: EditText
    private lateinit var btnApplySettings: Button
    private lateinit var btnResetSettings: Button
    private lateinit var btnResetStats: Button
    private lateinit var tvMeasurementCount: TextView
    private lateinit var tvMinDistance: TextView
    private lateinit var tvMaxDistance: TextView
    private lateinit var tvSensorInfo: TextView
    private lateinit var tvBluetoothInfo: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adminactivity)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        sharedPreferences = getSharedPreferences("SensorSettings", Context.MODE_PRIVATE)


        initViews()


        loadSettings()


        loadStatistics()


        loadSystemInfo()


        setupListeners()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        etRangeRed = findViewById(R.id.etRangeRed)
        etRangeYellow = findViewById(R.id.etRangeYellow)
        btnApplySettings = findViewById(R.id.btnApplySettings)
        btnResetSettings = findViewById(R.id.btnResetSettings)
        btnResetStats = findViewById(R.id.btnResetStats)
        tvMeasurementCount = findViewById(R.id.tvMeasurementCount)
        tvMinDistance = findViewById(R.id.tvMinDistance)
        tvMaxDistance = findViewById(R.id.tvMaxDistance)
        tvSensorInfo = findViewById(R.id.tvSensorInfo)
        tvBluetoothInfo = findViewById(R.id.tvBluetoothInfo)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnApplySettings.setOnClickListener {
            applySettings()
        }

        btnResetSettings.setOnClickListener {
            resetSettings()
        }

        btnResetStats.setOnClickListener {
            resetStatistics()
        }
    }

    private fun loadSettings() {
        val redThreshold = sharedPreferences.getFloat("redThreshold", 0.5f)
        val yellowThreshold = sharedPreferences.getFloat("yellowThreshold", 1.5f)

        etRangeRed.setText(redThreshold.toString())
        etRangeYellow.setText(yellowThreshold.toString())
    }

    private fun applySettings() {
        try {
            val redValue = etRangeRed.text.toString().toFloatOrNull()
            val yellowValue = etRangeYellow.text.toString().toFloatOrNull()

            if (redValue == null || yellowValue == null) {
                Toast.makeText(this, "Por favor ingrese valores válidos", Toast.LENGTH_SHORT).show()
                return
            }

            if (redValue <= 0 || yellowValue <= redValue) {
                Toast.makeText(this, "Valores inválidos. El valor amarillo debe ser mayor que el rojo", Toast.LENGTH_LONG).show()
                return
            }


            val editor = sharedPreferences.edit()
            editor.putFloat("redThreshold", redValue)
            editor.putFloat("yellowThreshold", yellowValue)
            editor.apply()

            Toast.makeText(this, "✓ Configuración guardada correctamente", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Error al guardar configuración", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetSettings() {
        etRangeRed.setText("0.5")
        etRangeYellow.setText("1.5")

        val editor = sharedPreferences.edit()
        editor.putFloat("redThreshold", 0.5f)
        editor.putFloat("yellowThreshold", 1.5f)
        editor.apply()

        Toast.makeText(this, "↺ Configuración restaurada a valores predeterminados", Toast.LENGTH_SHORT).show()
    }

    private fun loadStatistics() {
        val measurementCount = sharedPreferences.getInt("measurementCount", 0)
        val minDistance = sharedPreferences.getFloat("minDistance", Float.MAX_VALUE)
        val maxDistance = sharedPreferences.getFloat("maxDistance", 0f)

        tvMeasurementCount.text = measurementCount.toString()
        tvMinDistance.text = if (minDistance == Float.MAX_VALUE) "-- m" else String.format("%.2f m", minDistance)
        tvMaxDistance.text = if (maxDistance == 0f) "-- m" else String.format("%.2f m", maxDistance)
    }

    private fun resetStatistics() {
        val editor = sharedPreferences.edit()
        editor.putInt("measurementCount", 0)
        editor.putFloat("minDistance", Float.MAX_VALUE)
        editor.putFloat("maxDistance", 0f)
        editor.apply()

        loadStatistics()

        Toast.makeText(this, "🗑️ Estadísticas limpiadas", Toast.LENGTH_SHORT).show()
    }

    private fun loadSystemInfo() {
        // Verificar sensor de proximidad
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        if (proximitySensor != null) {
            tvSensorInfo.text = "✓ Sensor de Proximidad Activo"
            tvSensorInfo.setTextColor(getColor(android.R.color.holo_green_dark))
        } else {
            tvSensorInfo.text = "✗ Sensor no disponible"
            tvSensorInfo.setTextColor(getColor(android.R.color.holo_red_dark))
        }


        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter != null) {
            if (bluetoothAdapter.isEnabled) {
                tvBluetoothInfo.text = "✓ Bluetooth Activo y Disponible"
                tvBluetoothInfo.setTextColor(getColor(android.R.color.holo_green_dark))
            } else {
                tvBluetoothInfo.text = "⚠ Bluetooth Disponible (Desactivado)"
                tvBluetoothInfo.setTextColor(getColor(android.R.color.holo_orange_dark))
            }
        } else {
            tvBluetoothInfo.text = "✗ Bluetooth No Disponible"
            tvBluetoothInfo.setTextColor(getColor(android.R.color.holo_red_dark))
        }
    }

    override fun onResume() {
        super.onResume()

        loadStatistics()
        loadSystemInfo()
    }
}