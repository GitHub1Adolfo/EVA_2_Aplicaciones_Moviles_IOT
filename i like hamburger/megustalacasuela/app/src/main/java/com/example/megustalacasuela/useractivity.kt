package com.example.megustalacasuela

import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class useractivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var proximitySensor: Sensor? = null

    private lateinit var tvDistance: TextView
    private lateinit var tvStatus: TextView
    private lateinit var ledRed: View
    private lateinit var ledYellow: View
    private lateinit var ledBlue: View

    // Colores para los LEDs
    private val COLOR_LED_OFF = Color.parseColor("#EEEEEE")
    private val COLOR_RED_ON = Color.parseColor("#FF0000")
    private val COLOR_YELLOW_ON = Color.parseColor("#FFFF00")
    private val COLOR_BLUE_ON = Color.parseColor("#0000FF")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_useractivity)

        // Inicializar vistas
        tvDistance = findViewById(R.id.tvDistance)
        tvStatus = findViewById(R.id.tvStatus)
        ledRed = findViewById(R.id.ledRed)
        ledYellow = findViewById(R.id.ledYellow)
        ledBlue = findViewById(R.id.ledBlue)

        // Inicializar sensor manager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Obtener el sensor de proximidad
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        if (proximitySensor == null) {
            tvStatus.text = "Sensor de proximidad no disponible"
            tvStatus.setTextColor(Color.RED)
        } else {
            tvStatus.text = "Sensor Activo"
            tvStatus.setTextColor(Color.parseColor("#4CAF50"))
        }

        // Apagar todos los LEDs al inicio
        turnOffAllLeds()
    }

    override fun onResume() {
        super.onResume()
        // Registrar el listener del sensor
        proximitySensor?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        // Desregistrar el listener para ahorrar batería
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_PROXIMITY) {
                val distance = it.values[0]

                // Actualizar el texto de distancia
                tvDistance.text = String.format("%.2f", distance)

                // Controlar los LEDs según la distancia
                updateLeds(distance)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No es necesario implementar para este caso
    }

    private fun updateLeds(distance: Float) {
        // Apagar todos los LEDs primero
        turnOffAllLeds()

        // Encender el LED correspondiente según la distancia
        when {
            distance < 0.5f -> {
                // Muy cerca - LED ROJO
                ledRed.setBackgroundColor(COLOR_RED_ON)
                tvStatus.text = "ALERTA: Objeto muy cerca"
                tvStatus.setTextColor(COLOR_RED_ON)
            }
            distance in 0.5f..1.5f -> {
                // Distancia media - LED AMARILLO
                ledYellow.setBackgroundColor(COLOR_YELLOW_ON)
                tvStatus.text = "Precaución: Objeto a distancia media"
                tvStatus.setTextColor(Color.parseColor("#FFA000"))
            }
            else -> {
                // Lejos - LED AZUL
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