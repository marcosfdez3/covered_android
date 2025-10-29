package com.example.factcheckapp

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.factcheckapp.databinding.FragmentVerificationBinding
import com.example.factcheckapp.models.Noticia
import com.example.factcheckapp.models.VerificationResult
import com.example.factcheckapp.network.RetrofitInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

class VerificationFragment : Fragment() {

    private var _binding: FragmentVerificationBinding? = null
    private val binding get() = _binding!!

    // Variable para controlar el modo link
    private var isLinkMode = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVerificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        setupEditTextValidation()
    }

    private fun setupClickListeners() {
        // Configurar el botón de toggle link/texto
        binding.buttonLeft.setOnClickListener {
            toggleLinkMode()
        }

        binding.buttonVerify.setOnClickListener {
            verifyText()
        }

        binding.editTextInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                verifyText()
                true
            } else {
                false
            }
        }
    }

    private fun toggleLinkMode() {
        isLinkMode = !isLinkMode

        if (isLinkMode) {
            // Modo link activado
            binding.buttonLeft.isSelected = true
            binding.buttonLeft.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#1977BF"))
            binding.editTextInput.hint = "Pega el link aquí..."
            binding.editTextInput.inputType = InputType.TYPE_TEXT_VARIATION_URI
            binding.editTextInput.text?.clear()

        } else {
            // Modo texto normal
            binding.buttonLeft.isSelected = false
            binding.buttonLeft.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#626E5E"))
            binding.editTextInput.hint = "Verifica con Covered..."
            binding.editTextInput.inputType = InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_CLASS_TEXT
            binding.editTextInput.text?.clear()

        }

        // Forzar el foco en el EditText
        binding.editTextInput.requestFocus()
    }

    private fun setupEditTextValidation() {
        // Listener para validar en tiempo real si está en modo link
        binding.editTextInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isLinkMode && !s.isNullOrEmpty()) {
                    validateLinkFormat(s.toString())
                } else {
                    binding.editTextInput.error = null
                }
            }
        })
    }

    private fun validateLinkFormat(text: String) {
        if (!isValidUrl(text) && text.isNotEmpty()) {
            binding.editTextInput.error = "Formato de URL no válido"
        } else {
            binding.editTextInput.error = null
        }
    }

    private fun verifyText() {
        val inputText = binding.editTextInput.text.toString().trim()

        if (inputText.isEmpty()) {
            showMessage("Por favor ingresa un texto para verificar")
            return
        }

        // Validación adicional si está en modo link
        if (isLinkMode) {
            if (!isValidUrl(inputText)) {
                showMessage("Por favor ingresa una URL válida")
                binding.editTextInput.error = "URL no válida"
                return
            }
        }

        hideKeyboard()

        // Ocultar card anterior si está visible
        if (binding.cardResult.visibility == View.VISIBLE) {
            binding.cardResult.visibility = View.GONE
        }

        // Ocultar información y mostrar progress bar
        binding.layoutInfo.visibility = View.GONE
        binding.progressBarVerificando.visibility = View.VISIBLE
        binding.buttonVerify.isEnabled = false

        // Llamada REAL a la API
        realizarVerificacionReal(inputText)
        binding.editTextInput.text.clear()
    }

    private fun isValidUrl(text: String): Boolean {
        return if (text.startsWith("http://") || text.startsWith("https://")) {
            Patterns.WEB_URL.matcher(text).matches()
        } else {
            // Intentar con https:// por defecto
            Patterns.WEB_URL.matcher("https://$text").matches()
        }
    }

    private fun realizarVerificacionReal(texto: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val noticia = Noticia(
                    texto = texto,
                    usuarioId = "usuario_android",
                    dispositivoId = android.os.Build.MODEL
                )

                val resultado = RetrofitInstance.api.verificarNoticia(noticia)

                withContext(Dispatchers.Main) {
                    mostrarResultadoReal(resultado)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    mostrarError(e.message ?: "Error desconocido")
                }
            }
        }
    }

    private fun mostrarResultadoReal(resultado: VerificationResult) {
        binding.progressBarVerificando.visibility = View.GONE
        binding.buttonVerify.isEnabled = true

        if (resultado.success) {
            val razonamiento = resultado.razonamiento ?: "No se pudo generar un análisis detallado."

            // Configurar el card según el resultado
            configureCardAppearance(resultado)

            val mensajeCompleto = buildString {
                append(razonamiento)
            }

            // Mostrar card con animación
            showCardWithAnimation()

            // Iniciar efecto typewriter
            animateTypewriter(mensajeCompleto, binding.textResult, 35L)

        } else {
            showErrorCard()
        }
    }

    private fun configureCardAppearance(resultado: VerificationResult) {
        val (title, status, cardColor) = when (resultado.resultado) {
            "verificado", "probablemente_verdadero" -> Triple(
                "✅ Información Verificada",
                "Verificado",
                "#FFFFFF"
            )
            "probablemente_falso" -> Triple(
                "⚠️ Noticia Falsa",
                "Incorrecto",
                "#FFFFFF"
            )
            "mixto" -> Triple(
                "🔀 Resultado Mixto",
                "Mixto",
                "#FFFFFF"

            )
            "no_verificable", "no_encontrado" -> Triple(
                "🔍 No Verificable",
                "No encontrado",
                "#FFFFFF"
            )
            else -> Triple(
                "📊 Análisis de Covered",
                "Analizado",
                "#FFFFFF"
            )
        }

        binding.textResultTitle.text = title
        binding.textResultStatus.text = status


        // SOLO cambiar el color de fondo del CardView (sin triángulos)
        binding.cardResult.setCardBackgroundColor(Color.parseColor(cardColor))
    }


    private fun showErrorCard() {
        binding.textResultTitle.text = "❌ Error de Verificación"
        binding.textResultStatus.text = "Error"
        binding.textResultStatus.setTextColor(Color.parseColor("#DC3545"))
        binding.textResult.text = "No se pudo completar la verificación. Intenta nuevamente."
        showCardWithAnimation()
    }

    private fun showCardWithAnimation() {
        binding.cardResult.visibility = View.VISIBLE

        // Animación simple de entrada
        binding.cardResult.alpha = 0f
        binding.cardResult.scaleX = 0.9f
        binding.cardResult.scaleY = 0.9f
        binding.cardResult.translationY = 20f

        binding.cardResult.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .translationY(0f)
            .setDuration(400)
            .start()
    }

    private fun animateTypewriter(text: String, textView: TextView, delayPerChar: Long = 35L) {
        textView.text = ""

        var charIndex = 0
        val handler = Handler(Looper.getMainLooper())

        val runnable = object : Runnable {
            override fun run() {
                if (charIndex < text.length) {
                    val currentText = text.substring(0, charIndex + 1)
                    textView.text = currentText
                    charIndex++
                    handler.postDelayed(this, delayPerChar)
                }
            }
        }

        handler.postDelayed(runnable, 200)
    }

    private fun mostrarError(mensajeError: String) {
        binding.progressBarVerificando.visibility = View.GONE
        binding.buttonVerify.isEnabled = true

        showErrorCard()
        showMessage("Error: $mensajeError")
    }

    private fun showMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.editTextInput.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}