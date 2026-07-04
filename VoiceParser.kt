package com.puerto2026

import com.puerto2026.data.entities.Capture
import java.util.regex.Pattern

object VoiceParser {

    // Mapa números en Hassaniya (árabe dialectal mauritano)
    private val arabicNumbersMap = mapOf(
        "صفر" to 0,
        "واحد" to 1,
        "اثنين" to 2,
        "ثلاثة" to 3,
        "أربعة" to 4,
        "خمسة" to 5,
        "ستة" to 6,
        "سبعة" to 7,
        "ثمانية" to 8,
        "تسعة" to 9,
        "عشرة" to 10,
        "١" to 1,
        "٢" to 2,
        "٣" to 3,
        "٤" to 4,
        "٥" to 5,
        "٦" to 6,
        "٧" to 7,
        "٨" to 8,
        "٩" to 9,
        "٠" to 0
    )
    
    // Mapa nombres pescados español ↔ hassaniya (común puerto)
    private val fishNameMap = mapOf(
        // Español -> lista de equivalentes hassaniya
        "pulpo" to listOf("أخطبوط", "حببوط", "بطل"),
        "merluza" to listOf("مرلوزة"),
        "sardina" to listOf("سردين"),
        "pez espada" to listOf("سمك السيف"),
        "bacalao" to listOf("بقالو"),
        "calamar" to listOf("كالامار", "حبار"),
        "langosta" to listOf("لوبستر"),
        "cangrejo" to listOf("سلطعون"),
        "atún" to listOf("تونة", "تون"),
        "dorada" to listOf("قرموط")
        // agrega más según se necesite
    )

    // Invertimos para buscar hassaniya -> español
    private val fishNameReverseMap: Map<String, String> = fishNameMap.flatMap { (es, arList) ->
        listOf(es) + arList
    }.associateWith { key -> 
        fishNameMap.entries.find { it.value.contains(key) || it.key == key }?.key ?: key
    }

    // Función para normalizar número (puede venir como palabra árabe o dígito)
    private fun parseNumber(numStr: String): Double? {
        val cleaned = numStr.trim()
        // Intentar parsear directo número decimal
        cleaned.toDoubleOrNull()?.let { return it }

        // Si es palabra árabe
        arabicNumbersMap[cleaned]?.let { return it.toDouble() }

        // Si es palabra compuesta (ejemplo "خمسة عشر" 15) - para ampliar, aquí se puede mejorar

        return null
    }

    // Extraer nombre normalizado del pescado
    private fun normalizeFishName(rawName: String): String? {
        val lower = rawName.lowercase().trim()
        // Buscar clave en mapa directo
        fishNameReverseMap[lower]?.let { return it }
        // Buscar por similitud simplificada (contains)
        fishNameReverseMap.keys.forEach {
            if (lower.contains(it)) return fishNameReverseMap[it]
        }
        return null
    }

    /**
     * Parsear texto dictado para extraer captura:
     * Se espera que el texto contenga:
     * - Nombre persona
     * - Tipo pescado
     * - Kilos (número)
     * - Precio (número)
     *
     * Ejemplo en español:
     * "Juan pulpo 5 kilos a 10"
     * Ejemplo en hassaniya:
     * "محمد أخطبوط خمسة كيلو ب10"
     */
    fun parse(text: String): Capture? {
        // Normalizar texto a minúsculas, eliminar signos innecesarios
        val normalizedText = text.lowercase()

        // Patrón regex general:
        // Para capturar: nombre, pescado, kilos, precio
        // Ejemplo de patrón simplificado (puede ser mejorado según contexto):
        val pattern = Pattern.compile(
            "(\\w+)\\s+" +                              // Nombre persona (1 palabra)
            "([\\w\\p{InArabic}]+)\\s+" +              // Tipo pescado (palabra con árabe o latín)
            "(\\d+|[\\p{InArabic}]+)\\s*(kilos|كيلو|كيلوغرام|كيلو جرام)?\\s*" + // Kilos (número o palabra árabe)
            "(?:a|ب|بسعر)?\\s*" +                      // Conector precio (opcional)
            "(\\d+|[\\p{InArabic}]+)"                   // Precio (número o palabra árabe)
        )

        val matcher = pattern.matcher(normalizedText)
        if (matcher.find()) {
            val rawName = matcher.group(1) ?: return null
            val rawFish = matcher.group(2) ?: return null
            val rawKilos = matcher.group(3) ?: return null
            val rawPrice = matcher.group(5) ?: return null

            val fishName = normalizeFishName(rawFish) ?: return null
            val kilos = parseNumber(rawKilos) ?: return null
            val price = parseNumber(rawPrice) ?: return null

            return Capture(
                name = rawName.replaceFirstChar { it.uppercase() },
                fishType = fishName,
                kilos = kilos,
                price = price
            )
        }

        return null
    }

    /**
     * Detectar si el texto es una consulta rápida "¿Cuánto le debo a [Nombre]?" en español o hassaniya
     * Ejemplos:
     * - Español: "¿Cuánto le debo a Juan?"
     * - Hassaniya: "شحال علي محمد؟" (¿Cuánto le debo a Mohamed?)
     */
    fun parseQuery(text: String): String? {
        // Normalizado
        val t = text.lowercase().trim()

        // Español
        val spanishPattern = Pattern.compile("cu[áa]nto le debo a (\\w+)")
        val spanishMatcher = spanishPattern.matcher(t)
        if (spanishMatcher.find()) {
            return spanishMatcher.group(1)?.replaceFirstChar { it.uppercase() }
        }

        // Hassaniya (ejemplo frases comunes):
        // "شحال علي محمد" o "شحال عليا محمد"
        val hassPattern = Pattern.compile("شحال علي(?:ا)? (\\w+)")
        val hassMatcher = hassPattern.matcher(t)
        if (hassMatcher.find()) {
            return hassMatcher.group(1)?.replaceFirstChar { it.uppercase() }
        }

        return null
    }
}
