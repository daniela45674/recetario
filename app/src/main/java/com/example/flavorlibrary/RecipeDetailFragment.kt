package com.example.flavorlibrary

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment

class RecipeDetailFragment : Fragment(R.layout.fragment_recipe_detail), Refreshable {

    companion object {
        fun newInstance(id: Int) = RecipeDetailFragment().apply {
            arguments = Bundle().apply { putInt("id", id) }
        }
        const val MIN_PEOPLE = 1
        const val MAX_PEOPLE = 50
    }

    private lateinit var recipe: Recipe
    private var people = 1
    private var showSteps = false

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        recipe = RecipeRepository.find(requireArguments().getInt("id"))
        people = recipe.baseServings

        v.findViewById<ImageView>(R.id.iv_image)
            .setImageResource(RecipeRepository.imageRes(requireContext(), recipe.imageName))
        v.findViewById<TextView>(R.id.tv_category).text = recipe.category.uppercase()
        v.findViewById<TextView>(R.id.tv_name).text = recipe.name
        v.findViewById<TextView>(R.id.tv_desc).text = recipe.description

        val main = requireActivity() as MainActivity
        v.findViewById<Button>(R.id.btn_fav).setOnClickListener {
            main.requireLogin { SessionManager.toggleFavorite(recipe.id); refresh() }
        }
        v.findViewById<Button>(R.id.btn_back).setOnClickListener { parentFragmentManager.popBackStack() }

        // Pestañas
        v.findViewById<Button>(R.id.btn_tab_ing).setOnClickListener { showSteps = false; renderTab() }
        v.findViewById<Button>(R.id.btn_tab_steps).setOnClickListener { showSteps = true; renderTab() }

        // Calculadora de porciones
        v.findViewById<Button>(R.id.btn_minus).setOnClickListener {
            if (people > MIN_PEOPLE) { people--; renderTab() }
        }
        v.findViewById<Button>(R.id.btn_plus).setOnClickListener {
            if (people < MAX_PEOPLE) { people++; renderTab() }
        }

        // Comentarios (botón o tecla Enter)
        val et = v.findViewById<EditText>(R.id.et_comment)
        v.findViewById<Button>(R.id.btn_send).setOnClickListener { sendComment() }
        et.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                sendComment(); true
            } else false
        }
        refresh()
    }

    override fun refresh() {
        val v = view ?: return
        val avg = if (recipe.ratingCount == 0) "Sin calificaciones" else String.format("★ %.1f", recipe.average)
        v.findViewById<TextView>(R.id.tv_meta).text =
            "$avg (${recipe.ratingCount}) · ${recipe.timeText}"
        v.findViewById<Button>(R.id.btn_fav).text =
            if (SessionManager.isFavorite(recipe.id)) "♥ Quitar de favoritos" else "♡ Guardar en favoritos"
        renderTab(); renderRating(); renderComments()
    }

    private fun renderTab() {
        val v = view ?: return
        v.findViewById<View>(R.id.ll_portions).visibility = if (showSteps) View.GONE else View.VISIBLE
        v.findViewById<Button>(R.id.btn_tab_ing).alpha = if (showSteps) 0.5f else 1f
        v.findViewById<Button>(R.id.btn_tab_steps).alpha = if (showSteps) 1f else 0.5f
        val tv = v.findViewById<TextView>(R.id.tv_content)
        if (showSteps) {
            tv.text = recipe.steps.mapIndexed { i, s -> "${i + 1}. $s" }.joinToString("\n\n")
        } else {
            val factor = people.toDouble() / recipe.baseServings
            v.findViewById<TextView>(R.id.tv_people).text = "$people ${if (people == 1) "persona" else "personas"}"
            v.findViewById<TextView>(R.id.tv_adjust).text = "Ajuste: ${Math.round(factor * 100)}%"
            tv.text = recipe.ingredients.mapIndexed { i, s -> "${i + 1}. ${PortionScaler.scale(s, factor)}" }
                .joinToString("\n")
        }
    }

    private fun renderRating() {
        val v = view ?: return
        val box = v.findViewById<LinearLayout>(R.id.ll_stars)
        val msg = v.findViewById<TextView>(R.id.tv_rate_msg)
        box.removeAllViews()
        val entry = SessionManager.ratings.firstOrNull { it.recipeId == recipe.id }
        for (i in 1..5) {
            val star = TextView(requireContext()).apply {
                text = if (entry != null && i <= entry.stars) "★" else "☆"
                textSize = 32f
                setTextColor(0xFFFFC107.toInt())
                setPadding(8, 0, 8, 0)
                setOnClickListener { rate(i) }
            }
            box.addView(star)
        }
        msg.text = when {
            entry != null -> "Ya calificaste esta receta en esta sesión."
            !SessionManager.isLoggedIn -> "Inicia sesión para calificar."
            else -> "Toca una estrella para calificar."
        }
    }

    private fun rate(stars: Int) {
        if (SessionManager.hasRated(recipe.id)) return
        (requireActivity() as MainActivity).requireLogin {
            if (SessionManager.hasRated(recipe.id)) return@requireLogin
            recipe.ratingSum += stars
            recipe.ratingCount++
            SessionManager.ratings.add(RatingEntry(recipe.id, recipe.name, stars, System.currentTimeMillis()))
            refresh()
        }
    }

    private fun sendComment() {
        val v = view ?: return
        val et = v.findViewById<EditText>(R.id.et_comment)
        val text = et.text.toString().trim()
        if (text.isEmpty()) return
        (requireActivity() as MainActivity).requireLogin {
            val user = SessionManager.user ?: return@requireLogin
            val c = Comment(recipe.id, recipe.name, user.name, text, System.currentTimeMillis())
            recipe.comments.add(c)
            SessionManager.comments.add(c)
            et.setText("")
            renderComments()
        }
    }

    private fun renderComments() {
        val v = view ?: return
        val box = v.findViewById<LinearLayout>(R.id.ll_comments)
        box.removeAllViews()
        recipe.comments.reversed().forEach { c ->
            val row = layoutInflater.inflate(R.layout.item_history, box, false)
            row.findViewById<TextView>(R.id.tv_title).text = c.author
            row.findViewById<TextView>(R.id.tv_body).text = c.text
            row.findViewById<TextView>(R.id.tv_when).text = formatWhen(c.timestamp)
            box.addView(row)
        }
    }
}
