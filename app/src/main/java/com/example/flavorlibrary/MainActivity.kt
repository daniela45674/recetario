package com.example.flavorlibrary

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var dbHelper: UserDatabaseHelper

    private var isRegisterMode = false
    private var authDialog: AlertDialog? = null
    private var pendingAction: (() -> Unit)? = null   // acción que esperaba al login

    private val authenticators =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = UserDatabaseHelper(this)
        executor = ContextCompat.getMainExecutor(this)

        setupBiometricPrompt()
        setupBottomNavigation()

        if (savedInstanceState == null) showTab(HomeFragment())

        // El catálogo es público: solo se pide acceso rápido si ya hubo una cuenta en el dispositivo.
        val prefs = getSharedPreferences("FlavorPrefs", Context.MODE_PRIVATE)
        if (prefs.getString("active_email", null) != null &&
            BiometricManager.from(this).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
        ) {
            biometricPrompt.authenticate(promptInfo)
        }
    }

    // ---------------------------------------------------------------- Navegación

    private fun setupBottomNavigation() {
        findViewById<BottomNavigationView>(R.id.bottom_navigation).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> showTab(HomeFragment())
                R.id.nav_favoritos -> showTab(FavoritesFragment())
                R.id.nav_comentarios -> showTab(CommentsFragment())
                R.id.nav_perfil -> showTab(ProfileFragment())
                else -> return@setOnItemSelectedListener false
            }
            true
        }
    }

    private fun showTab(fragment: Fragment) {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    fun openRecipe(id: Int) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, RecipeDetailFragment.newInstance(id))
            .addToBackStack(null)
            .commit()
    }

    fun openUpload() {
        requireLogin {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, UploadRecipeFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    // ---------------------------------------------------------------- Sesión

    /** Ejecuta [action] si hay sesión; si no, abre el login y la ejecuta tras entrar. */
    fun requireLogin(action: () -> Unit) {
        if (SessionManager.isLoggedIn) action()
        else {
            pendingAction = action
            val hasAccount = getSharedPreferences("FlavorPrefs", Context.MODE_PRIVATE)
                .getString("active_email", null) != null
            showAuthDialog(startInRegister = !hasAccount)
        }
    }

    fun logout() {
        SessionManager.logout()
        // Se conserva "active_email" para poder volver a entrar con huella.
        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
        refreshCurrent()
    }

    private fun refreshCurrent() {
        (supportFragmentManager.findFragmentById(R.id.fragment_container) as? Refreshable)?.refresh()
    }

    private fun onLoginSuccess(userName: String, email: String) {
        SessionManager.login(userName, email)
        Toast.makeText(this, "Bienvenido, $userName", Toast.LENGTH_SHORT).show()
        refreshCurrent()
        pendingAction?.invoke()
        pendingAction = null
    }

    // ---------------------------------------------------------------- Biometría

    private fun setupBiometricPrompt() {
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        showAuthDialog(startInRegister = false)
                    } else {
                        Toast.makeText(applicationContext, "Verificación cancelada: $errString", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    val prefs = getSharedPreferences("FlavorPrefs", Context.MODE_PRIVATE)
                    val email = prefs.getString("active_email", "") ?: ""
                    val userName = dbHelper.getUserName(email)
                    Toast.makeText(applicationContext, "¡Huella reconocida! Bienvenido de nuevo, $userName", Toast.LENGTH_SHORT).show()
                    onLoginSuccess(userName, email)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Huella no reconocida. Intenta de nuevo.", Toast.LENGTH_SHORT).show()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Flavor Library")
            .setSubtitle("Acceso rápido por huella dactilar")
            .setNegativeButtonText("Usar contraseña")
            .setAllowedAuthenticators(authenticators)
            .build()
    }

    // ---------------------------------------------------------------- Diálogo de login / registro

    private fun showAuthDialog(startInRegister: Boolean) {
        authDialog?.dismiss()
        isRegisterMode = startInRegister

        val dialogView = layoutInflater.inflate(R.layout.dialog_login, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_title)
        val etName = dialogView.findViewById<EditText>(R.id.et_name)
        val etEmail = dialogView.findViewById<EditText>(R.id.et_email)
        val etPassword = dialogView.findViewById<EditText>(R.id.et_password)
        val btnMainAction = dialogView.findViewById<Button>(R.id.btn_main_action)
        val btnFingerprint = dialogView.findViewById<Button>(R.id.btn_fingerprint)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tv_message)
        val tvToggleMode = dialogView.findViewById<TextView>(R.id.tv_toggle_mode)
        val prefs = getSharedPreferences("FlavorPrefs", Context.MODE_PRIVATE)

        fun updateUI() {
            if (isRegisterMode) {
                tvTitle.text = "Crear una cuenta"
                etName.visibility = View.VISIBLE
                btnMainAction.text = "Registrarse"
                btnFingerprint.visibility = View.GONE
                tvToggleMode.text = "¿Ya tienes cuenta? Inicia sesión"
            } else {
                tvTitle.text = "Iniciar Sesión"
                etName.visibility = View.GONE
                btnMainAction.text = "Entrar con contraseña"
                btnFingerprint.visibility = View.VISIBLE
                tvToggleMode.text = "¿No tienes cuenta? Regístrate"
                val lastEmail = prefs.getString("active_email", "")
                if (!lastEmail.isNullOrEmpty()) etEmail.setText(lastEmail)
            }
            tvMessage.text = ""
        }
        updateUI()

        // Cancelable: el usuario puede cerrar el diálogo y seguir viendo el catálogo.
        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .setOnCancelListener { pendingAction = null }
            .create()
        authDialog = alertDialog

        tvToggleMode.setOnClickListener { isRegisterMode = !isRegisterMode; updateUI() }

        btnMainAction.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val name = etName.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || (isRegisterMode && name.isEmpty())) {
                tvMessage.text = "Por favor, completa todos los campos."
                return@setOnClickListener
            }

            if (isRegisterMode) {
                if (dbHelper.isEmailRegistered(email)) {
                    tvMessage.text = "Este correo ya está registrado."
                    return@setOnClickListener
                }
                if (dbHelper.registerUser(name, email, password)) {
                    prefs.edit().putString("active_email", email).apply()
                    alertDialog.dismiss()
                    Toast.makeText(this, "¡Estás registrado! Tu cuenta se guardó y tu huella quedó vinculada.", Toast.LENGTH_LONG).show()
                    onLoginSuccess(name, email)
                } else tvMessage.text = "Error al registrar la cuenta."
            } else {
                if (dbHelper.validateUser(email, password)) {
                    prefs.edit().putString("active_email", email).apply()
                    alertDialog.dismiss()
                    onLoginSuccess(dbHelper.getUserName(email), email)
                } else tvMessage.text = "Correo o contraseña incorrectos."
            }
        }

        btnFingerprint.setOnClickListener {
            val lastEmail = prefs.getString("active_email", null)
            if (lastEmail.isNullOrEmpty()) {
                tvMessage.text = "No hay ninguna cuenta previa en este dispositivo. Regístrate primero."
                return@setOnClickListener
            }
            when (BiometricManager.from(this).canAuthenticate(authenticators)) {
                BiometricManager.BIOMETRIC_SUCCESS -> { alertDialog.dismiss(); biometricPrompt.authenticate(promptInfo) }
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> tvMessage.text = "El dispositivo no cuenta con sensor biométrico."
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> tvMessage.text = "El sensor biométrico no está disponible."
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> tvMessage.text = "No hay huellas registradas en el dispositivo."
                else -> tvMessage.text = "El dispositivo no admite biometría."
            }
        }
        alertDialog.show()
    }
}
