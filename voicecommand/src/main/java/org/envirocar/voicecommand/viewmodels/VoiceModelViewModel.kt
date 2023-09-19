package org.envirocar.voicecommand.viewmodels

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.*
import com.justai.aimybox.extensions.className
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.envirocar.voicecommand.enums.ModelPhase
import org.envirocar.voicecommand.enums.ModelState
import org.envirocar.voicecommand.handlers.VoiceModelHandler
import org.envirocar.voicecommand.model.AvailableVoiceModel
import org.envirocar.voicecommand.model.DownloadedVoiceModel
import org.envirocar.voicecommand.model.VoiceModel
import org.envirocar.voicecommand.service.DownloadState
import java.io.File

class VoiceModelViewModel(application: Application) : AndroidViewModel(application) {

    private val application = getApplication<Application>()
    private val _progressPhase = MutableLiveData<ModelPhase>()
    private val _downloadProgress = MutableLiveData<Int>(0)

    private val _availableModels = MutableLiveData<AvailableVoiceModel>()
    private val _downloadedModels = MutableLiveData<DownloadedVoiceModel>()

    val availableModels: LiveData<AvailableVoiceModel>
        get() = _availableModels
    val downloadedModels: LiveData<DownloadedVoiceModel>
        get() = _downloadedModels
    val progressPhase: LiveData<ModelPhase>
        get() = _progressPhase
    val downloadProgress: LiveData<Int>
        get() = _downloadProgress

    private val preferences: SharedPreferences

    private val voiceModelHandler: VoiceModelHandler

    init {
        preferences = application.getSharedPreferences("voicemodel",Context.MODE_PRIVATE)
        voiceModelHandler = VoiceModelHandler(getApplication())

        refreshModelsList()
    }

    private fun refreshModelsList() {
        getDownloadedModelsList()
        getAvailableModelsList()
    }

    private fun getAvailableModelsList() = viewModelScope.launch(Dispatchers.IO) {
        _availableModels.postValue(AvailableVoiceModel(ModelState.LOADING, null))
        try{
            _availableModels.postValue(AvailableVoiceModel(ModelState.SUCCESS,voiceModelHandler.getModelsListFromNetwork(downloadedModels)))
        }
        catch(e: Exception) {
            _availableModels.postValue(AvailableVoiceModel(ModelState.FAILURE, null))
        }
    }

    private fun getDownloadedModelsList() = viewModelScope.launch(Dispatchers.IO) {
        _downloadedModels.postValue(DownloadedVoiceModel(ModelState.LOADING, null))
        try{
            if(File(application.getExternalFilesDir(null)!!.absoluteFile, "/kaldi-assets/meta").exists() && File(application.getExternalFilesDir(null)!!.absoluteFile, "/kaldi-assets/meta").listFiles().size > 0){
                val models = voiceModelHandler.getModelsListFromStorage()
                _downloadedModels.postValue(DownloadedVoiceModel(ModelState.SUCCESS, models))
            }
            else{
                _downloadedModels.postValue(DownloadedVoiceModel(ModelState.FAILURE, null))
            }
        }
        catch(e: Exception){
            _downloadedModels.postValue(DownloadedVoiceModel(ModelState.FAILURE, null))
        }
    }

    fun getModel(voiceModel: VoiceModel) = viewModelScope.launch(Dispatchers.IO) {
        _progressPhase.postValue(ModelPhase.DOWNLOADING)
        voiceModelHandler.getModelFromNetwork(voiceModel).collect { downloadState->
            when (downloadState) {
                is DownloadState.Downloading -> {
                    _downloadProgress.postValue(((downloadState.progress/voiceModel.size.toDouble())*100).toInt())
                }
                is DownloadState.Failed -> {
                    Log.d(className, downloadState.error!!.stackTraceToString())
                    with(preferences.edit()) {
                        putBoolean("prefkey_voice_model_downloaded", false)
                        apply()
                    }
                }
                is DownloadState.Finished -> {
                    _progressPhase.postValue(ModelPhase.DECOMPRESSING)
                    unzipModel(voiceModel.name)
                    with(preferences.edit()) {
                        putBoolean("prefkey_voice_model_downloaded", true)
                        apply()
                    }
                }
            }
        }

    }

    private fun unzipModel(voiceModelName: String) = viewModelScope.launch(Dispatchers.IO) {

        voiceModelHandler.unzipModelFromStorage(voiceModelName)

        _progressPhase.postValue(ModelPhase.FINISHED)

        refreshModelsList()
    }

    fun removeModelFromStorage() {
        voiceModelHandler.deleteModelFromStorage()
        refreshModelsList()
        with(preferences.edit()) {
            putBoolean("prefkey_voice_model_downloaded", false)
            apply()
        }
    }

}