package com.example.flavorlibrary

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HomeFragment : Fragment(R.layout.fragment_home), Refreshable {

    private lateinit var adapter: RecipeAdapter
    private lateinit var btnUpload: Button
    private lateinit var tvEmpty: TextView
    private var query = ""

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        val main = requireActivity() as MainActivity
        btnUpload = v.findViewById(R.id.btn_upload)
        tvEmpty = v.findViewById(R.id.tv_empty)

        adapter = RecipeAdapter(
            onClick = { main.openRecipe(it.id) },
            onFavorite = { r -> main.requireLogin { SessionManager.toggleFavorite(r.id); refresh() } }
        )
        v.findViewById<RecyclerView>(R.id.rv_recipes).apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = this@HomeFragment.adapter
        }
        v.findViewById<EditText>(R.id.et_search).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) { query = s.toString(); refresh() }
        })
        btnUpload.setOnClickListener { main.openUpload() }
    }

    override fun refresh() {
        if (view == null) return
        val list = RecipeRepository.search(query)
        adapter.submit(list)
        tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        btnUpload.visibility = if (SessionManager.isLoggedIn) View.VISIBLE else View.GONE
    }

    override fun onResume() { super.onResume(); refresh() }
}
