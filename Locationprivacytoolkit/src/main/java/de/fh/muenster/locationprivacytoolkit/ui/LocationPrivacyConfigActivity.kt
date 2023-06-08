package de.fh.muenster.locationprivacytoolkit.ui

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import de.fh.muenster.locationprivacytoolkit.config.LocationPrivacyConfig
import de.fh.muenster.locationprivacytoolkit.config.LocationPrivacyConfigManager
import de.fh.muenster.locationprivacytoolkit.databinding.ActivityLocationPrivacyConfigBinding

class LocationPrivacyConfigActivity : AppCompatActivity(), LocationPrivacyConfigAdapter.LocationPrivacyConfigAdapterListener {

    private lateinit var binding: ActivityLocationPrivacyConfigBinding
    private lateinit var configAdapter: LocationPrivacyConfigAdapter
    private lateinit var configManager: LocationPrivacyConfigManager

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configManager = LocationPrivacyConfigManager(this)
        binding = ActivityLocationPrivacyConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configAdapter = LocationPrivacyConfigAdapter(this).apply {
            val keys = LocationPrivacyConfig.values().toList()
            submitList(keys)
            notifyItemRangeChanged(0, keys.size)
        }
        binding.locationConfigRecyclerView.adapter = configAdapter
        val dividerItemDecoration = DividerItemDecoration(
            binding.locationConfigRecyclerView.context,
            RecyclerView.VERTICAL
        )
        binding.locationConfigRecyclerView.addItemDecoration(dividerItemDecoration)

        binding.locationConfigSystemSettingsButton.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.fromParts("package", packageName, null)
            startActivity(intent)
        }

        val hasFinePermission = checkSelfPermission(ACCESS_FINE_LOCATION)
        val hasCoarsePermission = checkSelfPermission(ACCESS_COARSE_LOCATION)
        binding.locationConfigSystemAccessValue.text = when (true) {
            (hasFinePermission == PackageManager.PERMISSION_GRANTED) -> "precise"
            (hasCoarsePermission == PackageManager.PERMISSION_GRANTED) -> "coarse"
            (hasCoarsePermission == PackageManager.PERMISSION_DENIED && hasFinePermission == PackageManager.PERMISSION_DENIED) -> "denied"
            else -> "unset"
        }

        val hasBackgroundPermission = checkSelfPermission(ACCESS_BACKGROUND_LOCATION)
        binding.locationConfigSystemBackgroundValue.text = if (hasBackgroundPermission == PackageManager.PERMISSION_GRANTED) {
            "yes"
        } else {
            "no"
        }
    }

    // LocationPrivacyConfigAdapterListener

    override fun onPrivacyConfigChanged(config: LocationPrivacyConfig, value: Int) {
        configManager.setPrivacyConfig(config, value)
    }

    override fun getPrivacyConfigValue(config: LocationPrivacyConfig): Int {
        return configManager.getPrivacyConfig(config) ?: config.defaultValue
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun refreshRecyclerView() {
        configAdapter.notifyDataSetChanged()
    }

}
