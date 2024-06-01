package com.example.myapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
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

        // Setup button click listener to update alarm values
        findViewById<Button>(R.id.updateAlarmsButton).setOnClickListener {
            updateAlarmValues()
        }

        // Setup button click listener to update controller values
        findViewById<Button>(R.id.updateControllerButton).setOnClickListener {
            updateControllerValues()
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
                val photoresistorLower = dataSnapshot.child("Photoresistor/Lower").getValue(Int::class.java)
                val photoresistorUpper = dataSnapshot.child("Photoresistor/Upper").getValue(Int::class.java)
                sonarLower = dataSnapshot.child("Sonar/Lower").getValue(Float::class.java)!!
                sonarUpper = dataSnapshot.child("Sonar/Upper").getValue(Float::class.java)!!
                val temperatureLower = dataSnapshot.child("Temperature/Lower").getValue(Int::class.java)
                val temperatureUpper = dataSnapshot.child("Temperature/Upper").getValue(Int::class.java)

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
                findViewById<EditText>(R.id.acOnEditText).setText(acOn.toString())
                findViewById<EditText>(R.id.acValueEditText).setText(acValue.toString())
                findViewById<EditText>(R.id.doorOpenEditText).setText(doorOpen.toString())
                findViewById<EditText>(R.id.lightOnEditText).setText(lightOn.toString())
                findViewById<EditText>(R.id.lightValueEditText).setText(lightValue.toString())
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("MainActivity", "Failed to read values", databaseError.toException())
            }
        })
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
                if (sonar!! !in sonarLower..sonarUpper) {
                    showNotification("Sonar Out of Range", "The sonar value is out of acceptable range.")
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("MainActivity", "Failed to read values", databaseError.toException())
            }
        })
    }

    private fun updateAlarmValues() {
        // Retrieve new values from EditText fields or any other input method
        val newPhotoresistorLower = findViewById<EditText>(R.id.photoresistorLowerEditText).text.toString().toInt()
        val newPhotoresistorUpper = findViewById<EditText>(R.id.photoresistorUpperEditText).text.toString().toInt()
        val newSonarLower = findViewById<EditText>(R.id.sonarLowerEditText).text.toString().toInt()
        val newSonarUpper = findViewById<EditText>(R.id.sonarUpperEditText).text.toString().toInt()
        val newTemperatureLower = findViewById<EditText>(R.id.temperatureLowerEditText).text.toString().toInt()
        val newTemperatureUpper = findViewById<EditText>(R.id.temperatureUpperEditText).text.toString().toInt()

        // Update values in Firebase Database
        alarmsRef.child("Photoresistor/Lower").setValue(newPhotoresistorLower)
        alarmsRef.child("Photoresistor/Upper").setValue(newPhotoresistorUpper)
        alarmsRef.child("Sonar/Lower").setValue(newSonarLower)
        alarmsRef.child("Sonar/Upper").setValue(newSonarUpper)
        alarmsRef.child("Temperature/Lower").setValue(newTemperatureLower)
        alarmsRef.child("Temperature/Upper").setValue(newTemperatureUpper)

        // Log the updated values
        Log.d("MainActivity", "Updated Photoresistor Lower: $newPhotoresistorLower, Upper: $newPhotoresistorUpper")
        Log.d("MainActivity", "Updated Sonar Lower: $newSonarLower, Upper: $newSonarUpper")
        Log.d("MainActivity", "Updated Temperature Lower: $newTemperatureLower, Upper: $newTemperatureUpper")
    }

    private fun updateControllerValues() {
        // Retrieve new values from EditText fields or any other input method
        val newACOn = findViewById<EditText>(R.id.acOnEditText).text.toString().toBoolean()
        val newACValue = findViewById<EditText>(R.id.acValueEditText).text.toString().toInt()
        val newDoorOpen = findViewById<EditText>(R.id.doorOpenEditText).text.toString().toBoolean()
        val newLightOn = findViewById<EditText>(R.id.lightOnEditText).text.toString().toBoolean()
        val newLightValue = findViewById<EditText>(R.id.lightValueEditText).text.toString().toInt()

        // Update values in Firebase Database
        controllerRef.child("ACOn").setValue(newACOn)
        controllerRef.child("ACValue").setValue(newACValue)
        controllerRef.child("DoorOpen").setValue(newDoorOpen)
        controllerRef.child("LightOn").setValue(newLightOn)
        controllerRef.child("LightValue").setValue(newLightValue)

        // Log the updated values
        Log.d("MainActivity", "Updated AC On: $newACOn, AC Value: $newACValue")
        Log.d("MainActivity", "Updated Door Open: $newDoorOpen")
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