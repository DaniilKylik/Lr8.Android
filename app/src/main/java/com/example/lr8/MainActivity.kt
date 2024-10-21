package com.example.lr8

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var remoteConfig: FirebaseRemoteConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        // Під’єднати застосунок до Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        remoteConfig = FirebaseRemoteConfig.getInstance()

        val buttonSignIn = findViewById<Button>(R.id.button_sign_in)
        val buttonCrash = findViewById<Button>(R.id.button_crash)
        val buttonChangeTheme = findViewById<Button>(R.id.button_change_theme)

        buttonSignIn.setOnClickListener {
            // Додати можливість авторизації за допомогою власних даних
            signIn("test@example.com", "password123")
        }

        buttonCrash.setOnClickListener {
            // Показати приклад події в CrashLytics
            FirebaseCrashlytics.getInstance().log("Test crash event")
            FirebaseCrashlytics.getInstance().recordException(Exception("Test exception"))
        }

        buttonChangeTheme.setOnClickListener {
            lifecycleScope.launch {
                fetchRemoteConfig() // Оновлюємо конфігурацію та застосовуємо нову тему
            }
        }

        lifecycleScope.launch {
            // Використати RemoteConfig для зміни теми застосунку
            fetchRemoteConfig()

            // Використати FireStore або Realtime, навести основні операції CRUD
            performFirestoreCRUD()
        }
    }

    // Firebase Authentication - вхід
    private fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Authentication successful", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Remote Config - отримання та застосування конфігурації
    private suspend fun fetchRemoteConfig() {
        // Встановлюємо значення за замовчуванням
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults).await()
        // Отримуємо конфігурацію
        val configUpdated = remoteConfig.fetchAndActivate().await()

        if (configUpdated) {
            Toast.makeText(this, "Config updated", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No config changes", Toast.LENGTH_SHORT).show()
        }
        // Отримання булевого значення для теми
        val isDarkModeEnabled = remoteConfig.getBoolean("dark_mode_enabled")
        applyTheme(isDarkModeEnabled)
        // Отримуємо повідомлення та показуємо його
        val welcomeMessage = remoteConfig.getString("welcome_message")
        Toast.makeText(this, welcomeMessage, Toast.LENGTH_LONG).show()
        // Увімкнення функції
        val isFeatureEnabled = remoteConfig.getBoolean("feature_enabled")
        if (isFeatureEnabled) {
            enableFeature()
        } else {
            disableFeature()
        }
        // Змінюємо видимість кнопки
        val isButtonVisible = remoteConfig.getBoolean("button_visibility")
        val button = findViewById<Button>(R.id.button_change_theme_2)

        if (isButtonVisible) {
            button.visibility = View.VISIBLE
        } else {
            button.visibility = View.GONE
        }
        // Отримуємо колір і застосовуємо його
        val customColor = remoteConfig.getString("custom_color")
        val rootView = findViewById<View>(R.id.root_view)
        rootView.setBackgroundColor(Color.parseColor(customColor))
    }

    private fun applyTheme(isDarkModeEnabled: Boolean) {
        if (isDarkModeEnabled) {
            setTheme(R.style.Theme_Dark)
        } else {
            setTheme(R.style.Theme_Light)
        }
    }

    private fun enableFeature() {
        // Код для увімкнення функції
        Toast.makeText(this, "Feature enabled!", Toast.LENGTH_SHORT).show()
    }

    private fun disableFeature() {
        // Код для вимкнення функції
        Toast.makeText(this, "Feature disabled!", Toast.LENGTH_SHORT).show()
    }

    // Firestore CRUD операції
    private suspend fun performFirestoreCRUD() {
        val usersCollection = db.collection("users")
        // Create
        val user = hashMapOf(
            "name" to "John Doe",
            "age" to 30
        )
        usersCollection.add(user).await()
        // Read
        val users = usersCollection.get().await()
        for (document in users) {
            println("User: ${document.data}")
        }
        // Update
        val userId = users.documents[0].id
        usersCollection.document(userId).update("name", "Jane Doe").await()
         // Delete
//         usersCollection.document(userId).delete().await()
    }
}