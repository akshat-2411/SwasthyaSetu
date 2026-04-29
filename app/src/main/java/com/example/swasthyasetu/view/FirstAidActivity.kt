package com.example.swasthyasetu.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.swasthyasetu.databinding.ActivityFirstAidBinding
import com.example.swasthyasetu.model.FirstAidContent
import org.json.JSONArray
import java.io.InputStreamReader
import kotlin.jvm.java

class FirstAidActivity : BaseActivity() {

    private lateinit var binding: ActivityFirstAidBinding
    private lateinit var adapter: FirstAidAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFirstAidBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()

        // Load data offline natively from assets
        val data = loadFirstAidData()

        setupRecyclerView(data)
        setupSearch()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun loadFirstAidData(): List<FirstAidContent> {
        val list = mutableListOf<FirstAidContent>()
        try {
            val inputStream = assets.open("first_aid_data.json")
            val jsonString = InputStreamReader(inputStream).readText()
            val jsonArray = JSONArray(jsonString)

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val stepsArray = obj.getJSONArray("steps")
                val stepsList = mutableListOf<String>()
                for (j in 0 until stepsArray.length()) {
                    stepsList.add(stepsArray.getString(j))
                }

                list.add(
                    FirstAidContent(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        description = obj.getString("description"),
                        iconResName = obj.optString("iconResName", "ic_first_aid"),
                        steps = stepsList
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("FirstAidActivity", "Error natively parsing JSON assets", e)
        }
        return list
    }

    private fun setupRecyclerView(data: List<FirstAidContent>) {
        adapter = FirstAidAdapter(data) { selectedItem ->
            val intent = Intent(this, FirstAidDetailActivity::class.java).apply {
                putExtra(FirstAidDetailActivity.EXTRA_FIRST_AID, selectedItem)
            }
            startActivity(intent)
        }

        adapter.onFilterComplete = {
            updateEmptyState()
        }

        binding.rvFirstAid.layoutManager = LinearLayoutManager(this)
        binding.rvFirstAid.adapter = adapter
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                adapter.filter.filter(query)
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter.filter(newText)
                return false
            }
        })
    }

    private fun updateEmptyState() {
        if (adapter.itemCount == 0) {
            binding.tvNoResults.visibility = View.VISIBLE
            binding.rvFirstAid.visibility = View.GONE
        } else {
            binding.tvNoResults.visibility = View.GONE
            binding.rvFirstAid.visibility = View.VISIBLE
        }
    }
}
