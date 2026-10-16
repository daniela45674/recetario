package com.example.flavorlibrary

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment

class CommentsFragment : Fragment(R.layout.fragment_comments), Refreshable {

    private var showRatings = false
    private lateinit var list: LinearLayout
    private lateinit var tvEmpty: TextView
    private lateinit var btnLogin: Button
    private lateinit var tabComments: Button
    private lateinit var tabRatings: Button

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        list = v.findViewById(R.id.ll_list)
        tvEmpty = v.findViewById(R.id.tv_empty)
        btnLogin = v.findViewById(R.id.btn_login)
        tabComments = v.findViewById(R.id.btn_tab_comments)
        tabRatings = v.findViewById(R.id.btn_tab_ratings)
        tabComments.setOnClickListener { showRatings = false; refresh() }
        tabRatings.setOnClickListener { showRatings = true; refresh() }
        btnLogin.setOnClickListener { (requireActivity() as MainActivity).requireLogin { refresh() } }
    }

    override fun refresh() {
        if (view == null) return
        list.removeAllViews()
        val logged = SessionManager.isLoggedIn
        btnLogin.visibility = if (logged) View.GONE else View.VISIBLE
        tabComments.alpha = if (showRatings) 0.5f else 1f
        tabRatings.alpha = if (showRatings) 1f else 0.5f

        val rows: List<Triple<String, String, String>> = if (!logged) emptyList()
        else if (showRatings) SessionManager.ratings.map {
            Triple(it.recipeName, "★".repeat(it.stars) + "☆".repeat(5 - it.stars), formatWhen(it.timestamp)) }
        else SessionManager.comments.map { Triple(it.recipeName, it.text, formatWhen(it.timestamp)) }

        rows.reversed().forEach { (title, body, whenText) ->
            val card = layoutInflater.inflate(R.layout.item_history, list, false)
            card.findViewById<TextView>(R.id.tv_title).text = title
            card.findViewById<TextView>(R.id.tv_body).text = body
            card.findViewById<TextView>(R.id.tv_when).text = whenText
            list.addView(card)
        }
        tvEmpty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
        tvEmpty.text = when {
            !logged -> "Inicia sesión para ver tu historial."
            showRatings -> "Todavía no has calificado recetas."
            else -> "Todavía no has escrito comentarios."
        }
    }

    override fun onResume() { super.onResume(); refresh() }
}
