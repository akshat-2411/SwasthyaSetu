package com.example.swasthyasetu.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import com.example.swasthyasetu.R
import androidx.core.content.ContextCompat
import com.example.swasthyasetu.databinding.ActivityFirstAidDetailBinding
import com.example.swasthyasetu.model.FirstAidContent

class FirstAidDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityFirstAidDetailBinding

    companion object {
        const val EXTRA_FIRST_AID = "extra_first_aid"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFirstAidDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val item = intent.getParcelableExtra<FirstAidContent>(EXTRA_FIRST_AID)

        if (item == null) {
            finish()
            return
        }

        setupToolbar(item.title)
        populateData(item)
        setupFab()
    }

    private fun setupToolbar(title: String) {
        binding.toolbar.title = title
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun populateData(item: FirstAidContent) {
        binding.tvDetailTitle.text = item.title
        binding.tvDetailDescription.text = item.description

        // Dynamically locate matching drawable identifier locally via reflection to avoid room db constraints
        val resId = resources.getIdentifier(item.iconResName, "drawable", packageName)
        if (resId != 0) {
            binding.ivDetailHeader.setImageResource(resId)
        }

        // Programmatically inject bullet points scaling based on JSON length
        binding.layoutStepsContainer.removeAllViews()
        val colorPrimaryText = ContextCompat.getColor(this, R.color.text_primary)

        item.steps.forEachIndexed { index, stepText ->
            val tvStep = TextView(this).apply {
                text = "${index + 1}. $stepText"
                textSize = 15f
                setTextColor(colorPrimaryText)
                setPadding(0, 0, 0, 16)
                setLineSpacing(4f, 1f)
            }
            binding.layoutStepsContainer.addView(tvStep)
        }
    }

    private fun setupFab() {
        binding.fabCallAmbulance.setOnClickListener {
            // Instantly dial 108 Emergency Medical Service
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:108")
            startActivity(intent)
        }
    }
}
