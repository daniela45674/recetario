package com.example.flavorlibrary

import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class User(val name: String, val email: String)
data class Comment(val recipeId: Int, val recipeName: String, val author: String, val text: String, val timestamp: Long)
data class RatingEntry(val recipeId: Int, val recipeName: String, val stars: Int, val timestamp: Long)

class Recipe(
    val id: Int,
    val name: String,
    val description: String,
    val category: String,
    val timeMin: Int,
    val baseServings: Int,
    val ingredients: List<String>,
    val steps: List<String>,
    val imageName: String,            // nombre del drawable (ej. "img_tacos")
    var ratingSum: Double = 0.0,
    var ratingCount: Int = 0,
    val isPublic: Boolean = true,
    val ownerEmail: String? = null,
    val comments: MutableList<Comment> = mutableListOf()
) {
    val average: Double get() = if (ratingCount == 0) 0.0 else ratingSum / ratingCount
    val timeText: String
        get() = if (timeMin >= 60) "${timeMin / 60}h" + (if (timeMin % 60 > 0) " ${timeMin % 60} min" else "")
        else "$timeMin min"
}

fun formatWhen(ts: Long): String =
    if (System.currentTimeMillis() - ts < 60_000) "ahora mismo"
    else SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(ts))

fun String.normalized(): String =
    Normalizer.normalize(lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

/** Escala la cantidad numérica al inicio de una línea de ingrediente: "500 g de carne" -> "1000 g de carne". */
object PortionScaler {
    private val regex = Regex("""^\s*(\d+\s*/\s*\d+|\d+(?:[.,]\d+)?)""")

    fun scale(line: String, factor: Double): String {
        val m = regex.find(line) ?: return line
        val raw = m.groupValues[1]
        val value = if (raw.contains("/")) {
            val p = raw.split("/")
            p[0].trim().toDouble() / p[1].trim().toDouble()
        } else raw.replace(',', '.').toDouble()
        val txt = String.format(Locale.US, "%.2f", value * factor).trimEnd('0').trimEnd('.')
        return txt + line.substring(m.range.last + 1)
    }
}
