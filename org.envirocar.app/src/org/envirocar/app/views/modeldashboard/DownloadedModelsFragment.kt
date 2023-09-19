package org.envirocar.app.views.modeldashboard

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.envirocar.app.databinding.FragmentDownloadedModelsBinding
import org.envirocar.voicecommand.model.VoiceModel


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [DownloadedModelsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class DownloadedModelsFragment(modelAdapter: ModelAdapter, models: List<String>) : Fragment() {

    private var _binding: FragmentDownloadedModelsBinding? = null
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

        _binding = FragmentDownloadedModelsBinding.inflate(inflater, container, false)
        val view = binding.root

        modelList.apply { modelsAdapter.models = modelList.map { VoiceModel(it,"",0) } }

        binding.downloadedRecycler.adapter = modelsAdapter
        binding.downloadedRecycler.layoutManager = LinearLayoutManager(activity)
        binding.downloadedRecycler.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))

        return view
    }

}