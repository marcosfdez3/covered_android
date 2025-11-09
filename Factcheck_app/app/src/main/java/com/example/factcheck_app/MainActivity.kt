package com.example.factcheckapp

import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import androidx.activity.addCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import com.example.factcheckapp.databinding.ActivityMainBinding
import com.example.factcheckapp.models.HistoryItem
import com.example.factcheckapp.network.RetrofitInstance
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.SharedPreferences
import android.content.Intent
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificar si necesita mostrar login
        sharedPreferences = getSharedPreferences("CoveredPrefs", MODE_PRIVATE)
        val shouldShowLogin = !sharedPreferences.getBoolean("skipped_login", false) &&
                !sharedPreferences.getBoolean("user_logged_in", false) &&
                Firebase.auth.currentUser == null

        if (shouldShowLogin) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { view, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.setPadding(
                view.paddingLeft,
                statusBarHeight,
                view.paddingRight,
                view.paddingBottom
            )
            insets
        }

        setupNavigationDrawer()
        loadInitialFragment()
        cargarHistorialReal()
        setupLoginClickListener()
        onBackPressedDispatcher.addCallback(this) {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                finish()
            }
        }
    }

    private fun setupLoginClickListener() {
        val navView = findViewById<NavigationView>(R.id.nav_view)
        val headerView = navView.getHeaderView(0)
        val btnLogin = headerView.findViewById<Button>(R.id.btnAuth)

        val isLogged = sharedPreferences.getBoolean("user_logged_in", false) ||
                sharedPreferences.getBoolean("skipped_login", false) ||
                Firebase.auth.currentUser != null

        if (isLogged) {
            btnLogin.text = "Cerrar Sesión"
            btnLogin.setOnClickListener {
                // Limpiar preferencias y cerrar sesión
                sharedPreferences.edit().clear().apply()
                Firebase.auth.signOut()

                // Recargar la app
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        } else {
            btnLogin.text = "Iniciar Sesión"
            btnLogin.setOnClickListener {
                startActivity(Intent(this, LoginActivity::class.java))
            }
        }

        // Hacer clickable todo el header
        headerView.setOnClickListener {
            // Opcional: acción al hacer click en el header
        }
    }

    private fun setupNavigationDrawer() {
        setSupportActionBar(binding.toolbar)

        toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        toggle.drawerArrowDrawable.color = getColor(android.R.color.black)

        binding.navView.setNavigationItemSelectedListener { menuItem ->
            handleNavigationItemSelected(menuItem)
        }

        // Recargar historial cuando se abre el drawer
        binding.drawerLayout.addDrawerListener(object : androidx.drawerlayout.widget.DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: android.view.View, slideOffset: Float) {}
            override fun onDrawerStateChanged(newState: Int) {}
            override fun onDrawerClosed(drawerView: android.view.View) {}
            override fun onDrawerOpened(drawerView: android.view.View) {
                cargarHistorialReal()
            }
        })
    }

    private fun cargarHistorialReal() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val historialResponse = RetrofitInstance.api.obtenerHistorial(limit = 5)

                withContext(Dispatchers.Main) {
                    if (historialResponse.consultas.isNotEmpty()) {
                        actualizarMenuHistorial(historialResponse.consultas)
                    } else {
                        mostrarMenuVacio()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    mostrarErrorHistorial()
                }
            }
        }
    }

    private fun actualizarMenuHistorial(consultasReales: List<HistoryItem>) {
        val navView = findViewById<NavigationView>(R.id.nav_view)
        val menu = navView.menu

        // Limpiar solo los items del historial (dejando los otros menus intactos)
        limpiarItemsHistorial(menu)

        // Buscar el item padre del historial
        val itemHistorial = menu.findItem(R.id.nav_history_header)
        val subMenuHistorial = itemHistorial?.subMenu

        if (subMenuHistorial != null) {
            // Agregar items del historial REAL
            consultasReales.forEach { item ->
                subMenuHistorial.add(crearTextoMenuHistorial(item))?.apply {
                    setOnMenuItemClickListener {
                        mostrarDetalleHistorial(item)
                        binding.drawerLayout.closeDrawer(GravityCompat.START)
                        true
                    }
                }
            }
        }
    }

    private fun limpiarItemsHistorial(menu: android.view.Menu) {
        val itemHistorial = menu.findItem(R.id.nav_history_header)
        val subMenuHistorial = itemHistorial?.subMenu

        subMenuHistorial?.clear()
    }

    private fun crearTextoMenuHistorial(item: HistoryItem): String {
        val textoCorto = if (item.texto.length > 1) {
            "${item.texto.take(60)}..."
        } else {
            item.texto
        }

        // Capitalizar solo la primera letra
        val textoCapitalizado = textoCorto.replaceFirstChar { firstChar ->
            if (firstChar.isLowerCase()) firstChar.titlecase() else firstChar.toString()
        }

        return "$textoCapitalizado"
    }

    private fun mostrarMenuVacio() {
        val navView = findViewById<NavigationView>(R.id.nav_view)
        val menu = navView.menu
        val subMenuHistorial = menu.findItem(R.id.nav_history_header)?.subMenu

        subMenuHistorial?.clear()
        subMenuHistorial?.add("No hay verificaciones")?.apply {
            isEnabled = false
        }
    }

    private fun mostrarErrorHistorial() {
        val navView = findViewById<NavigationView>(R.id.nav_view)
        val menu = navView.menu
        val subMenuHistorial = menu.findItem(R.id.nav_history_header)?.subMenu

        subMenuHistorial?.clear()
        subMenuHistorial?.add("Error al cargar")?.apply {
            isEnabled = false
        }

        subMenuHistorial?.add("Reintentar")?.apply {
            setOnMenuItemClickListener {
                cargarHistorialReal()
                true
            }
        }
    }

    private fun mostrarDetalleHistorial(item: HistoryItem) {
        val mensaje = """
        📝 ${item.texto}
        
        🔍 Resultado: ${item.resultado.replace("_", " ").replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }}
        📅 Fecha: ${formatearFecha(item.fecha)}
        ${if (!item.url.isNullOrEmpty()) "🔗 URL: ${item.url}" else ""}
    """.trimIndent()

        // Mostrar en un AlertDialog más elegante
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Detalle de verificación")
            .setMessage(mensaje)
            .setPositiveButton("Aceptar") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun formatearFecha(fecha: String): String {
        return try {
            // Formato ISO: "2024-01-15T12:30:45" → "15/01/2024 12:30"
            fecha.replace("T", " ").substring(0, 16).replace("-", "/")
        } catch (e: Exception) {
            fecha
        }
    }

    private fun handleNavigationItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.nav_verification -> {
                loadFragment(VerificationFragment())
            }
            R.id.nav_settings -> {
                // Aquí puedes cargar un fragment de configuración
                showMessage("Configuración - Próximamente")
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun loadInitialFragment() {
        loadFragment(VerificationFragment())
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun showMessage(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    // En MainActivity
    private fun loadUserData() {
        val sharedPreferences = getSharedPreferences("CoveredPrefs", MODE_PRIVATE)
        val userName = sharedPreferences.getString("user_name", "Invitado")

        
    }
}