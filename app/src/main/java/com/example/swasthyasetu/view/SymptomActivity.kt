package com.example.swasthyasetu.view

import kotlin.jvm.java
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.example.swasthyasetu.R
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.swasthyasetu.model.Symptom
import com.example.swasthyasetu.viewmodel.SymptomViewModel
import com.example.swasthyasetu.databinding.ActivitySymptomBinding

class SymptomActivity : BaseActivity() {

    private lateinit var binding: ActivitySymptomBinding
    private lateinit var adapter: SymptomAdapter
    private val symptomViewModel: SymptomViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySymptomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupClickListeners()
        observeDiagnosisResult()
        observeLoading()
    }

    private fun setupRecyclerView() {
        val symptoms = listOf(
            Symptom(getString(R.string.symptom_fever), R.drawable.ic_fever),
            Symptom(getString(R.string.symptom_cough), R.drawable.ic_cough),
            Symptom(getString(R.string.symptom_body_pain), R.drawable.ic_body_pain),
            Symptom(getString(R.string.symptom_headache), R.drawable.ic_headache),
            Symptom(getString(R.string.symptom_nausea), R.drawable.ic_nausea),
            Symptom(getString(R.string.symptom_fatigue), R.drawable.ic_fatigue),
            Symptom(getString(R.string.symptom_sore_throat), R.drawable.ic_sore_throat),
            Symptom(getString(R.string.symptom_cold), R.drawable.ic_cold)
        )

        adapter = SymptomAdapter(symptoms) { selectedSymptoms ->
            if (selectedSymptoms.isEmpty()) {
                binding.fabCheckNow.text = getString(R.string.symptom_check_now)
            } else {
                binding.fabCheckNow.text =
                    getString(R.string.symptom_check_now_count, selectedSymptoms.size)
            }
        }

        binding.rvSymptoms.layoutManager = GridLayoutManager(this, 2)
        binding.rvSymptoms.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnSymptomBack.setOnClickListener { finish() }

        binding.fabCheckNow.setOnClickListener {
            val selected = adapter.getSelectedSymptoms()
            if (selected.isEmpty()) {
                Toast.makeText(this,
                    getString(R.string.symptom_none_selected),
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Feed symptom names into the ViewModel's decision tree
            val symptomNames = selected.map { it.name }
            symptomViewModel.analyzeSymptoms(symptomNames)
        }
    }

    private fun observeDiagnosisResult() {
        symptomViewModel.diagnosisResult.observe(this) { result ->
            val intent = Intent(this, ResultActivity::class.java).apply {
                putExtra(ResultActivity.EXTRA_DIAGNOSIS_RESULT, result)
            }
            startActivity(intent)
        }
    }

    private fun observeLoading() {
        symptomViewModel.isLoading.observe(this) { loading ->
            if (loading) {
                binding.fabCheckNow.isEnabled = false
                binding.fabCheckNow.text = getString(R.string.symptom_analyzing)
                Toast.makeText(this,
                    getString(R.string.symptom_ai_analyzing),
                    Toast.LENGTH_SHORT).show()
            } else {
                binding.fabCheckNow.isEnabled = true
                // Reset FAB text to reflect current selection count
                val count = adapter.getSelectedSymptoms().size
                binding.fabCheckNow.text = if (count == 0) {
                    getString(R.string.symptom_check_now)
                } else {
                    getString(R.string.symptom_check_now_count, count)
                }
            }
        }
    }
}
