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
import android.widget.Spinner;

import com.afollestad.materialdialogs.MaterialDialog;

import org.envirocar.app.R;
import org.envirocar.app.handler.CarPreferenceHandler;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Scheduler;
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

    @InjectView(R.id.logbook_layout_addfueling_toolbar)
    protected Toolbar addFuelingToolbar;
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

    @Inject
    protected CarPreferenceHandler carHandler;
    @Inject
    protected DAOProvider daoProvider;

    private Scheduler.Worker backgroundWorker = Schedulers.io().createWorker();
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

        Car car = carHandler.getCar();
        if (car == null) {
            LOG.info("Cant create fueling entry, because the car is empty");
            return;
        }

        double cost = Double.parseDouble(addFuelingTotalCostText.getText()
                .toString().split(" ")[0]);
        double milage = Double.parseDouble(addFuelingMilageText.getText()
                .toString().split(" ")[0]);
        double volume = Double.parseDouble(addFuelingVolumeText.getText()
                .toString().split(" ")[0]);
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
                if (!hasFocus) {
                    String volumeText = addFuelingVolumeText.getText().toString();
                    if (volumeText != null && !volumeText.isEmpty()) {
                        addFuelingVolumeText.setText(volumeText + " l");

                        if (hasEditTextValue(addFuelingTotalCostText)) {
                            setTotalPriceValue(getEditTextDoubleValue(volumeText) *
                                    getEditTextDoubleValue(addFuelingPricePerLitreText));
                        } else if (hasEditTextValue(addFuelingTotalCostText)) {
                            setPricePerLitreValue(getEditTextDoubleValue(addFuelingTotalCostText)
                                    / getEditTextDoubleValue(volumeText));
                        }
                    }
                } else {
                    String volumeText = addFuelingVolumeText.getText().toString();
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
                String price = addFuelingPricePerLitreText.getText().toString();
                if (price != null && !price.isEmpty()) {
                    if (hasFocus) {
                        addFuelingPricePerLitreText.setText(price.split(" ")[0]);
                    } else {
                        addFuelingPricePerLitreText.setText(price + " €/l");

                        if (hasEditTextValue(addFuelingVolumeText)) {
                            setTotalPriceValue(getEditTextDoubleValue(addFuelingVolumeText) *
                                    getEditTextDoubleValue(price));
                        } else if (hasEditTextValue(addFuelingTotalCostText)) {
                            setVolumeValue(getEditTextDoubleValue(addFuelingTotalCostText) /
                                    getEditTextDoubleValue(price));
                        }
                    }
                }
            }
        });

        addFuelingTotalCostText.setFilters(new InputFilter[]{
                new DigitsInputFilter(addFuelingTotalCostText, 3, 2)});
        addFuelingTotalCostText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                String totalCost = addFuelingTotalCostText.getText().toString();
                if (totalCost != null && !totalCost.isEmpty()) {
                    if (hasFocus) {
                        addFuelingTotalCostText.setText(totalCost.split(" ")[0]);
                    } else {
                        addFuelingTotalCostText.setText(totalCost + " €");

                        if (hasEditTextValue(addFuelingVolumeText)) {
                            setPricePerLitreValue(getEditTextDoubleValue(totalCost) /
                                    getEditTextDoubleValue(addFuelingVolumeText));
                        } else if (hasEditTextValue(addFuelingPricePerLitreText)) {
                            setVolumeValue(getEditTextDoubleValue(totalCost)
                                    / getEditTextDoubleValue(addFuelingPricePerLitreText));
                        }
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
        addFuelingTotalCostText.setText(DECIMAL_FORMATTER_2.format(value) + " €");
    }

    private boolean hasEditTextValue(EditText input) {
        String pricePerLitre = addFuelingPricePerLitreText.getText().toString();
        return pricePerLitre != null && !pricePerLitre.isEmpty();
    }

    private double getEditTextDoubleValue(EditText input) {
        return getEditTextDoubleValue(input.getText().toString());
    }

    private double getEditTextDoubleValue(String input) {
        String stringValue = input.split(" ")[0];
        return Double.parseDouble(stringValue);
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
