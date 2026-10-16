package com.example.flavorlibrary

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment

class UploadRecipeFragment : Fragment(R.layout.fragment_upload) {

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        val spinner = v.findViewById<Spinner>(R.id.sp_category)
        spinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item,
            listOf("Desayuno", "Almuerzo", "Cena", "Postres"))

        v.findViewById<Button>(R.id.btn_publish).setOnClickListener {
            val name = v.findViewById<EditText>(R.id.et_name).text.toString().trim()
            val desc = v.findViewById<EditText>(R.id.et_desc).text.toString().trim()
            val time = v.findViewById<EditText>(R.id.et_time).text.toString().toIntOrNull()
            val servings = v.findViewById<EditText>(R.id.et_servings).text.toString().toIntOrNull()
            val ingredients = v.findViewById<EditText>(R.id.et_ingredients).text.lines().map { it.trim() }.filter { it.isNotEmpty() }
            val steps = v.findViewById<EditText>(R.id.et_steps).text.lines().map { it.trim() }.filter { it.isNotEmpty() }
            val isPrivate = v.findViewById<SwitchCompat>(R.id.sw_private).isChecked

            if (name.isEmpty() || desc.isEmpty() || time == null || servings == null || servings < 1 ||
                ingredients.isEmpty() || steps.isEmpty()) {
                Toast.makeText(context, "Completa todos los campos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            RecipeRepository.add(
                Recipe(RecipeRepository.nextId(), name, desc, spinner.selectedItem.toString(), time, servings,
                    ingredients, steps, "img_default", isPublic = !isPrivate,
                    ownerEmail = SessionManager.user?.email)
            )
            Toast.makeText(context, "Receta publicada", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }
}
