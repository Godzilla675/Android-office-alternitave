package com.officesuite.app.writing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint

/**
 * Math Equation Renderer for LaTeX-style equations
 * Part of Medium Priority Features Phase 2: Advanced Note-Taking
 * 
 * Supports common LaTeX mathematical notation with a simple text-based renderer.
 * For full LaTeX support, consider integrating a library like MathJax or KaTeX.
 */
class MathEquationRenderer(private val context: Context) {

    companion object {
        // Common LaTeX symbol mappings
        private val SYMBOLS = mapOf(
            "\\alpha" to "α",
            "\\beta" to "β",
            "\\gamma" to "γ",
            "\\delta" to "δ",
            "\\epsilon" to "ε",
            "\\zeta" to "ζ",
            "\\eta" to "η",
            "\\theta" to "θ",
            "\\iota" to "ι",
            "\\kappa" to "κ",
            "\\lambda" to "λ",
            "\\mu" to "μ",
            "\\nu" to "ν",
            "\\xi" to "ξ",
            "\\pi" to "π",
            "\\rho" to "ρ",
            "\\sigma" to "σ",
            "\\tau" to "τ",
            "\\upsilon" to "υ",
            "\\phi" to "φ",
            "\\chi" to "χ",
            "\\psi" to "ψ",
            "\\omega" to "ω",
            "\\Alpha" to "Α",
            "\\Beta" to "Β",
            "\\Gamma" to "Γ",
            "\\Delta" to "Δ",
            "\\Theta" to "Θ",
            "\\Lambda" to "Λ",
            "\\Xi" to "Ξ",
            "\\Pi" to "Π",
            "\\Sigma" to "Σ",
            "\\Phi" to "Φ",
            "\\Psi" to "Ψ",
            "\\Omega" to "Ω",
            "\\infty" to "∞",
            "\\pm" to "±",
            "\\mp" to "∓",
            "\\times" to "×",
            "\\div" to "÷",
            "\\cdot" to "·",
            "\\leq" to "≤",
            "\\geq" to "≥",
            "\\neq" to "≠",
            "\\approx" to "≈",
            "\\equiv" to "≡",
            "\\subset" to "⊂",
            "\\supset" to "⊃",
            "\\subseteq" to "⊆",
            "\\supseteq" to "⊇",
            "\\cup" to "∪",
            "\\cap" to "∩",
            "\\in" to "∈",
            "\\notin" to "∉",
            "\\forall" to "∀",
            "\\exists" to "∃",
            "\\nabla" to "∇",
            "\\partial" to "∂",
            "\\sum" to "∑",
            "\\prod" to "∏",
            "\\int" to "∫",
            "\\oint" to "∮",
            "\\sqrt" to "√",
            "\\to" to "→",
            "\\leftarrow" to "←",
            "\\rightarrow" to "→",
            "\\Leftarrow" to "⇐",
            "\\Rightarrow" to "⇒",
            "\\leftrightarrow" to "↔",
            "\\Leftrightarrow" to "⇔",
            "\\ldots" to "…",
            "\\cdots" to "⋯",
            "\\vdots" to "⋮",
            "\\ddots" to "⋱",
            "\\prime" to "′",
            "\\degree" to "°",
            "\\angle" to "∠",
            "\\perp" to "⊥",
            "\\parallel" to "∥"
        )

        // Superscript mappings
        private val SUPERSCRIPTS = mapOf(
            '0' to '⁰',
            '1' to '¹',
            '2' to '²',
            '3' to '³',
            '4' to '⁴',
            '5' to '⁵',
            '6' to '⁶',
            '7' to '⁷',
            '8' to '⁸',
            '9' to '⁹',
            '+' to '⁺',
            '-' to '⁻',
            '=' to '⁼',
            '(' to '⁽',
            ')' to '⁾',
            'n' to 'ⁿ',
            'i' to 'ⁱ',
            'x' to 'ˣ',
            'y' to 'ʸ'
        )

        // Subscript mappings
        private val SUBSCRIPTS = mapOf(
            '0' to '₀',
            '1' to '₁',
            '2' to '₂',
            '3' to '₃',
            '4' to '₄',
            '5' to '₅',
            '6' to '₆',
            '7' to '₇',
            '8' to '₈',
            '9' to '₉',
            '+' to '₊',
            '-' to '₋',
            '=' to '₌',
            '(' to '₍',
            ')' to '₎',
            'a' to 'ₐ',
            'e' to 'ₑ',
            'o' to 'ₒ',
            'x' to 'ₓ',
            'i' to 'ᵢ',
            'j' to 'ⱼ',
            'n' to 'ₙ',
            'm' to 'ₘ'
        )
    }

    /**
     * Data class for equation rendering result
     */
    data class EquationResult(
        val renderedText: String,
        val isValid: Boolean,
        val errorMessage: String? = null
    )

    /**
     * Parse and render LaTeX-style equation to Unicode text
     */
    fun renderEquation(latex: String): EquationResult {
        return try {
            var result = latex.trim()
            
            // Remove math delimiters if present
            result = result.removePrefix("$").removeSuffix("$")
            result = result.removePrefix("\\(").removeSuffix("\\)")
            result = result.removePrefix("\\[").removeSuffix("\\]")
            
            // Process fractions
            result = processFractions(result)
            
            // Process superscripts
            result = processSuperscripts(result)
            
            // Process subscripts  
            result = processSubscripts(result)
            
            // Process square roots
            result = processSquareRoots(result)
            
            // Replace symbols
            SYMBOLS.forEach { (latex, unicode) ->
                result = result.replace(latex, unicode)
            }
            
            // Clean up remaining LaTeX commands
            result = result.replace("\\\\".toRegex(), "")
            
            EquationResult(result, true)
        } catch (e: Exception) {
            EquationResult(latex, false, e.message)
        }
    }

    /**
     * Process LaTeX fractions: \frac{a}{b} -> a/b
     */
    private fun processFractions(input: String): String {
        var result = input
        val fracPattern = "\\\\frac\\{([^}]*)\\}\\{([^}]*)\\}".toRegex()
        
        while (fracPattern.containsMatchIn(result)) {
            result = fracPattern.replace(result) { match ->
                val numerator = match.groupValues[1]
                val denominator = match.groupValues[2]
                "$numerator/$denominator"
            }
        }
        
        return result
    }

    /**
     * Process superscripts: ^{content} or ^x
     */
    private fun processSuperscripts(input: String): String {
        var result = input
        
        // Handle ^{...} format
        val bracedPattern = "\\^\\{([^}]*)\\}".toRegex()
        result = bracedPattern.replace(result) { match ->
            val content = match.groupValues[1]
            content.map { SUPERSCRIPTS[it] ?: it }.joinToString("")
        }
        
        // Handle ^x format (single character)
        val singlePattern = "\\^(\\w)".toRegex()
        result = singlePattern.replace(result) { match ->
            val char = match.groupValues[1].firstOrNull() ?: match.groupValues[1].first()
            (SUPERSCRIPTS[char] ?: char).toString()
        }
        
        return result
    }

    /**
     * Process subscripts: _{content} or _x
     */
    private fun processSubscripts(input: String): String {
        var result = input
        
        // Handle _{...} format
        val bracedPattern = "_\\{([^}]*)\\}".toRegex()
        result = bracedPattern.replace(result) { match ->
            val content = match.groupValues[1]
            content.map { SUBSCRIPTS[it] ?: it }.joinToString("")
        }
        
        // Handle _x format (single character)
        val singlePattern = "_(\\w)".toRegex()
        result = singlePattern.replace(result) { match ->
            val char = match.groupValues[1].firstOrNull() ?: match.groupValues[1].first()
            (SUBSCRIPTS[char] ?: char).toString()
        }
        
        return result
    }

    /**
     * Process square roots: \sqrt{content} -> √content
     */
    private fun processSquareRoots(input: String): String {
        val pattern = "\\\\sqrt\\{([^}]*)\\}".toRegex()
        return pattern.replace(input) { match ->
            "√(${match.groupValues[1]})"
        }
    }

    /**
     * Render equation to bitmap for display
     */
    fun renderToBitmap(latex: String, textSize: Float = 48f, textColor: Int = Color.BLACK): Bitmap {
        val result = renderEquation(latex)
        val text = result.renderedText
        
        val paint = TextPaint().apply {
            color = textColor
            this.textSize = textSize
            isAntiAlias = true
            typeface = Typeface.create("serif", Typeface.NORMAL)
        }
        
        val width = paint.measureText(text).toInt() + 40
        val height = (textSize * 1.5f).toInt() + 20
        
        val bitmap = Bitmap.createBitmap(width.coerceAtLeast(100), height.coerceAtLeast(50), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        
        canvas.drawText(text, 20f, textSize + 5, paint)
        
        return bitmap
    }

    /**
     * Check if text contains LaTeX math notation
     */
    fun containsMath(text: String): Boolean {
        val mathIndicators = listOf(
            "$", "\\(", "\\)", "\\[", "\\]",
            "\\frac", "\\sqrt", "^", "_",
            "\\alpha", "\\beta", "\\sum", "\\int"
        )
        return mathIndicators.any { text.contains(it) }
    }

    /**
     * Get common equation templates
     */
    fun getEquationTemplates(): List<EquationTemplate> {
        return listOf(
            EquationTemplate("Quadratic Formula", "x = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a}"),
            EquationTemplate("Pythagorean Theorem", "a^2 + b^2 = c^2"),
            EquationTemplate("Einstein's E=mc²", "E = mc^2"),
            EquationTemplate("Area of Circle", "A = \\pi r^2"),
            EquationTemplate("Euler's Identity", "e^{i\\pi} + 1 = 0"),
            EquationTemplate("Sum Formula", "\\sum_{i=1}^{n} i = \\frac{n(n+1)}{2}"),
            EquationTemplate("Integral", "\\int_a^b f(x) dx"),
            EquationTemplate("Derivative", "\\frac{df}{dx}"),
            EquationTemplate("Limit", "\\lim_{x \\to \\infty} f(x)"),
            EquationTemplate("Binomial", "(a + b)^n = \\sum_{k=0}^{n} \\binom{n}{k} a^{n-k} b^k")
        )
    }

    data class EquationTemplate(
        val name: String,
        val latex: String
    )

    /**
     * Get LaTeX help reference
     */
    fun getLatexHelp(): String {
        return """
            |## LaTeX Math Quick Reference
            |
            |### Superscripts & Subscripts
            |- x^2 → x²
            |- x^{10} → x¹⁰  
            |- x_1 → x₁
            |- x_{12} → x₁₂
            |
            |### Fractions
            |- \frac{a}{b} → a/b
            |
            |### Square Root
            |- \sqrt{x} → √(x)
            |
            |### Greek Letters
            |- \alpha → α, \beta → β, \gamma → γ
            |- \pi → π, \theta → θ, \omega → ω
            |- \Sigma → Σ, \Delta → Δ, \Omega → Ω
            |
            |### Operators
            |- \sum → ∑, \prod → ∏
            |- \int → ∫, \oint → ∮
            |- \times → ×, \div → ÷
            |- \pm → ±, \cdot → ·
            |
            |### Relations
            |- \leq → ≤, \geq → ≥
            |- \neq → ≠, \approx → ≈
            |- \equiv → ≡
            |
            |### Arrows
            |- \to → →, \leftarrow → ←
            |- \Rightarrow → ⇒
            |
            |### Sets
            |- \in → ∈, \subset → ⊂
            |- \cup → ∪, \cap → ∩
            |
            |### Other
            |- \infty → ∞
            |- \forall → ∀, \exists → ∃
            |- \nabla → ∇, \partial → ∂
        """.trimMargin()
    }
}
