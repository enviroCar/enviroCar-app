package org.envirocar.voicecommand.handlers

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import com.justai.aimybox.extensions.className
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.ResponseBody
import org.envirocar.voicecommand.enums.ModelState
import org.envirocar.voicecommand.model.DownloadedVoiceModel
import org.envirocar.voicecommand.model.VoiceModel
import org.envirocar.voicecommand.model.DownloadState
import org.envirocar.voicecommand.service.VoiceModelNetwork
import java.io.BufferedInputStream
import java.io.File
import java.util.zip.ZipInputStream

class VoiceModelHandler(application: Application) {

    val application = application

    suspend fun getModelsListFromNetwork(downloadedModels: LiveData<DownloadedVoiceModel>): List<VoiceModel> {
        try{
            val modelsList = VoiceModelNetwork.voiceModel.getModelsList()
            return if(downloadedModels.value?.status?.equals(ModelState.SUCCESS) == true)
                modelsList.body()?.filter { (downloadedModels.value!!.response?.contains(it.name.split(".")[0]) == false)}!!
            else
                modelsList.body()!!
        }
        catch(e: Exception){
            Log.e(className,e.stackTraceToString())
            throw e
        }
    }

    fun getModelsListFromStorage(): List<String> {
        try{
            return listOf(File(application.applicationContext.getExternalFilesDir(null)!!.absoluteFile,"/kaldi-assets/meta").listFiles()[0].name)
        }
        catch(e: Exception){
            Log.e(className,e.stackTraceToString())
            throw e
        }

    }

    suspend fun getModelFromNetwork(voiceModel: VoiceModel): Flow<DownloadState> {
        return VoiceModelNetwork.voiceModel.getModel(voiceModel.name).saveFile(voiceModel.name)
    }

    fun unzipModelFromStorage(voiceModelName: String) {

        try{
            var filename: String
            val fis = application.openFileInput(voiceModelName)
            val zis = ZipInputStream(BufferedInputStream(fis))

            var ze = zis.nextEntry

            File(
                application.getExternalFilesDir(null)!!.absoluteFile,
                "kaldi-assets"
            ).mkdirs()

            while (ze != null) {

                filename = ze.name

                if (ze.isDirectory) {
                    val fmd = File(
                        application.getExternalFilesDir(null)!!.absoluteFile,
                        "/kaldi-assets/$filename"
                    )
                    fmd.mkdirs()
                    ze = zis.getNextEntry()
                    continue
                }

                File(
                    application.getExternalFilesDir(null)!!.absoluteFile,
                    "/kaldi-assets/$filename"
                )
                    .outputStream()
                    .use { outputStream ->
                        zis.copyTo(outputStream, DEFAULT_BUFFER_SIZE)
                        outputStream.flush()
                    }

                zis.closeEntry();

                ze = zis.nextEntry

            }
        }
        catch(e: Exception){
            Log.e(className,e.stackTraceToString())
        }


    }

    fun deleteModelFromStorage() {
        try{
            val kaldiDir = File(application.getExternalFilesDir(null)!!.absoluteFile, "kaldi-assets")
            if(kaldiDir.exists()) kaldiDir.deleteRecursively()
        }
        catch(e: Exception){
            Log.e(className,e.stackTraceToString())
        }
    }

    private fun ResponseBody.saveFile(voiceModelName: String): Flow<DownloadState> {
        return flow{
            emit(DownloadState.Downloading(0))

            try {
                byteStream().use { inputStream->
                    application.openFileOutput(voiceModelName, Context.MODE_PRIVATE).use { outputStream->
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