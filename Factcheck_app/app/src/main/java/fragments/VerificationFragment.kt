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

        // Detectar automáticamente si es URL
        val isUrl = isValidUrl(inputText) && !inputText.contains(" ")

        if (isUrl) {
            // Forzar modo URL incluso si el usuario no activó el toggle
            realizarVerificacionUrl(inputText)
        } else {
            realizarVerificacionUrl(inputText)
        }
    }

    private fun isValidUrl(text: String): Boolean {
        return if (text.startsWith("http://") || text.startsWith("https://")) {
            Patterns.WEB_URL.matcher(text).matches()
        } else {
            // Intentar con https:// por defecto
            Patterns.WEB_URL.matcher("https://$text").matches()
        }
    }

    private fun realizarVerificacionUrl(url: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Enviar la URL directamente, no como texto
                val noticia = Noticia(
                    texto = "", // Texto vacío
                    url = url,  // URL en el campo correspondiente
                    usuarioId = "usuario_android"
                )

                val resultado = RetrofitInstance.api.verificarNoticia(noticia)
                withContext(Dispatchers.Main) {
                    mostrarResultadoReal(resultado)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    mostrarError(e.message ?: "Error procesando URL")
                }
            }
        }
    }

    private fun mostrarResultadoReal(resultado: VerificationResult) {
        binding.progressBarVerificando.visibility = View.GONE
        binding.buttonVerify.isEnabled = true

        if (resultado.success) {
            val razonamiento = resultado.razonamiento ?: "No se pudo generar un análisis detallado."

            // Configurar el resultado según el veredicto
            configureResultAppearance(resultado)

            val mensajeCompleto = buildString {
                append(razonamiento)
            }

            // Mostrar resultado con animación
            showResultWithAnimation()

            // Iniciar efecto typewriter
            animateTypewriter(mensajeCompleto, binding.textResult, 35L)

        } else {
            showErrorResult()
        }
    }

    private fun configureResultAppearance(resultado: VerificationResult) {
        val (title, status, textColor) = when (resultado.resultado) {
            "verificado", "probablemente_verdadero" -> Triple(
                "✅ Información Verificada",
                "Verificado",
                "#1977BF"
            )
            "probablemente_falso" -> Triple(
                "⚠️ Noticia Falsa",
                "Incorrecto",
                "#DC3545"
            )
            "mixto" -> Triple(
                "🔀 Resultado Mixto",
                "Mixto",
                "#FF9800"
            )
            "no_verificable", "no_encontrado" -> Triple(
                "🔍 No Verificable",
                "No encontrado",
                "#6C757D"
            )
            else -> Triple(
                "📊 Análisis de Covered",
                "Analizado",
                "#1977BF"
            )
        }

        binding.textResultTitle.text = title
    }

    private fun showErrorResult() {
        binding.textResultTitle.text = "❌ Error de Verificación"
        binding.textResult.text = "No se pudo completar la verificación. Intenta nuevamente."
        showResultWithAnimation()
    }

    private fun showResultWithAnimation() {
        binding.resultContainer.visibility = View.VISIBLE

        // Animación simple de entrada
        binding.resultContainer.alpha = 0f
        binding.resultContainer.scaleX = 0.95f
        binding.resultContainer.scaleY = 0.95f
        binding.resultContainer.translationY = 20f

        binding.resultContainer.animate()
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

        showErrorResult()
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