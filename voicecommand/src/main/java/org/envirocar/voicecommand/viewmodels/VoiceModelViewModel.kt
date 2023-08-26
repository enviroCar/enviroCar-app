package org.envirocar.voicecommand.viewmodels

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.*
import org.envirocar.voicecommand.model.VoiceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import org.envirocar.voicecommand.service.DownloadState
import org.envirocar.voicecommand.service.VoiceModelNetwork

class VoiceModelViewModel(application: Application) : AndroidViewModel(application) {

    private var _models = MutableLiveData<List<VoiceModel>>()

    val models: LiveData<List<VoiceModel>>
        get() = _models

    init {
        getModelsListFromNetwork()
    }

    private fun getModelsListFromNetwork() = viewModelScope.launch {

        val modelsList = VoiceModelNetwork.voiceModel.getModelsList()

        _models.postValue(modelsList.body())
    }

    fun getModel() = viewModelScope.launch(Dispatchers.IO) {

        VoiceModelNetwork.voiceModel.getModel("Vosk-Model-Latest.7z").saveFile().collect { downloadState->
            when (downloadState) {
                is DownloadState.Downloading -> {
                    Log.d("myTag", "progress=${downloadState.progress}")
                }
                is DownloadState.Failed -> {
                    Log.d("myTag", downloadState.error.toString())
                }
                is DownloadState.Finished -> {
                    Log.d("myTag", "Finished")
                }
            }
        }

    }

    private fun ResponseBody.saveFile(): Flow<DownloadState> {
        return flow{
            emit(DownloadState.Downloading(0))

            try {
                byteStream().use { inputStream->
                    getApplication<Application>().openFileOutput("some", Context.MODE_PRIVATE).use { outputStream->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var progressBytes = 0L
                        var bytes = inputStream.read(buffer)
                        while (bytes >= 0) {
                            outputStream.write(buffer, 0, bytes)
                            progressBytes += bytes
                            bytes = inputStream.read(buffer)
                            emit(DownloadState.Downloading(progressBytes.toInt()))
                        }
                    }
                }
                emit(DownloadState.Finished)
            } catch (e: Exception) {
                emit(DownloadState.Failed(e))
            }
        }.flowOn(Dispatchers.IO).distinctUntilChanged()
    }
}