/**
 * Copyright (C) 2013 - 2015 the enviroCar community
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
package org.envirocar.app.view.logbook;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import org.envirocar.app.R;
import org.envirocar.app.handler.CarPreferenceHandler;
import org.envirocar.app.view.utils.ECAnimationUtils;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Fueling;
import org.envirocar.core.entity.FuelingImpl;
import org.envirocar.core.exception.DataCreationFailureException;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.ResourceConflictException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.core.injection.BaseInjectorFragment;
import org.envirocar.core.logging.Logger;
import org.envirocar.remote.DAOProvider;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class LogbookAddFuelingFragment extends BaseInjectorFragment {
    private static final Logger LOG = Logger.getLogger(LogbookAddFuelingFragment.class);
    private static final DecimalFormat DECIMAL_FORMATTER_2 = new DecimalFormat("#.##");
    private static final DecimalFormat DECIMAL_FORMATTER_3 = new DecimalFormat("#.###");

    static{
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMAN);
        symbols.setDecimalSeparator('.');
        DECIMAL_FORMATTER_2.setDecimalFormatSymbols(symbols);
        DECIMAL_FORMATTER_3.setDecimalFormatSymbols(symbols);
    }

    @InjectView(R.id.logbook_layout_addfueling_toolbar)
    protected Toolbar addFuelingToolbar;
    @InjectView(R.id.activity_logbook_add_fueling_card_content)
    protected View contentView;
    @InjectView(R.id.activity_logbook_add_fueling_car_selection)
    protected Spinner addFuelingCarSelection;
    @InjectView(R.id.logbook_add_fueling_milagetext)
    protected EditText addFuelingMilageText;
    @InjectView(R.id.logbook_add_fueling_volumetext)
    protected EditText addFuelingVolumeText;
    @InjectView(R.id.logbook_add_fueling_totalpricetext)
    protected EditText addFuelingTotalCostText;
    @InjectView(R.id.logbook_add_fueling_priceperlitretext)
    protected EditText addFuelingPricePerLitreText;
    @InjectView(R.id.logbook_add_fueling_partialfueling_checkbox)
    protected CheckBox partialFuelingCheckbox;
    @InjectView(R.id.logbook_add_fueling_missedfueling_checkbox)
    protected CheckBox missedFuelingCheckbox;
    @InjectView(R.id.logbook_add_fueling_comment)
    protected EditText commentText;

    @InjectView(R.id.layout_general_info_background)
    protected View infoBackground;
    @InjectView(R.id.layout_general_info_background_img)
    protected ImageView infoBackgroundImg;
    @InjectView(R.id.layout_general_info_background_firstline)
    protected TextView infoBackgroundFirst;
    @InjectView(R.id.layout_general_info_background_secondline)
    protected TextView infoBackgroundSecond;

    @Inject
    protected CarPreferenceHandler carHandler;
    @Inject
    protected DAOProvider daoProvider;

    private CompositeSubscription subscriptions = new CompositeSubscription();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // Inflate the view and inject the annotated view.
        View view = inflater.inflate(R.layout.activity_logbook_add_fueling_card, container, false);
        ButterKnife.inject(this, view);

        addFuelingToolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
        addFuelingToolbar.inflateMenu(R.menu.menu_logbook_add_fueling);
        addFuelingToolbar.setNavigationOnClickListener(v ->
                ((LogbookUiListener) getActivity()).onHideAddFuelingCard());
        addFuelingToolbar.setOnMenuItemClickListener(item -> {
            onClickAddFueling();
            return true;
        });

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
        if (!subscriptions.isUnsubscribed()) {
            subscriptions.unsubscribe();
        }
        super.onDestroy();
    }

    private void onClickAddFueling() {
        // Reset the errors.
        addFuelingMilageText.setError(null);
        addFuelingTotalCostText.setError(null);
        addFuelingVolumeText.setError(null);

        boolean formError = false;
        View focusView = null;
        if (addFuelingMilageText.getText() == null || addFuelingMilageText.getText().toString()
                .equals("")) {
            addFuelingMilageText.setError(getString(R.string.logbook_error_form_blank_input));
            focusView = addFuelingMilageText;
            formError = true;
        }

        if (addFuelingTotalCostText.getText() == null || addFuelingTotalCostText.getText()
                .toString().equals("")) {
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
            cost = getEditTextDoubleValue(addFuelingTotalCostText.toString());
            milage = getEditTextDoubleValue(addFuelingMilageText.toString());
            volume = getEditTextDoubleValue(addFuelingVolumeText.toString());
        } catch (ParseException e) {
            formError = true;
            if (cost == null) {
                LOG.error(String.format("Invalid input text -> [%s]", addFuelingTotalCostText
                        .toString()), e);
                addFuelingTotalCostText.setError("Ungültige Eingabe.");
                focusView = addFuelingTotalCostText;
            } else if (milage == null) {
                LOG.error(String.format("Invalid input text -> [%s]", addFuelingMilageText
                        .toString()), e);
                addFuelingMilageText.setError("Ungültige Eingabe.");
                focusView = addFuelingMilageText;
            } else {
                LOG.error(String.format("Invalid input text -> [%s]", addFuelingVolumeText
                        .toString()), e);
                addFuelingVolumeText.setError("Ungültige Eingabe.");
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

    private void initTextViews() {
        addFuelingMilageText.setFilters(new InputFilter[]{
                new DigitsInputFilter(addFuelingMilageText, 7)});
        addFuelingMilageText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                String milage = addFuelingMilageText.getText().toString();
                if (milage != null && !milage.isEmpty()) {
                    if (hasFocus) {
                        addFuelingMilageText.setText(milage.split(" ")[0]);
                    } else {
                        addFuelingMilageText.setText(milage + " km");
                    }
                }
            }
        });

        addFuelingVolumeText.setFilters(new InputFilter[]{
                new DigitsInputFilter(addFuelingVolumeText, 3, 2)});
        addFuelingVolumeText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
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
                .subscribe(new Subscriber<Car>() {
                    private MaterialDialog dialog;

                    @Override
                    public void onStart() {
                        LOG.info("uploadCarBeforeFueling() has started");

                        dialog = new MaterialDialog.Builder(getContext())
                                .progress(true, 0)
                                .title(R.string.logbook_dialog_uploading_fueling_header)
                                .content(R.string.logbook_dialog_uploading_fueling_car)
                                .cancelable(false)
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
                    public void onCompleted() {
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
                            showSnackbarInfo(R.string.logbook_error_resource_conflict);
                        } else if (e instanceof UnauthorizedException) {
                            showSnackbarInfo(R.string.logbook_error_unauthorized);
                        } else {
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
                .subscribe(new Subscriber<Void>() {
                    private MaterialDialog dialog;

                    @Override
                    public void onStart() {
                        LOG.info("Started the creation of a fueling.");
                        dialog = new MaterialDialog.Builder(getContext())
                                .progress(true, 0)
                                .title(R.string.logbook_dialog_uploading_fueling_header)
                                .content(R.string.logbook_dialog_uploading_fueling_content)
                                .cancelable(false)
                                .show();
                    }

                    @Override
                    public void onCompleted() {
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
            addFuelingTotalCostText.setError("Ungültige Eingabe.");
            addFuelingTotalCostText.requestFocus();
            throw e;
        }
    }

    private double getEditTextDoubleValue(String input) throws ParseException {
        String numberValue = input.split(" ")[0].replaceAll(",", ".");
        return Double.parseDouble(numberValue);
    }

    private void showSnackbarInfo(int resourceID) {
        Snackbar.make(addFuelingToolbar, resourceID, Snackbar.LENGTH_LONG).show();
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
                Matcher matcher = pattern.matcher(source.toString().split(" ")[0]);
                if (!matcher.matches()) {
                    addFuelingPricePerLitreText.setError("Invalid input");
                    addFuelingVolumeText.setError("Invalid input");
                    addFuelingTotalCostText.setError("Invalid input");
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
}
