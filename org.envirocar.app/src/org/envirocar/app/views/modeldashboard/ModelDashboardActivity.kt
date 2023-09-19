package org.envirocar.app.views.modeldashboard

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.Job
import org.envirocar.app.R
import org.envirocar.app.databinding.ActivityModelDashboardBinding
import org.envirocar.voicecommand.enums.ModelState
import org.envirocar.voicecommand.model.AvailableVoiceModel
import org.envirocar.voicecommand.model.DownloadedVoiceModel
import org.envirocar.voicecommand.model.VoiceModel
import org.envirocar.voicecommand.viewmodels.VoiceModelViewModel

class ModelDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityModelDashboardBinding

    private lateinit var availableModelsProgressBar: ProgressBar;
    private lateinit var downloadedModelsProgressBar: ProgressBar;
    private val downloadDialogFragment = DownloadDialogFragment(::stopModelDownload)
    private val deleteDialogFragment = DeleteDialogFragment(::removeModel)

    private lateinit var viewModel : VoiceModelViewModel

    private lateinit var availableModelsAdapter: ModelAdapter
    private lateinit var downloadedModelsAdapter: ModelAdapter

    private lateinit var downloadModelJob: Job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModelDashboardBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        viewModel = ViewModelProvider(this).get(VoiceModelViewModel::class.java)

        availableModelsAdapter = ModelAdapter(::startModelDownload)
        downloadedModelsAdapter = ModelAdapter(::showRemoveModelDialog)


        viewModel.availableModels.observe(this, Observer<AvailableVoiceModel> { modelsList ->
            when(modelsList.status){
                ModelState.LOADING -> showAvailableLoader()
                ModelState.FAILURE -> showNetworkError(binding.availableModelsProgressBar, R.id.available_content)
                ModelState.SUCCESS -> showAvailableModels(modelsList.response!!)
            }
        })

        viewModel.downloadedModels.observe(this, Observer<DownloadedVoiceModel> { modelsList ->
            when(modelsList.status){
                ModelState.LOADING -> showDownloadedLoader()
                ModelState.FAILURE -> showNoModelsFound(binding.downloadedModelsProgressBar, R.id.downloaded_content)
                ModelState.SUCCESS -> showDownloadedModels(modelsList.response!!)
            }
        })

    }


    private fun showAvailableLoader() {
        availableModelsProgressBar.visibility = View.VISIBLE
        supportFragmentManager.beginTransaction().replace(R.id.available_content, AvailableModelsFragment(availableModelsAdapter, listOf())).commit()
    }

    private fun showDownloadedLoader() {
        downloadedModelsProgressBar.visibility = View.VISIBLE
        supportFragmentManager.beginTransaction().replace(R.id.downloaded_content, DownloadedModelsFragment(downloadedModelsAdapter,listOf())).commit()
    }

    private fun showNetworkError(loader: ProgressBar, placeholder: Int) {
        loader.visibility = View.GONE
        supportFragmentManager.beginTransaction().replace(placeholder, NetworkErrorFragment()).commit()
    }

    private fun showNoModelsFound(loader: ProgressBar, placeholder: Int) {
        loader.visibility = View.GONE
        supportFragmentManager.beginTransaction().replace(placeholder, NoModelFragment()).commit()
    }

    private fun showAvailableModels(models: List<VoiceModel>) {
        supportFragmentManager.beginTransaction().replace(R.id.available_content, AvailableModelsFragment(availableModelsAdapter,models)).commit()
        availableModelsProgressBar.visibility = View.GONE
    }

    private fun showDownloadedModels(models: List<String>) {
        supportFragmentManager.beginTransaction().replace(R.id.downloaded_content, DownloadedModelsFragment(downloadedModelsAdapter,models)).commit()
        downloadedModelsProgressBar.visibility = View.GONE
    }


    private fun startModelDownload(voiceModel: VoiceModel) {
        downloadModelJob = viewModel.getModel(voiceModel)
        downloadDialogFragment.phase = viewModel.progressPhase
        downloadDialogFragment.progress = viewModel.downloadProgress
        downloadDialogFragment.isCancelable = false
        downloadDialogFragment.show(supportFragmentManager,"ModelDownloadDialog")
    }

    private fun stopModelDownload() {
        if(downloadModelJob.isActive) {
            downloadModelJob.cancel()
            viewModel.removeModelFromStorage()
        }
    }

    private fun showRemoveModelDialog(voiceModel: VoiceModel) {
        deleteDialogFragment.isCancelable = false
        deleteDialogFragment.show(supportFragmentManager,"RemoveModelDialog")
    }
    private fun removeModel() {
        viewModel.removeModelFromStorage()
    }
}