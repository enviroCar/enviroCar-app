package org.envirocar.app.views.modeldashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import org.envirocar.app.databinding.FragmentAvailableModelsBinding
import org.envirocar.voicecommand.model.VoiceModel


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [AvailableModelsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AvailableModelsFragment(modelAdapter: ModelAdapter, models: List<VoiceModel>) : Fragment() {

    private var _binding: FragmentAvailableModelsBinding? = null
    private val binding get() = _binding!!

    private var param1: String? = null
    private var param2: String? = null

    private val modelList = models

    private var modelsAdapter = modelAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentAvailableModelsBinding.inflate(inflater, container, false)
        val view = binding.root

        modelList.apply { modelsAdapter.models = modelList }

        binding.availableRecycler.adapter = modelsAdapter
        binding.availableRecycler.layoutManager = LinearLayoutManager(activity)
        binding.availableRecycler.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))

        return view
    }

}