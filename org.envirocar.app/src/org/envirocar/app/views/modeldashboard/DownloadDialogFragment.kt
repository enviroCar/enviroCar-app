package org.envirocar.app.views.modeldashboard

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import org.envirocar.app.databinding.DialogModelDownloadBinding
import org.envirocar.voicecommand.enums.ModelPhase

class DownloadDialogFragment(cancelDownload: ()->Unit) : DialogFragment() {

    private var _binding: DialogModelDownloadBinding? = null
    private val binding get() = _binding!!
    
    lateinit var phase: LiveData<ModelPhase>
    lateinit var progress: LiveData<Int>
    val cancelDownload = cancelDownload

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater;
            _binding = DialogModelDownloadBinding.inflate(inflater)
            val view = binding.root

            binding.modelDownloadProgressbar.max = 100
            
            phase.observe(requireActivity()) { currentPhase ->
                when (currentPhase) {
                    ModelPhase.DOWNLOADING -> {
                        binding.modelPhaseTextview.text = "1 / 2"
                        binding.phaseDescriptionTextview.text = "Downloading model..."
                        binding.multipurposeButton.text = "CANCEL"
                        binding.multipurposeButton.setOnClickListener(View.OnClickListener { cancelDownload(); dismiss() })
                        progress.observe(requireActivity(), Observer<Int> { prog ->
                            binding.modelDownloadProgressbar.progress = prog; binding.modelDownloadPercentage.text = "$prog%"
                        })
                    }
                    ModelPhase.DECOMPRESSING -> {
                        binding.modelPhaseTextview.text = "2 / 2"
                        binding.phaseDescriptionTextview.text = "Decompressing model..."
                        binding.modelDownloadPercentage.text = ""
                        binding.multipurposeButton.text = "CANCEL"
                        binding.multipurposeButton.setOnClickListener(View.OnClickListener { cancelDownload(); dismiss() })
                        binding.modelDownloadProgressbar.isIndeterminate = true
                    }
                    ModelPhase.FINISHED -> {
                        binding.modelPhaseTextview.text = ""
                        binding.phaseDescriptionTextview.text = "Model Download Successful"
                        binding.modelDownloadPercentage.text = ""
                        binding.multipurposeButton.text = "DONE"
                        binding.multipurposeButton.setOnClickListener(View.OnClickListener { dismiss() })
                        binding.modelDownloadProgressbar.visibility = View.GONE
                    }
                }
            }

            builder.setView(view)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}