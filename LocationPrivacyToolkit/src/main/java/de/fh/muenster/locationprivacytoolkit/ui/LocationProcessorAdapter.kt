package de.fh.muenster.locationprivacytoolkit.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.fh.muenster.locationprivacytoolkit.R
import de.fh.muenster.locationprivacytoolkit.config.LocationPrivacyConfig
import de.fh.muenster.locationprivacytoolkit.databinding.ListItemLocationProcessorBinding
import de.fh.muenster.locationprivacytoolkit.processors.AbstractInternalLocationProcessor
import de.fh.muenster.locationprivacytoolkit.processors.utils.LocationProcessorUserInterface


class LocationProcessorAdapter(private var listener: LocationPrivacyConfigAdapterListener) :
    ListAdapter<AbstractInternalLocationProcessor, LocationProcessorAdapter.LocationProcessorViewHolder>(
        diffCallback
    ) {

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): LocationProcessorViewHolder {
        val dataBinding = ListItemLocationProcessorBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LocationProcessorViewHolder(dataBinding, listener)
    }

    override fun onBindViewHolder(holder: LocationProcessorViewHolder, position: Int) {
        getItem(position)?.let { config ->
            holder.bindTo(config)
        }
    }


    class LocationProcessorViewHolder(
        private val dataBinding: ListItemLocationProcessorBinding,
        private val listener: LocationPrivacyConfigAdapterListener
    ) : RecyclerView.ViewHolder(dataBinding.root) {

        fun bindTo(processor: AbstractInternalLocationProcessor) {
            dataBinding.locationConfigTitle.text =
                dataBinding.root.context.getString(processor.titleId)
            dataBinding.locationConfigSubtitle.text =
                dataBinding.root.context.getString(processor.subtitleId)

            val hasLocationAccess =
                listener.getPrivacyConfigValue(LocationPrivacyConfig.Access) == 1
            when (processor.userInterface) {
                LocationProcessorUserInterface.Switch -> initSwitch(processor, hasLocationAccess)
                LocationProcessorUserInterface.Slider -> initSlider(processor, hasLocationAccess)
                LocationProcessorUserInterface.Fragment -> initFragment(processor)
            }

            dataBinding.locationConfigTitle.setOnClickListener { showConfigDetails(processor) }
        }

        private fun initSwitch(
            processor: AbstractInternalLocationProcessor,
            hasLocationAccess: Boolean
        ) {
            val value = listener.getPrivacyConfigValue(processor.config) ?: processor.defaultValue
            dataBinding.locationConfigSwitch.isChecked = value > 0
            dataBinding.locationConfigSwitch.tag = processor.config
            val isLocationAccessConfig = processor.config == LocationPrivacyConfig.Access
            dataBinding.locationConfigSwitch.setOnCheckedChangeListener { switch, isChecked ->
                // ensure correct view
                if (switch.tag != processor.config) return@setOnCheckedChangeListener
                listener.onPrivacyConfigChanged(
                    processor.config, if (isChecked) 1 else 0
                )
                if (isLocationAccessConfig) {
                    listener.refreshRecyclerView()
                }
            }
            dataBinding.root.setOnClickListener { dataBinding.locationConfigSwitch.toggle() }
            // enable, if location-access is enabled or if this is the button, to toggle location-access
            dataBinding.locationConfigSwitch.isEnabled = hasLocationAccess || isLocationAccessConfig
            dataBinding.locationConfigSwitch.visibility = View.VISIBLE
            dataBinding.locationConfigSlider.visibility = View.GONE
            dataBinding.locationConfigChip.visibility = View.GONE
        }

        private fun initSlider(
            processor: AbstractInternalLocationProcessor,
            hasLocationAccess: Boolean
        ) {
            val range = processor.valueRange
            dataBinding.locationConfigSlider.valueFrom = range.first.toFloat()
            dataBinding.locationConfigSlider.valueTo = range.last.toFloat()
            dataBinding.locationConfigSlider.stepSize = 1f
            dataBinding.locationConfigSlider.isTickVisible = true
            dataBinding.locationConfigSlider.setLabelFormatter { value ->
                val configValue = processor.indexToValue(value)

                if (configValue != null) {
                    processor.formatLabel(configValue) ?: ""
                } else {
                    ""
                }
            }
            val currentValue =
                listener.getPrivacyConfigValue(processor.config) ?: processor.defaultValue
            val currentIndex = processor.valueToIndex(currentValue)
            updateCurrentState(processor, currentValue)
            dataBinding.locationConfigSlider.value = currentIndex?.toFloat() ?: 0f
            dataBinding.locationConfigSlider.tag = processor.config
            dataBinding.locationConfigSlider.addOnChangeListener { slider, value, fromUser ->
                // ensure correct view
                if (!fromUser || slider.tag != processor.config) return@addOnChangeListener
                val configValue = processor.indexToValue(value)
                if (configValue != null) {
                    listener.onPrivacyConfigChanged(
                        processor.config, configValue
                    )
                    updateCurrentState(processor, configValue)
                }
            }
            // enable, if location-access is enabled
            dataBinding.locationConfigSlider.isEnabled = hasLocationAccess
            dataBinding.locationConfigSlider.visibility = View.VISIBLE
            dataBinding.locationConfigChip.isEnabled = hasLocationAccess
            dataBinding.locationConfigChip.visibility = View.VISIBLE
            dataBinding.locationConfigSwitch.visibility = View.GONE
        }

        private fun initFragment(processor: AbstractInternalLocationProcessor) {
            dataBinding.locationConfigSlider.visibility = View.GONE
            dataBinding.locationConfigChip.visibility = View.GONE
            dataBinding.locationConfigSwitch.visibility = View.GONE
            dataBinding.root.setOnClickListener {
                listener.replaceFragment(processor.fragment)
            }
        }

        private fun updateCurrentState(processor: AbstractInternalLocationProcessor, value: Int?) {
            dataBinding.locationConfigChip.text =
                processor.formatLabel(value ?: processor.defaultValue)
        }

        private fun showConfigDetails(processor: AbstractInternalLocationProcessor) {
            MaterialAlertDialogBuilder(dataBinding.root.context).apply {
                setTitle(processor.titleId)
                setMessage(processor.descriptionId)
                setPositiveButton(R.string.locationPrivacyToolkitGlobalClose, null)
            }.show()
        }
    }

    interface LocationPrivacyConfigAdapterListener {
        fun onPrivacyConfigChanged(config: LocationPrivacyConfig, value: Int)
        fun getPrivacyConfigValue(config: LocationPrivacyConfig): Int?
        fun refreshRecyclerView()
        fun replaceFragment(fragment: Fragment?)
    }

    companion object {
        val diffCallback: DiffUtil.ItemCallback<AbstractInternalLocationProcessor> =
            object : DiffUtil.ItemCallback<AbstractInternalLocationProcessor>() {
                override fun areItemsTheSame(
                    oldItem: AbstractInternalLocationProcessor,
                    newItem: AbstractInternalLocationProcessor
                ): Boolean = oldItem.config == newItem.config

                override fun areContentsTheSame(
                    oldItem: AbstractInternalLocationProcessor,
                    newItem: AbstractInternalLocationProcessor
                ): Boolean = oldItem.config == newItem.config
            }
    }
}