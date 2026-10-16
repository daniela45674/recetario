package com.example.flavorlibrary

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FavoritesFragment : Fragment(R.layout.fragment_favorites), Refreshable {

    private lateinit var adapter: RecipeAdapter
    private lateinit var tvEmpty: TextView
    private lateinit var btnLogin: Button

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        val main = requireActivity() as MainActivity
        tvEmpty = v.findViewById(R.id.tv_empty)
        btnLogin = v.findViewById(R.id.btn_login)
        adapter = RecipeAdapter(
            onClick = { main.openRecipe(it.id) },
            onFavorite = { SessionManager.toggleFavorite(it.id); refresh() }   // quitar de favoritos
        )
        v.findViewById<RecyclerView>(R.id.rv_recipes).apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = this@FavoritesFragment.adapter
        }
        btnLogin.setOnClickListener { main.requireLogin { refresh() } }
    }

    override fun refresh() {
        if (view == null) return
        val logged = SessionManager.isLoggedIn
        val list = if (logged) RecipeRepository.favorites() else emptyList()
        adapter.submit(list)
        btnLogin.visibility = if (logged) View.GONE else View.VISIBLE
        tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        tvEmpty.text = if (logged) "Aún no tienes recetas favoritas." else "Inicia sesión para ver tus favoritos."
    }

    override fun onResume() { super.onResume(); refresh() }
}
