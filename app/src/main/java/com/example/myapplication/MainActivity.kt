package com.example.myapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.google.firebase.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.Float.Companion.MAX_VALUE
import kotlin.Float.Companion.MIN_VALUE

class MainActivity : AppCompatActivity() {

    private lateinit var alarmsRef: DatabaseReference
    private lateinit var controllerRef: DatabaseReference
    private lateinit var sensorRef: DatabaseReference
    var sonarLower = MIN_VALUE;
    var sonarUpper = MAX_VALUE;
    var photoresistorLower = MIN_VALUE;
    var photoresistorUpper = MAX_VALUE;
    var temperatureLower = MIN_VALUE;
    var temperatureUpper = MAX_VALUE;
    private var sonarNotificationShown = false
    private var photoresistorNotificationShown = false
    private var temperatureNotificationShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Initialize Firebase Database references
        val database = FirebaseDatabase.getInstance()
        alarmsRef = database.getReference("Alarms")
        controllerRef = database.getReference("Room/Controller")
        sensorRef = database.getReference("Room/Sensor")

        // Retrieve initial values from Firebase Database and display them
        retrieveAlarmValues()
        retrieveControllerValues()
        retrieveSensorValuesContinuously()

        // Setup button click listeners to update alarm values
        findViewById<Button>(R.id.updatePhotoresistorButton).setOnClickListener {
            updatePhotoresistorValues()
        }
        findViewById<Button>(R.id.updateSonarButton).setOnClickListener {
            updateSonarValues()
        }
        findViewById<Button>(R.id.updateTemperatureButton).setOnClickListener {
            updateTemperatureValues()
        }

        // Setup button click listeners to update controller values
        findViewById<Button>(R.id.updateAcButton).setOnClickListener {
            updateACValues()
        }
        findViewById<Button>(R.id.updateDoorButton).setOnClickListener {
            updateDoorValues()
        }
        findViewById<Button>(R.id.updateLightButton).setOnClickListener {
            updateLightValues()
        }

        setupSpinner(R.id.acOnSpinner)
        setupSpinner(R.id.lightOnSpinner)
        setupSpinner(R.id.doorOnSpinner)
    }

    private fun setupSpinner(spinnerId: Int) {
        val spinner: Spinner = findViewById(spinnerId)
        ArrayAdapter.createFromResource(
            this,
            R.array.boolean_values,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }
    }


    private fun retrieveSensorValuesContinuously() {
        GlobalScope.launch {
            while (true) {
                retrieveSensorValues()
                delay(2000) // Fetch values every 2 seconds
            }
        }
    }

    private fun retrieveAlarmValues() {
        alarmsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                photoresistorLower = dataSnapshot.child("Photoresistor/Lower").getValue(Float::class.java)!!
                photoresistorUpper = dataSnapshot.child("Photoresistor/Upper").getValue(Float::class.java)!!
                sonarLower = dataSnapshot.child("Sonar/Lower").getValue(Float::class.java)!!
                sonarUpper = dataSnapshot.child("Sonar/Upper").getValue(Float::class.java)!!
                temperatureLower = dataSnapshot.child("Temperature/Lower").getValue(Float::class.java)!!
                temperatureUpper = dataSnapshot.child("Temperature/Upper").getValue(Float::class.java)!!

                // Log the retrieved values
                Log.d("MainActivity", "Photoresistor Lower: $photoresistorLower, Upper: $photoresistorUpper")
                Log.d("MainActivity", "Sonar Lower: $sonarLower, Upper: $sonarUpper")
                Log.d("MainActivity", "Temperature Lower: $temperatureLower, Upper: $temperatureUpper")

                // Display retrieved values in EditText fields
                findViewById<EditText>(R.id.photoresistorLowerEditText).setText(photoresistorLower.toString())
                findViewById<EditText>(R.id.photoresistorUpperEditText).setText(photoresistorUpper.toString())
                findViewById<EditText>(R.id.sonarLowerEditText).setText(sonarLower.toString())
                findViewById<EditText>(R.id.sonarUpperEditText).setText(sonarUpper.toString())
                findViewById<EditText>(R.id.temperatureLowerEditText).setText(temperatureLower.toString())
                findViewById<EditText>(R.id.temperatureUpperEditText).setText(temperatureUpper.toString())
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("MainActivity", "Failed to read values", databaseError.toException())
            }
        })
    }

    private fun retrieveControllerValues() {
        controllerRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val acOn = dataSnapshot.child("ACOn").getValue(Boolean::class.java)
                val acValue = dataSnapshot.child("ACValue").getValue(Int::class.java)
                val doorOpen = dataSnapshot.child("DoorOpen").getValue(Boolean::class.java)
                val lightOn = dataSnapshot.child("LightOn").getValue(Boolean::class.java)
                val lightValue = dataSnapshot.child("LightValue").getValue(Int::class.java)

                // Log the retrieved values
                Log.d("MainActivity", "AC On: $acOn, AC Value: $acValue")
                Log.d("MainActivity", "Door Open: $doorOpen")
                Log.d("MainActivity", "Light On: $lightOn, Light Value: $lightValue")

                // Display retrieved values in EditText fields
                setSpinnerSelection(R.id.acOnSpinner, acOn)
                setSpinnerSelection(R.id.lightOnSpinner, lightOn)
                setSpinnerSelection(R.id.doorOnSpinner, doorOpen)

                findViewById<EditText>(R.id.acValueEditText).setText(acValue.toString())
                findViewById<EditText>(R.id.lightValueEditText).setText(lightValue.toString())
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("MainActivity", "Failed to read values", databaseError.toException())
            }
        })
    }

    private fun setSpinnerSelection(spinnerId: Int, booleanValue: Boolean?) {
        val spinner: Spinner = findViewById(spinnerId)
        val value = if (booleanValue == true) "true" else "false"
        val position = (spinner.adapter as ArrayAdapter<String>).getPosition(value)
        spinner.setSelection(position)
    }

    private fun retrieveSensorValues() {
        sensorRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val door = dataSnapshot.child("Door").getValue(Boolean::class.java)
                val photoresistor = dataSnapshot.child("Photoresistor").getValue(Int::class.java)
                val sonar = dataSnapshot.child("Sonar").getValue(Float::class.java)
                val temperature = dataSnapshot.child("Temperature").getValue(Double::class.java)

                findViewById<TextView>(R.id.doorSensorTextView).text = "Door: $door"
                findViewById<TextView>(R.id.photoresistorSensorTextView).text = "Photoresistor: $photoresistor"
                findViewById<TextView>(R.id.sonarSensorTextView).text = "Sonar: $sonar"
                findViewById<TextView>(R.id.temperatureSensorTextView).text = "Temperature: $temperature"

                // Log the retrieved values
                Log.d("MainActivity", "Sensor Door: $door")
                Log.d("MainActivity", "Sensor Photoresistor: $photoresistor")
                Log.d("MainActivity", "Sensor Sonar: $sonar")
                Log.d("MainActivity", "Sensor Temperature: $temperature")

                // Check sonar range
                if (sonar != null && sonar !in sonarLower..sonarUpper) {
                    if (!sonarNotificationShown) {
                        showNotification("Sonar Out of Range", "The sonar value is out of acceptable range.")
                        sonarNotificationShown = true
                    }
                } else {
                    sonarNotificationShown = false
                }

                // Check photoresistor range
                if (photoresistor != null && (photoresistor < photoresistorLower || photoresistor > photoresistorUpper)) {
                    if (!photoresistorNotificationShown) {
                        showNotification("Photoresistor Out of Range", "The photoresistor value is out of acceptable range.")
                        photoresistorNotificationShown = true
                    }
                } else {
                    photoresistorNotificationShown = false
                }

                // Check temperature range
                if (temperature != null && (temperature < temperatureLower || temperature > temperatureUpper)) {
                    if (!temperatureNotificationShown) {
                        showNotification("Temperature Out of Range", "The temperature value is out of acceptable range.")
                        temperatureNotificationShown = true
                    }
                } else {
                    temperatureNotificationShown = false
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("MainActivity", "Failed to read values", databaseError.toException())
            }
        })
    }

    private fun updatePhotoresistorValues() {
        // Retrieve new values from EditText fields or any other input method
        val newPhotoresistorLower = findViewById<EditText>(R.id.photoresistorLowerEditText).text.toString().toFloat()
        val newPhotoresistorUpper = findViewById<EditText>(R.id.photoresistorUpperEditText).text.toString().toFloat()

        // Update values in Firebase Database
        alarmsRef.child("Photoresistor/Lower").setValue(newPhotoresistorLower)
        alarmsRef.child("Photoresistor/Upper").setValue(newPhotoresistorUpper)

        // Log the updated values
        Log.d("MainActivity", "Updated Photoresistor Lower: $newPhotoresistorLower, Upper: $newPhotoresistorUpper")
    }

    private fun updateSonarValues() {
        // Retrieve new values from EditText fields or any other input method
        val newSonarLower = findViewById<EditText>(R.id.sonarLowerEditText).text.toString().toFloat()
        val newSonarUpper = findViewById<EditText>(R.id.sonarUpperEditText).text.toString().toFloat()

        // Update values in Firebase Database
        alarmsRef.child("Sonar/Lower").setValue(newSonarLower)
        alarmsRef.child("Sonar/Upper").setValue(newSonarUpper)

        // Log the updated values
        Log.d("MainActivity", "Updated Sonar Lower: $newSonarLower, Upper: $newSonarUpper")
    }

    private fun updateTemperatureValues() {
        // Retrieve new values from EditText fields or any other input method
        val newTemperatureLower = findViewById<EditText>(R.id.temperatureLowerEditText).text.toString().toFloat()
        val newTemperatureUpper = findViewById<EditText>(R.id.temperatureUpperEditText).text.toString().toFloat()

        // Update values in Firebase Database
        alarmsRef.child("Temperature/Lower").setValue(newTemperatureLower)
        alarmsRef.child("Temperature/Upper").setValue(newTemperatureUpper)

        // Log the updated values
        Log.d("MainActivity", "Updated Temperature Lower: $newTemperatureLower, Upper: $newTemperatureUpper")
    }

    private fun updateACValues() {
        // Retrieve new values from EditText fields or any other input method
        val acOnSpinner: Spinner = findViewById(R.id.acOnSpinner)
        val newACOn = acOnSpinner.selectedItem.toString().toBoolean()
        val newACValue = findViewById<EditText>(R.id.acValueEditText).text.toString().toFloat()

        // Update values in Firebase Database
        controllerRef.child("ACOn").setValue(newACOn)
        controllerRef.child("ACValue").setValue(newACValue)

        // Log the updated values
        Log.d("MainActivity", "Updated AC On: $newACOn, AC Value: $newACValue")
    }

    private fun updateDoorValues() {
        // Retrieve new values from EditText fields or any other input method
        val doorOnSpinner: Spinner = findViewById(R.id.doorOnSpinner)
        val newDoorOpen = doorOnSpinner.selectedItem.toString().toBoolean()

        // Update values in Firebase Database
        controllerRef.child("DoorOpen").setValue(newDoorOpen)

        // Log the updated values
        Log.d("MainActivity", "Updated Door Open: $newDoorOpen")
    }

    private fun updateLightValues() {
        // Retrieve new values from EditText fields or any other input method
        val lightOnSpinner: Spinner = findViewById(R.id.lightOnSpinner)
        val newLightOn = lightOnSpinner.selectedItem.toString().toBoolean()
        val newLightValue = findViewById<EditText>(R.id.lightValueEditText).text.toString().toFloat()

        // Update values in Firebase Database
        controllerRef.child("LightOn").setValue(newLightOn)
        controllerRef.child("LightValue").setValue(newLightValue)

        // Log the updated values
        Log.d("MainActivity", "Updated Light On: $newLightOn, Light Value: $newLightValue")
    }

    private fun showNotification(title: String, message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Notification channel ID is required for Android O and above
        val channelId = "your_channel_id"
        val channelName = "Your Channel Name"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(0, notificationBuilder.build())
    }
}
