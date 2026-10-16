package com.example.flavorlibrary

import android.content.Context

object RecipeRepository {

    private val recipes = mutableListOf(
        Recipe(1, "Tacos de carnitas", "Tacos jugosos con carnitas marinadas en especias mexicanas y piña.", "Almuerzo", 45, 4,
            listOf("500 g de carne de cerdo", "12 tortillas de maíz", "1 taza de piña en cubos", "1/2 cebolla picada", "2 cucharadas de cilantro", "Sal y comino al gusto"),
            listOf("Corta el cerdo en cubos y sazona con sal y comino.", "Cocina a fuego medio hasta que dore y quede jugoso.", "Calienta las tortillas.", "Sirve con piña, cebolla y cilantro."),
            "img_tacos", 583.0, 124),
        Recipe(2, "Pasta carbonara", "La auténtica carbonara romana con guanciale, yemas y Pecorino. Sin nata.", "Cena", 25, 2,
            listOf("200 g de espagueti", "100 g de guanciale", "3 yemas de huevo", "50 g de queso Pecorino", "Pimienta negra al gusto"),
            listOf("Cuece la pasta en agua con sal.", "Dora el guanciale hasta que suelte la grasa.", "Mezcla las yemas con el queso y la pimienta.", "Une todo fuera del fuego con un poco de agua de cocción."),
            "img_carbonara", 1450.0, 312),
        Recipe(3, "Pancakes de arándanos", "Pancakes esponjosos con ralladura de limón y arándanos frescos. Miel de arce.", "Desayuno", 20, 4,
            listOf("200 g de harina", "2 huevos", "300 ml de leche", "1 taza de arándanos", "2 cucharadas de azúcar", "1 cucharadita de levadura"),
            listOf("Mezcla los ingredientes secos.", "Añade huevos y leche hasta obtener una masa suave.", "Incorpora los arándanos.", "Cocina porciones en sartén y sirve con miel."),
            "img_pancakes", 410.0, 89),
        Recipe(4, "Ramen de miso", "Caldo de cerdo cremoso, pasta de miso, chashu, huevo marinado y nori.", "Cena", 210, 2,
            listOf("500 g de huesos de cerdo", "2 cucharadas de pasta de miso", "200 g de fideos de ramen", "2 huevos", "2 láminas de nori"),
            listOf("Hierve los huesos varias horas para el caldo.", "Disuelve el miso en el caldo caliente.", "Cuece los fideos y los huevos.", "Sirve con chashu y nori."),
            "img_ramen", 2090.0, 445),
        Recipe(5, "Tarta de chocolate", "Densa y húmeda de chocolate belga con base crujiente de galleta.", "Postres", 90, 8,
            listOf("200 g de chocolate negro", "150 g de galletas", "100 g de mantequilla", "3 huevos", "200 ml de nata"),
            listOf("Tritura las galletas con mantequilla y forma la base.", "Derrite el chocolate con la nata.", "Añade los huevos y vierte sobre la base.", "Hornea y deja enfriar antes de servir."),
            "img_tarta", 760.0, 160),
        Recipe(6, "Ensalada mediterránea", "Fresca y ligera con tomate, pepino, aceitunas y queso feta.", "Almuerzo", 15, 2,
            listOf("2 tomates", "1 pepino", "100 g de queso feta", "10 aceitunas negras", "3 cucharadas de aceite de oliva"),
            listOf("Corta las verduras en trozos.", "Añade aceitunas y feta.", "Aliña con aceite, sal y orégano."),
            "img_ensalada", 330.0, 70)
    )

    fun nextId() = (recipes.maxOfOrNull { it.id } ?: 0) + 1
    fun find(id: Int) = recipes.first { it.id == id }
    fun add(r: Recipe) { recipes.add(r) }

    private fun visible(r: Recipe) = r.isPublic || r.ownerEmail == SessionManager.user?.email

    /** Búsqueda en tiempo real por nombre y categoría (ignora tildes y mayúsculas). */
    fun search(query: String): List<Recipe> {
        val q = query.trim().normalized()
        return recipes.filter { visible(it) &&
                (q.isEmpty() || it.name.normalized().contains(q) || it.category.normalized().contains(q)) }
    }

    fun favorites() = recipes.filter { SessionManager.isFavorite(it.id) }
    fun ownedBy(email: String) = recipes.filter { it.ownerEmail == email }

    fun imageRes(ctx: Context, name: String): Int {
        val id = ctx.resources.getIdentifier(name, "drawable", ctx.packageName)
        return if (id != 0) id else android.R.drawable.ic_menu_gallery
    }
}
