package org.envirocar.app.views.modeldasboard

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import org.envirocar.app.R

class DownloadDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)

            val inflater = requireActivity().layoutInflater;

            builder.setView(inflater.inflate(R.layout.dialog_model_download, null))
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}