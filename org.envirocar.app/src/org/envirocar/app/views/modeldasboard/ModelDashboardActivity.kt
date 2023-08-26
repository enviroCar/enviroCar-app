package org.envirocar.app.views.modeldasboard

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.envirocar.app.R
import org.envirocar.voicecommand.model.VoiceModel
import org.envirocar.voicecommand.viewmodels.VoiceModelViewModel

class ModelDashboardActivity : AppCompatActivity() {

    private lateinit var viewModel : VoiceModelViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_model_dashboard)

        viewModel = ViewModelProvider(this).get(VoiceModelViewModel::class.java)

        val availableRecyclerView = findViewById<RecyclerView>(R.id.availableRecycler)


        val modelAdapter = ModelAdapter()


        viewModel.models.observe(this, Observer<List<VoiceModel>> { model ->
            model.apply{ modelAdapter.models = model }
        })

        availableRecyclerView.adapter = modelAdapter
        availableRecyclerView.layoutManager = LinearLayoutManager(this)
        availableRecyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

//        DownloadDialogFragment().show(supportFragmentManager,"dialog")

//        viewModel.getModel()

    }
}