/**
 * Copyright (C) 2013 - 2021 the enviroCar community
 * <p>
 * This file is part of the enviroCar app.
 * <p>
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.views.settings.custom;

import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.preference.DialogPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

import org.envirocar.app.BuildConfig;
import org.envirocar.app.R;
import org.envirocar.app.databinding.PreferenceMapStyleListItemBinding;
import org.envirocar.app.databinding.PreferenceMapViewDialogBinding;
import org.envirocar.app.handler.ApplicationSettings;
import org.envirocar.app.views.settings.SettingsActivity;
import org.envirocar.map.provider.mapbox.MapboxMapProvider;
import org.envirocar.map.provider.maplibre.MapLibreMapProvider;

import java.util.Map;
import java.util.Objects;

public class MapViewPreference extends DialogPreference {

    public interface MapViewPreferenceNotifier {
        void notifyMapViewSettingsChanged();
    }

    public MapViewPreference(Context context) {
        super(context);
    }

    public MapViewPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public int getDialogLayoutResource() {
        return R.layout.preference_map_view_dialog;
    }

    public static class Dialog extends PreferenceDialogFragmentCompat {
        static final Map<String, String> mapLibreStyles = Map.of(
                "OpenStreetMap", MapLibreMapProvider.DEFAULT_STYLE,
                "MapTiler (Basic)", "https://api.maptiler.com/maps/basic/style.json?key=" + BuildConfig.MAPTILER_API_KEY
        );

        static final Map<String, String> mapboxStyles = Map.of(
                "Mapbox (Streets)", MapboxMapProvider.DEFAULT_STYLE,
                "MapTiler (Basic)", "https://api.maptiler.com/maps/basic/style.json?key=" + BuildConfig.MAPTILER_API_KEY
        );

        PreferenceMapViewDialogBinding binding;

        private String mapProvider;
        private String mapLibreStyle;
        private String mapboxStyle;

        private boolean isUpdateMapProviderCalled = false;

        public static Dialog newInstance(String key) {
            final Dialog fragment = new Dialog();
            final Bundle b = new Bundle(1);
            b.putString(ARG_KEY, key);
            fragment.setArguments(b);
            return fragment;
        }

        @Override
        protected void onBindDialogView(@NonNull View view) {
            binding = PreferenceMapViewDialogBinding.bind(view);

            updateMapProvider(ApplicationSettings.getMapProvider(view.getContext()));
            updateMapLibreStyle(ApplicationSettings.getMapLibreStyle(view.getContext()));
            updateMapboxStyle(ApplicationSettings.getMapboxStyle(view.getContext()));

            binding.mapLibreTextView.setOnClickListener(v -> {
                updateMapProvider(MapLibreMapProvider.class.getName());
            });
            binding.mapLibreRadioButton.setOnCheckedChangeListener((v, isChecked) -> {
                if (isChecked) {
                    updateMapProvider(MapLibreMapProvider.class.getName());
                }
            });
            final ArrayAdapter<String> mapLibreListViewAdapter = new ArrayAdapter<String>(
                    view.getContext(),
                    R.layout.preference_map_style_list_item,
                    R.id.styleTextView,
                    mapLibreStyles.keySet().toArray(new String[0])
            ) {
                @NonNull
                @Override
                public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                    final View view = super.getView(position, convertView, parent);
                    final PreferenceMapStyleListItemBinding binding = PreferenceMapStyleListItemBinding.bind(view);
                    binding.styleRadioButton.setChecked(Objects.equals(mapLibreStyles.get(getItem(position)), mapLibreStyle));
                    binding.styleRadioButton.setOnCheckedChangeListener((v, isChecked) -> {
                        if (isChecked) {
                            updateMapLibreStyle(mapLibreStyles.get(getItem(position)));
                        }
                        notifyDataSetChanged();
                    });
                    view.setOnClickListener(v -> {
                        updateMapLibreStyle(mapLibreStyles.get(getItem(position)));
                        notifyDataSetChanged();
                    });
                    return view;
                }
            };
            binding.mapLibreListView.setDividerHeight(0);
            binding.mapLibreListView.setAdapter(mapLibreListViewAdapter);

            binding.mapboxTextView.setOnClickListener(v -> {
                updateMapProvider(MapboxMapProvider.class.getName());
            });
            binding.mapboxRadioButton.setOnCheckedChangeListener((v, isChecked) -> {
                if (isChecked) {
                    updateMapProvider(MapboxMapProvider.class.getName());
                }
            });
            final ArrayAdapter<String> mapboxListViewAdapter = new ArrayAdapter<String>(
                    view.getContext(),
                    R.layout.preference_map_style_list_item,
                    R.id.styleTextView,
                    mapboxStyles.keySet().toArray(new String[0])
            ) {
                @NonNull
                @Override
                public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                    final View view = super.getView(position, convertView, parent);
                    final PreferenceMapStyleListItemBinding binding = PreferenceMapStyleListItemBinding.bind(view);
                    binding.styleRadioButton.setChecked(Objects.equals(mapboxStyles.get(getItem(position)), mapboxStyle));
                    binding.styleRadioButton.setOnCheckedChangeListener((v, isChecked) -> {
                        if (isChecked) {
                            updateMapboxStyle(mapboxStyles.get(getItem(position)));
                        }
                        notifyDataSetChanged();
                    });
                    view.setOnClickListener(v -> {
                        updateMapboxStyle(mapboxStyles.get(getItem(position)));
                        notifyDataSetChanged();
                    });
                    return view;
                }
            };
            binding.mapboxListView.setDividerHeight(0);
            binding.mapboxListView.setAdapter(mapboxListViewAdapter);
        }

        @Override
        public void onDialogClosed(boolean positiveResult) {
            if (positiveResult) {
                ApplicationSettings.setMapProvider(getContext(), mapProvider);
                ApplicationSettings.setMapLibreStyle(getContext(), mapLibreStyle);
                ApplicationSettings.setMapboxStyle(getContext(), mapboxStyle);
                ((SettingsActivity) requireActivity()).notifyMapViewSettingsChanged();
            }
        }

        private void updateMapProvider(String value) {
            if (mapProvider != null && mapProvider.equals(value)) {
                return;
            }
            mapProvider = value;

            final float mapLibreLinearLayoutMaxHeight = (36.0f + mapLibreStyles.size() * 48.0f) * getResources().getDisplayMetrics().density;
            final float mapboxLinearLayoutMaxHeight = (36.0f + mapboxStyles.size() * 48.0f) * getResources().getDisplayMetrics().density;

            binding.mapLibreListView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) mapLibreLinearLayoutMaxHeight));
            binding.mapboxListView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) mapboxLinearLayoutMaxHeight));

            final ValueAnimator animator = ValueAnimator.ofFloat(0.0f, 1.0f);

            if (mapProvider.equals(MapLibreMapProvider.class.getName())) {
                if (!isUpdateMapProviderCalled) {
                    final ViewGroup.LayoutParams mapLibreLayoutParams = binding.mapLibreLinearLayout.getLayoutParams();
                    mapLibreLayoutParams.height = (int) mapLibreLinearLayoutMaxHeight;
                    binding.mapLibreLinearLayout.setLayoutParams(mapLibreLayoutParams);
                    final ViewGroup.LayoutParams mapboxLayoutParams = binding.mapboxLinearLayout.getLayoutParams();
                    mapboxLayoutParams.height = 2;
                    binding.mapboxLinearLayout.setLayoutParams(mapboxLayoutParams);
                }
                animator.addUpdateListener(animation -> {
                    final float mapLibreValue = (float) animation.getAnimatedValue() * mapLibreLinearLayoutMaxHeight;
                    final float mapboxValue = (1.0f - (float) animation.getAnimatedValue()) * mapboxLinearLayoutMaxHeight;
                    final ViewGroup.LayoutParams mapLibreLayoutParams = binding.mapLibreLinearLayout.getLayoutParams();
                    mapLibreLayoutParams.height = Math.max((int) mapLibreValue, 2);
                    binding.mapLibreLinearLayout.setLayoutParams(mapLibreLayoutParams);
                    final ViewGroup.LayoutParams mapboxLayoutParams = binding.mapboxLinearLayout.getLayoutParams();
                    mapboxLayoutParams.height = Math.max((int) mapboxValue, 2);
                    binding.mapboxLinearLayout.setLayoutParams(mapboxLayoutParams);
                });
                binding.mapLibreRadioButton.setChecked(true);
                binding.mapboxRadioButton.setChecked(false);
                binding.mapLibreExpandImageView.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.baseline_expand_less_24));
                binding.mapboxExpandImageView.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.baseline_expand_more_24));
            } else if (mapProvider.equals(MapboxMapProvider.class.getName())) {
                if (!isUpdateMapProviderCalled) {
                    final ViewGroup.LayoutParams mapLibreLayoutParams = binding.mapLibreLinearLayout.getLayoutParams();
                    mapLibreLayoutParams.height = 2;
                    binding.mapLibreLinearLayout.setLayoutParams(mapLibreLayoutParams);
                    final ViewGroup.LayoutParams mapboxLayoutParams = binding.mapboxLinearLayout.getLayoutParams();
                    mapboxLayoutParams.height = (int) mapboxLinearLayoutMaxHeight;
                    binding.mapboxLinearLayout.setLayoutParams(mapboxLayoutParams);
                }
                animator.addUpdateListener(animation -> {
                    final float mapLibreValue = (1.0f - (float) animation.getAnimatedValue()) * mapLibreLinearLayoutMaxHeight;
                    final float mapboxValue = (float) animation.getAnimatedValue() * mapboxLinearLayoutMaxHeight;
                    final ViewGroup.LayoutParams mapLibreLayoutParams = binding.mapLibreLinearLayout.getLayoutParams();
                    mapLibreLayoutParams.height = Math.max((int) mapLibreValue, 2);
                    binding.mapLibreLinearLayout.setLayoutParams(mapLibreLayoutParams);
                    final ViewGroup.LayoutParams mapboxLayoutParams = binding.mapboxLinearLayout.getLayoutParams();
                    mapboxLayoutParams.height = Math.max((int) mapboxValue, 2);
                    binding.mapboxLinearLayout.setLayoutParams(mapboxLayoutParams);
                });
                binding.mapLibreRadioButton.setChecked(false);
                binding.mapboxRadioButton.setChecked(true);
                binding.mapLibreExpandImageView.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.baseline_expand_more_24));
                binding.mapboxExpandImageView.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.baseline_expand_less_24));
            } else {
                throw new IllegalStateException("Unknown Class<T: MapProvider>: " + mapProvider);
            }

            if (isUpdateMapProviderCalled) {
                new Handler().postDelayed(
                        () -> {
                            animator.setDuration(200);
                            animator.setInterpolator(new LinearInterpolator());
                            animator.start();
                        },
                        400
                );
            }
            isUpdateMapProviderCalled = true;
        }

        private void updateMapLibreStyle(String value) {
            if (mapLibreStyle != null && mapLibreStyle.equals(value)) {
                return;
            }
            mapLibreStyle = value;
        }

        private void updateMapboxStyle(String value) {
            if (mapboxStyle != null && mapboxStyle.equals(value)) {
                return;
            }
            mapboxStyle = value;
        }
    }
}
