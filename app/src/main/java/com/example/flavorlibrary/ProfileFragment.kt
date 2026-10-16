package com.example.flavorlibrary

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ProfileFragment : Fragment(R.layout.fragment_profile), Refreshable {

    private lateinit var pubAdapter: RecipeAdapter
    private lateinit var privAdapter: RecipeAdapter

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        val main = requireActivity() as MainActivity
        fun adapter() = RecipeAdapter(
            onClick = { main.openRecipe(it.id) },
            onFavorite = { r -> SessionManager.toggleFavorite(r.id); refresh() })
        pubAdapter = adapter(); privAdapter = adapter()
        listOf(R.id.rv_public to pubAdapter, R.id.rv_private to privAdapter).forEach { (id, ad) ->
            v.findViewById<RecyclerView>(id).apply {
                layoutManager = LinearLayoutManager(context)
                adapter = ad
                isNestedScrollingEnabled = false
            }
        }
        v.findViewById<Button>(R.id.btn_login).setOnClickListener { main.requireLogin { refresh() } }
        v.findViewById<Button>(R.id.btn_upload).setOnClickListener { main.openUpload() }
        v.findViewById<Button>(R.id.btn_logout).setOnClickListener { main.logout() }
    }

    override fun refresh() {
        val v = view ?: return
        val user = SessionManager.user
        val logged = user != null
        v.findViewById<View>(R.id.ll_guest).visibility = if (logged) View.GONE else View.VISIBLE
        v.findViewById<View>(R.id.ll_user).visibility = if (logged) View.VISIBLE else View.GONE
        if (user == null) return
        v.findViewById<TextView>(R.id.tv_user_name).text = user.name
        v.findViewById<TextView>(R.id.tv_user_email).text = user.email
        val mine = RecipeRepository.ownedBy(user.email)
        pubAdapter.submit(mine.filter { it.isPublic })
        privAdapter.submit(mine.filter { !it.isPublic })
    }

    override fun onResume() { super.onResume(); refresh() }
}
