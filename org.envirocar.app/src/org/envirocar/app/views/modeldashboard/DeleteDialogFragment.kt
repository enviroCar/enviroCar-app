package org.envirocar.app.views.modeldashboard

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import androidx.fragment.app.DialogFragment
import org.envirocar.app.databinding.DialogModelDeleteBinding

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [DeleteDialogFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class DeleteDialogFragment(deleteModel: ()->Unit) : DialogFragment() {

    private var _binding: DialogModelDeleteBinding? = null
    private val binding get() = _binding!!

    val deleteModel = deleteModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater;

            _binding = DialogModelDeleteBinding.inflate(inflater)
            val view = binding.root

            binding.confirmButton.setOnClickListener(View.OnClickListener { deleteModel(); dismiss() })
            binding.cancelButton.setOnClickListener(View.OnClickListener { dismiss() })


            builder.setView(view)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}