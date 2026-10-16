package com.example.flavorlibrary

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RecipeAdapter(
    private val onClick: (Recipe) -> Unit,
    private val onFavorite: (Recipe) -> Unit
) : RecyclerView.Adapter<RecipeAdapter.VH>() {

    private var items: List<Recipe> = emptyList()

    fun submit(list: List<Recipe>) { items = list; notifyDataSetChanged() }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.tv_name)
        val desc: TextView = v.findViewById(R.id.tv_desc)
        val stars: TextView = v.findViewById(R.id.tv_stars)
        val meta: TextView = v.findViewById(R.id.tv_meta)
        val image: ImageView = v.findViewById(R.id.iv_image)
        val fav: TextView = v.findViewById(R.id.tv_fav)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_recipe, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val r = items[position]
        h.name.text = r.name.uppercase()
        h.desc.text = r.description
        val filled = Math.round(r.average).toInt().coerceIn(0, 5)
        h.stars.text = "★".repeat(filled) + "☆".repeat(5 - filled)
        h.meta.text = "(${r.ratingCount}) · ${r.timeText}"
        h.image.setImageResource(RecipeRepository.imageRes(h.itemView.context, r.imageName))
        h.fav.text = if (SessionManager.isFavorite(r.id)) "♥" else "♡"
        h.itemView.alpha = if (r.isPublic) 1f else 0.6f   // recetas privadas atenuadas
        h.itemView.setOnClickListener { onClick(r) }
        h.fav.setOnClickListener { onFavorite(r) }
    }
}
