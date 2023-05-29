package de.fh.muenster.locationprivacytoolkit.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.fh.muenster.locationprivacytoolkit.config.LocationPrivacyConfig
import de.fh.muenster.locationprivacytoolkit.config.LocationPrivacyConfigInterface
import de.fh.muenster.locationprivacytoolkit.databinding.ListItemLocationPrivacyConfigBinding


class LocationPrivacyConfigAdapter(private var listener: LocationPrivacyConfigAdapterListener): ListAdapter<LocationPrivacyConfig, LocationPrivacyConfigAdapter.LocationPrivacyConfigViewHolder>(
    diffCallback
) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): LocationPrivacyConfigViewHolder {
        val dataBinding = ListItemLocationPrivacyConfigBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LocationPrivacyConfigViewHolder(dataBinding, listener)
    }

    override fun onBindViewHolder(holder: LocationPrivacyConfigViewHolder, position: Int) {
        getItem(position)?.let { config ->
            holder.bindTo(config)
        }
    }


    class LocationPrivacyConfigViewHolder(
        private val dataBinding: ListItemLocationPrivacyConfigBinding,
        private val listener: LocationPrivacyConfigAdapterListener
    ) : RecyclerView.ViewHolder(dataBinding.root) {

        fun bindTo(config: LocationPrivacyConfig) {
            dataBinding.locationConfigTitle.text =
                dataBinding.root.context.getString(config.titleId)
            dataBinding.locationConfigSubtitle.text =
                dataBinding.root.context.getString(config.subtitleId)

            val hasLocationAccess = listener.getPrivacyConfigValue(LocationPrivacyConfig.Access) > 0

            when (config.userInterface) {
                LocationPrivacyConfigInterface.Switch -> initSwitch(config, hasLocationAccess)
                LocationPrivacyConfigInterface.Slider -> initSlider(config, hasLocationAccess)
            }

            dataBinding.locationConfigTitleView.setOnClickListener { showConfigDetails(config) }
        }

        private fun initSwitch(config: LocationPrivacyConfig, hasLocationAccess: Boolean) {
            dataBinding.locationConfigSwitch.isChecked = listener.getPrivacyConfigValue(config) > 0
            val isLocationAccessConfig = config == LocationPrivacyConfig.Access
            dataBinding.locationConfigSwitch.setOnCheckedChangeListener { _, isChecked ->
                listener.onPrivacyConfigChanged(
                    config,
                    if (isChecked) 1 else 0
                )
                if (isLocationAccessConfig) {
                    listener.refreshRecyclerView()
                }
            }
            dataBinding.root.setOnClickListener { dataBinding.locationConfigSwitch.toggle() }
            // enable, if location-access is enabled or if this is the button, to toggle location-access
            dataBinding.locationConfigSwitch.isEnabled = hasLocationAccess || isLocationAccessConfig
            dataBinding.locationConfigSwitch.visibility = View.VISIBLE
        }

        private fun initSlider(config: LocationPrivacyConfig, hasLocationAccess: Boolean) {
            val range = config.range
            dataBinding.locationConfigSlider.valueFrom = range.first.toFloat()
            dataBinding.locationConfigSlider.valueTo = range.last.toFloat()
            dataBinding.locationConfigSlider.stepSize = 1f
            dataBinding.locationConfigSlider.isTickVisible = true
            dataBinding.locationConfigSlider.setLabelFormatter { value ->
                val configValue = config.indexToValue(value)
                if (configValue != null) {
                    config.formatLabel(configValue)
                } else {
                    ""
                }
            }
            val currentValue = config.valueToIndex(listener.getPrivacyConfigValue(config))
            val initialValue = (currentValue ?: config.defaultValue).toFloat()
            updateCurrentState(config, config.indexToValue(initialValue))
            dataBinding.locationConfigSlider.value = initialValue
            dataBinding.locationConfigSlider.addOnChangeListener { _, value, _ ->
                val configValue = config.indexToValue(value)
                if (configValue != null) {
                    listener.onPrivacyConfigChanged(
                        config,
                        configValue
                    )
                    updateCurrentState(config, configValue)
                }
            }
            // enable, if location-access is enabled
            dataBinding.locationConfigSlider.isEnabled = hasLocationAccess
            dataBinding.locationConfigSlider.visibility = View.VISIBLE
            dataBinding.locationConfigChip.isEnabled = hasLocationAccess
            dataBinding.locationConfigChip.visibility = View.VISIBLE
        }

        private fun updateCurrentState(config: LocationPrivacyConfig, value: Int?) {
            dataBinding.locationConfigChip.text = config.formatLabel(value ?: config.defaultValue)
        }

        private fun showConfigDetails(config: LocationPrivacyConfig) {
            MaterialAlertDialogBuilder(dataBinding.root.context).apply {
                setTitle(config.titleId)
                setMessage(config.descriptionId)
                setPositiveButton("Close", null)
            }.show()
        }
    }

    interface LocationPrivacyConfigAdapterListener {
        fun onPrivacyConfigChanged(config: LocationPrivacyConfig, value: Int)
        fun getPrivacyConfigValue(config: LocationPrivacyConfig): Int
        fun refreshRecyclerView()
    }

    companion object {
        val diffCallback: DiffUtil.ItemCallback<LocationPrivacyConfig> = object : DiffUtil.ItemCallback<LocationPrivacyConfig>() {
            override fun areItemsTheSame(oldItem: LocationPrivacyConfig, newItem: LocationPrivacyConfig): Boolean =
                oldItem == newItem

            override fun areContentsTheSame(oldItem: LocationPrivacyConfig, newItem: LocationPrivacyConfig): Boolean =
                oldItem == newItem
        }
    }
}