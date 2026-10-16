package com.example.flavorlibrary

/** Estado de la sesión en memoria (se limpia al cerrar sesión). */
object SessionManager {
    var user: User? = null
        private set
    private val favorites = mutableSetOf<Int>()
    val comments = mutableListOf<Comment>()
    val ratings = mutableListOf<RatingEntry>()

    val isLoggedIn get() = user != null

    fun login(name: String, email: String) { user = User(name, email) }

    fun logout() {
        user = null
        favorites.clear(); comments.clear(); ratings.clear()
    }

    fun isFavorite(id: Int) = id in favorites
    fun toggleFavorite(id: Int) { if (!favorites.add(id)) favorites.remove(id) }
    fun hasRated(id: Int) = ratings.any { it.recipeId == id }
}

interface Refreshable { fun refresh() }
