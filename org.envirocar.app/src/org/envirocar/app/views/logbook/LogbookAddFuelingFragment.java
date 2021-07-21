/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.views.logbook;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.snackbar.Snackbar;

import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.R;
import org.envirocar.app.handler.DAOProvider;
import org.envirocar.app.handler.preferences.CarPreferenceHandler;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.app.views.utils.DialogUtils;
import org.envirocar.app.views.utils.ECAnimationUtils;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Fueling;
import org.envirocar.core.entity.FuelingImpl;
import org.envirocar.core.exception.DataCreationFailureException;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.ResourceConflictException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.core.logging.Logger;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class LogbookAddFuelingFragment extends BaseInjectorFragment {
    private static final Logger LOG = Logger.getLogger(LogbookAddFuelingFragment.class);
    private static final DecimalFormat DECIMAL_FORMATTER_2 = new DecimalFormat("#.##");
    private static final DecimalFormat DECIMAL_FORMATTER_3 = new DecimalFormat("#.###");

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMAN);
        symbols.setDecimalSeparator('.');
        DECIMAL_FORMATTER_2.setDecimalFormatSymbols(symbols);
        DECIMAL_FORMATTER_3.setDecimalFormatSymbols(symbols);
    }

    @BindView(R.id.logbook_layout_addfueling_toolbar)
    protected Toolbar addFuelingToolbar;
    @BindView(R.id.activity_log_book_add_fueling_toolbar_exp)
    protected View addFuelingToolbarExp;
    @BindView(R.id.activity_logbook_add_fueling_card_content)
    protected View contentView;
    @BindView(R.id.activity_logbook_add_fueling_card_scrollview)
    protected View contentScrollview;
    @BindView(R.id.activity_logbook_add_fueling_car_selection)
    protected Spinner addFuelingCarSelection;
    @BindView(R.id.logbook_add_fueling_milagetext)
    protected EditText addFuelingMilageText;
    @BindView(R.id.logbook_add_fueling_volumetext)
    protected EditText addFuelingVolumeText;
    @BindView(R.id.logbook_add_fueling_totalpricetext)
    protected EditText addFuelingTotalCostText;
    @BindView(R.id.logbook_add_fueling_priceperlitretext)
    protected EditText addFuelingPricePerLitreText;
    @BindView(R.id.logbook_add_fueling_partialfueling_checkbox)
    protected CheckBox partialFuelingCheckbox;
    @BindView(R.id.logbook_add_fueling_missedfueling_checkbox)
    protected CheckBox missedFuelingCheckbox;
    @BindView(R.id.logbook_add_fueling_comment)
    protected EditText commentText;

    @BindView(R.id.layout_general_info_background)
    protected View infoBackground;
    @BindView(R.id.layout_general_info_background_img)
    protected ImageView infoBackgroundImg;
    @BindView(R.id.layout_general_info_background_firstline)
    protected TextView infoBackgroundFirst;
    @BindView(R.id.layout_general_info_background_secondline)
    protected TextView infoBackgroundSecond;

    @Inject
    protected CarPreferenceHandler carHandler;
    @Inject
    protected DAOProvider daoProvider;

    private CompositeDisposable subscriptions = new CompositeDisposable();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // Inflate the view and inject the annotated view.
        View view = inflater.inflate(R.layout.activity_logbook_add_fueling_card, container, false);
        ButterKnife.bind(this, view);

        addFuelingToolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
        addFuelingToolbar.inflateMenu(R.menu.menu_logbook_add_fueling);

        addFuelingToolbar.setNavigationOnClickListener(v ->
                closeThisFragment());

        addFuelingToolbar.setOnMenuItemClickListener(item -> {
            onClickAddFueling();
            hideKeyboard(getView());
            return true;
        });

        // initially we set the toolbar exp to gone
        addFuelingToolbar.setVisibility(View.GONE);
        addFuelingToolbarExp.setVisibility(View.GONE);
        contentScrollview.setVisibility(View.GONE);

        initTextViews();

        Car selectedCar = carHandler.getCar();
        List<Car> addedCars = carHandler.getDeserialzedCars();

        // Inflate the values for the car spinner.
        LogbookCarSpinnerAdapter carSpinnerAdapter = new LogbookCarSpinnerAdapter(getActivity(),
                addedCars);
        addFuelingCarSelection.setAdapter(carSpinnerAdapter);


        if (selectedCar != null) {
            // Set the position of the car inside the spinner as default.
            int spinnerPosition = carSpinnerAdapter.getPosition(selectedCar);
            addFuelingCarSelection.setSelection(spinnerPosition);
        } else if (addedCars.isEmpty()) {
            // Show a notification that there is no selected car
            contentView.setVisibility(View.GONE);
            infoBackgroundImg.setImageResource(R.drawable.img_car);
            infoBackgroundFirst.setText(R.string.logbook_background_no_cars_first);
            infoBackgroundSecond.setText(R.string.logbook_background_no_cars_second);
            ECAnimationUtils.animateShowView(getContext(), infoBackground, R.anim.fade_in);
        }

        return view;
    }

    @Override
    public void onDestroy() {
        if (!subscriptions.isDisposed()) {
            subscriptions.dispose();
        }
        super.onDestroy();
    }

    @Override
    public void onResume() {
        LOG.info("onResume()");
        super.onResume();
        ECAnimationUtils.animateShowView(getContext(), addFuelingToolbar,
                R.anim.translate_slide_in_top_fragment);
        ECAnimationUtils.animateShowView(getContext(), addFuelingToolbarExp,
                R.anim.translate_slide_in_top_fragment);
        ECAnimationUtils.animateShowView(getContext(), contentScrollview,
                R.anim.translate_slide_in_bottom_fragment);
    }

    private void onClickAddFueling() {
        // Reset the errors.
        addFuelingMilageText.setError(null);
        addFuelingTotalCostText.setError(null);
        addFuelingVolumeText.setError(null);

        if (!isNetworkAvailable(requireActivity().getApplication())){
            closeThisFragment();
            showSnackbarInfo(R.string.error_not_connected_to_network);
        }

        boolean formError = false;
        View focusView = null;
        if (addFuelingMilageText.getText() == null || addFuelingMilageText.getText().toString().equals("")) {
            addFuelingMilageText.setError(getString(R.string.logbook_error_form_blank_input));
            focusView = addFuelingMilageText;
            formError = true;
        }

        if (addFuelingTotalCostText.getText() == null || addFuelingTotalCostText.getText().toString().equals("")) {
            addFuelingTotalCostText.setError(getString(R.string.logbook_error_form_blank_input));
            focusView = addFuelingTotalCostText;
            formError = true;
        }

        if (addFuelingVolumeText.getText() == null || addFuelingVolumeText.getText()
                .toString().equals("")) {
            addFuelingVolumeText.setError(getString(R.string.logbook_error_form_blank_input));
            focusView = addFuelingVolumeText;
            formError = true;
        }

        if (formError) {
            LOG.info("Error on input form.");
            focusView.requestFocus();
            return;
        }

        Car car = (Car) addFuelingCarSelection.getSelectedItem();

        if (car == null) {
            LOG.info("Cant create fueling entry, because the car is empty");
            Snackbar.make(addFuelingToolbar,
                    "You must have selected a car type for creating a fueling.",
                    Snackbar.LENGTH_LONG).show();
            return;
        }

        Double cost = null, milage = null, volume = null;
        try {
            cost = getEditTextDoubleValue(addFuelingTotalCostText.getText().toString());
            milage = getEditTextDoubleValue(addFuelingMilageText.getText().toString());
            volume = getEditTextDoubleValue(addFuelingVolumeText.getText().toString());
        } catch (ParseException e) {
            formError = true;
            if (cost == null) {
                LOG.error(String.format("Invalid input text -> [%s]", addFuelingTotalCostText.toString()), e);
                addFuelingTotalCostText.setError(getString(R.string.logbook_invalid_input));
                focusView = addFuelingTotalCostText;
            } else if (milage == null) {
                LOG.error(String.format("Invalid input text -> [%s]", addFuelingMilageText.toString()), e);
                addFuelingMilageText.setError(getString(R.string.logbook_invalid_input));
                focusView = addFuelingMilageText;
            } else {
                LOG.error(String.format("Invalid input text -> [%s]", addFuelingVolumeText.toString()), e);
                addFuelingVolumeText.setError(getString(R.string.logbook_invalid_input));
                focusView = addFuelingVolumeText;
            }
        }

        if (formError) {
            LOG.info("Error on input form.");
            focusView.requestFocus();
            return;
        }

        boolean missedFuelStop = missedFuelingCheckbox.isChecked();
        boolean partialFueling = partialFuelingCheckbox.isChecked();

        Fueling fueling = new FuelingImpl();
        fueling.setTime(System.currentTimeMillis());
        fueling.setCar(car);
        fueling.setCost(cost, Fueling.CostUnit.EURO);
        fueling.setVolume(volume, Fueling.VolumeUnit.LITRES);
        fueling.setMilage(milage, Fueling.MilageUnit.KILOMETRES);
        fueling.setMissedFuelStop(missedFuelStop);
        fueling.setPartialFueling(partialFueling);

        if (commentText.getText() != null) {
            String comment = commentText.getText().toString();
            if (comment != null && !comment.isEmpty()) {
                fueling.setComment(comment);
            }
        }

        // upload the fueling
        if (car.getId() == null || !car.getId().isEmpty()) {
            uploadCarBeforeFueling(car, fueling);
        } else {
            uploadFueling(fueling);
        }
    }

    private Boolean isNetworkAvailable(Application application) {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                application.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network nw = connectivityManager.getActiveNetwork();
            if (nw == null) return false;
            NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(nw);
            return true;
        } else {
            NetworkInfo nwInfo = connectivityManager.getActiveNetworkInfo();
            return true;
        }
    }

    private void initTextViews() {
        addFuelingMilageText.setFilters(new InputFilter[]{
                new DigitsInputFilter(addFuelingMilageText, 7)});
        addFuelingMilageText.setOnFocusChangeListener((v, hasFocus) -> {
            String milage = addFuelingMilageText.getText().toString();
            if (milage != null && !milage.isEmpty()) {
                if (hasFocus) {
                    addFuelingMilageText.setText(milage.split(" ")[0]);
                } else {
                    addFuelingMilageText.setText(milage + " km");
                }
            }
        });

        addFuelingVolumeText.setFilters(new InputFilter[]{
                new DigitsInputFilter(addFuelingVolumeText, 3, 2)});
        addFuelingVolumeText.setOnFocusChangeListener((v, hasFocus) -> {
            String volumeText = addFuelingVolumeText.getText().toString();
            if (volumeText == null || volumeText.isEmpty())
                return;

            if (!hasFocus) {
                if (volumeText != null && !volumeText.isEmpty()) {
                    addFuelingVolumeText.setText(volumeText + " l");

                    try {
                        if (hasEditTextValue(addFuelingPricePerLitreText)) {
                            setTotalPriceValue(getEditTextDoubleValue(volumeText) *
                                    getEditTextDoubleValue(addFuelingPricePerLitreText));
                        } else if (hasEditTextValue(addFuelingTotalCostText)) {
                            setPricePerLitreValue(getEditTextDoubleValue
                                    (addFuelingTotalCostText)
                                    / getEditTextDoubleValue(volumeText));
                        }
                    } catch (ParseException e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
            } else {
                if (volumeText != null && !volumeText.isEmpty()) {
                    addFuelingVolumeText.setText(volumeText.split(" ")[0]);
                }
            }
        });

        addFuelingPricePerLitreText.setFilters(new InputFilter[]{
                new DigitsInputFilter(addFuelingPricePerLitreText, 2, 3)});
        addFuelingPricePerLitreText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                String pricePerLitre = addFuelingPricePerLitreText.getText().toString();
                if (pricePerLitre == null || pricePerLitre.isEmpty())
                    return;

                if (!hasFocus) {
                    addFuelingPricePerLitreText.setText(pricePerLitre + " €/l");

                    try {
                        if (hasEditTextValue(addFuelingVolumeText)) {
                            setTotalPriceValue(getEditTextDoubleValue(addFuelingVolumeText) *
                                    getEditTextDoubleValue(pricePerLitre));
                        } else if (hasEditTextValue(addFuelingTotalCostText)) {
                            setVolumeValue(getEditTextDoubleValue(addFuelingTotalCostText) /
                                    getEditTextDoubleValue(pricePerLitre));
                        }
                    } catch (ParseException e) {
                        LOG.error(e.getMessage(), e);
                    }
                } else {
                    addFuelingPricePerLitreText.setText(pricePerLitre.split(" ")[0]);
                }
            }
        });

        addFuelingTotalCostText.setFilters(new InputFilter[]{
                new DigitsInputFilter(addFuelingTotalCostText, 3, 2)});
        addFuelingTotalCostText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                String totalCost = addFuelingTotalCostText.getText().toString();
                if (totalCost == null || totalCost.isEmpty())
                    return;

                if (!hasFocus) {
                    addFuelingTotalCostText.setText(totalCost + " €");

                    try {
                        if (hasEditTextValue(addFuelingVolumeText)) {
                            setPricePerLitreValue(getEditTextDoubleValue(totalCost) /
                                    getEditTextDoubleValue(addFuelingVolumeText));
                        } else if (hasEditTextValue(addFuelingPricePerLitreText)) {
                            setVolumeValue(getEditTextDoubleValue(totalCost)
                                    / getEditTextDoubleValue(addFuelingPricePerLitreText));
                        }
                    } catch (ParseException e) {
                        LOG.error(e.getMessage(), e);
                    }
                } else {
                    if (totalCost != null && !totalCost.isEmpty()) {
                        addFuelingTotalCostText.setText(totalCost.split(" ")[0]);
                    }
                }
            }
        });
    }

    private void uploadCarBeforeFueling(final Car car, final Fueling fueling) {
        subscriptions.add(daoProvider.getSensorDAO()
                .createCarObservable(car)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<Car>() {
                    private AlertDialog dialog;

                    @Override
                    protected void onStart() {
                        LOG.info("uploadCarBeforeFueling() has started");
                        dialog = DialogUtils.createProgressBarDialogBuilder(getContext(),
                                R.string.logbook_dialog_uploading_fueling_header,
                                R.drawable.others_logbook_white_24dp,
                                R.string.logbook_dialog_uploading_fueling_car)
                                .setCancelable(false)
                                .show();
                    }

                    @Override
                    public void onNext(Car car) {
                        // car has been successfully uploaded
                        LOG.info(String.format(
                                "uploadCarBeforeFueling(): car has been uploaded -> [%s]",
                                car.getId()));
                    }

                    @Override
                    public void onComplete() {
                        LOG.info("uploadCarBeforeFueling(): was successful.");

                        dialog.dismiss();

                        // car upload was sucessful. Now upload the fueling.
                        uploadFueling(fueling);
                    }

                    @Override
                    public void onError(Throwable e) {
                        LOG.error(e.getMessage(), e);
                        if (e instanceof NotConnectedException) {
                            showSnackbarInfo(R.string.logbook_error_communication);
                        } else if (e instanceof DataCreationFailureException) {
                            if (!isNetworkAvailable(requireActivity().getApplication())) {
                                showSnackbarInfo(R.string.logbook_error_resource_conflict);
                            }
                        } else if (e instanceof UnauthorizedException) {
                            showSnackbarInfo(R.string.logbook_error_unauthorized);
                        } else {
                            if (isNetworkAvailable(requireActivity().getApplication()))
                            showSnackbarInfo(R.string.logbook_error_general);
                        }
                        dialog.dismiss();
                    }

                }));
    }

    /**
     * Uploads the fueling to the enviroCar Server.
     *
     * @param fueling the fueling to upload.
     */
    private void uploadFueling(Fueling fueling) {
        subscriptions.add(daoProvider.getFuelingDAO().createFuelingObservable(fueling)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<Void>() {
                    private AlertDialog dialog;

                    @Override
                    public void onStart() {
                        LOG.info("Started the creation of a fueling.");
                        dialog = DialogUtils.createProgressBarDialogBuilder(getContext(),
                                R.string.logbook_dialog_uploading_fueling_header,
                                R.drawable.others_logbook_white_24dp,
                                R.string.logbook_dialog_uploading_fueling_content)
                                .setCancelable(false)
                                .show();
                    }

                    @Override
                    public void onComplete() {
                        LOG.info(String.format("Successfully uploaded fueling -> [%s]", fueling
                                .getRemoteID()));

                        dialog.dismiss();

                        ((LogbookUiListener) getActivity()).onFuelingUploaded(fueling);
                        ((LogbookUiListener) getActivity()).onHideAddFuelingCard();
                    }

                    @Override
                    public void onError(Throwable e) {
                        LOG.error(e.getMessage(), e);
                        if (e instanceof NotConnectedException) {
                            showSnackbarInfo(R.string.logbook_error_communication);
                        } else if (e instanceof ResourceConflictException) {
                            showSnackbarInfo(R.string.logbook_error_resource_conflict);
                        } else if (e instanceof UnauthorizedException) {
                            showSnackbarInfo(R.string.logbook_error_unauthorized);
                        }
                        dialog.dismiss();
                    }

                    @Override
                    public void onNext(Void aVoid) {
                        // Nothing to do
                    }
                }));
    }

    private void setVolumeValue(double volume) {
        addFuelingVolumeText.setText(DECIMAL_FORMATTER_2.format(volume) + " l");
    }

    private void setPricePerLitreValue(double price) {
        addFuelingPricePerLitreText.setText(DECIMAL_FORMATTER_3.format(price) + " €/l");
    }

    private void setTotalPriceValue(double value) {
        addFuelingTotalCostText.setText(
                (DECIMAL_FORMATTER_2.format(value) + " €").replaceAll(",", "."));
    }

    private boolean hasEditTextValue(EditText input) {
        String value = input.getText().toString();
        return value != null && !value.isEmpty();
    }

    private double getEditTextDoubleValue(EditText input) throws ParseException {
        try {
            return getEditTextDoubleValue(input.getText().toString());
        } catch (ParseException e) {
            LOG.error(String.format("Invalid input text -> [%s]", input.toString()), e);
            addFuelingTotalCostText.setError(getString(R.string.logbook_invalid_input));
            addFuelingTotalCostText.requestFocus();
            throw e;
        }
    }

    private double getEditTextDoubleValue(String input) throws ParseException {
        String[] yea = input.split(" ");
        if (yea.length == 0) {
            return 0.0;
        }

        String toParse = yea[0].replace(",", ".");
        try {
            return Double.parseDouble(toParse);
        } catch (NumberFormatException e) {
            LOG.error(String.format("Error while parsing double [%s]", toParse), e);
            throw new ParseException(e.getMessage(), 0);
        }

    }

    private void showSnackbarInfo(int resourceID) {
        Snackbar.make(addFuelingToolbar, resourceID, Snackbar.LENGTH_LONG).show();
    }

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        baseApplicationComponent.inject(this);
    }

    private class DigitsInputFilter implements InputFilter {
        private final Pattern pattern;
        private final EditText editText;

        public DigitsInputFilter(final EditText editText, int digitsBefore) {
            this(editText, digitsBefore, -1);
        }

        /**
         * Constructor.
         *
         * @param digitsBefore
         * @param digitsAfter
         */
        public DigitsInputFilter(final EditText editText, int digitsBefore, int digitsAfter) {
            String pattern = "^(\\d{0," + (digitsBefore) + "})";
            if (digitsAfter > 0) {
                pattern += "(\\.(\\d{1," + (digitsAfter) + "})?)?$";
            }
            this.pattern = Pattern.compile(pattern);

            this.editText = editText;
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int
                dstart, int dend) {
            if (source.toString().contains(" ")) {
                addFuelingPricePerLitreText.setError(null);
                addFuelingVolumeText.setError(null);
                addFuelingTotalCostText.setError(null);

                // The string value contains a unit. Therefore split the value and show an error
                // if the splitted value does not match.
                if (source.toString().split(" ").length == 0)
                    return "";

                Matcher matcher = pattern.matcher(source.toString().split(" ")[0]);
                if (!matcher.matches()) {
                    addFuelingPricePerLitreText.setError(getString(R.string.logbook_invalid_input));
                    addFuelingVolumeText.setError(getString(R.string.logbook_invalid_input));
                    addFuelingTotalCostText.setError(getString(R.string.logbook_invalid_input));
                }
                return null;
            }

            String complete = dest.toString() + source.toString();
            Matcher matcher = pattern.matcher(complete);
            if (!matcher.matches()) {
                return "";
            }
            return null;
        }
    }


    public void closeThisFragment() {
        // ^^
        ECAnimationUtils.animateHideView(getContext(),
                ((LogbookActivity) getActivity()).overlayView, R.anim.fade_out);
        ECAnimationUtils.animateHideView(getContext(), R.anim
                .translate_slide_out_top_fragment, addFuelingToolbar, addFuelingToolbarExp);
        ECAnimationUtils.animateHideView(getContext(), contentScrollview, R.anim
                .translate_slide_out_bottom, () -> ((LogbookUiListener) getActivity()).onHideAddFuelingCard());
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

}
