package org.envirocar.app.view.logbook;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;

import org.envirocar.app.R;
import org.envirocar.app.handler.CarPreferenceHandler;
import org.envirocar.app.view.utils.ECAnimationUtils;
import org.envirocar.app.views.TypefaceEC;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Fueling;
import org.envirocar.core.entity.FuelingImpl;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.ResourceConflictException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.core.injection.BaseInjectorActivity;
import org.envirocar.core.logging.Logger;
import org.envirocar.remote.DAOProvider;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
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
public class LogbookActivity extends BaseInjectorActivity {
    private static final Logger LOG = Logger.getLogger(LogbookActivity.class);
    private static final DecimalFormat DECIMAL_FORMATTER_2 = new DecimalFormat("#.##");
    private static final DecimalFormat DECIMAL_FORMATTER_3 = new DecimalFormat("#.###");

    @Inject
    protected CarPreferenceHandler carHandler;
    @Inject
    protected DAOProvider daoProvider;

    @InjectView(R.id.activity_logbook_new_fueling_card)
    protected View addFuelingCard;
    @InjectView(R.id.activity_logbook_toolbar)
    protected Toolbar toolbar;
    @InjectView(R.id.activity_logbook_toolbar_new_fueling_fab)
    protected View newFuelingFab;

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

    @InjectView(R.id.activity_logbook_toolbar_fuelinglist)
    protected ListView fuelingList;
    protected LogbookListAdapter fuelingListAdapter;
    protected final List<Fueling> fuelings = new ArrayList<Fueling>();

    private final Scheduler.Worker bgWorker = Schedulers.io().createWorker();
    private final CompositeSubscription subscription = new CompositeSubscription();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // First, set the content view.
        setContentView(R.layout.activity_logbook);

        // Inject the Views.
        ButterKnife.inject(this);

        TypefaceEC.applyCustomFont((ViewGroup) addFuelingCard.getParent(), TypefaceEC.Raleway
                (this));

        // Initializes the Toolbar.
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Logbook");
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        addFuelingToolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
        addFuelingToolbar.inflateMenu(R.menu.menu_logbook_add_fueling);
        addFuelingToolbar.setNavigationOnClickListener(v -> hideAddFuelingCard());
        addFuelingToolbar.setOnMenuItemClickListener(item -> {
            onClickAddFueling();
            return true;
        });

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

        fuelingListAdapter = new LogbookListAdapter(this, fuelings);
        fuelingList.setAdapter(fuelingListAdapter);
        downloadFuelings();
    }

    @Override
    public void onBackPressed() {
        if (addFuelingCard.getVisibility() == View.VISIBLE) {
            hideAddFuelingCard();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!subscription.isUnsubscribed()) {
            subscription.unsubscribe();
            subscription.clear();
        }
    }

    @OnClick(R.id.activity_logbook_toolbar_new_fueling_fab)
    protected void onClickNewFuelingFAB() {
        showAddFuelingCard();
    }

    protected void onClickAddFueling() {
        // Reset the errors.
        addFuelingMilageText.setError(null);
        addFuelingTotalCostText.setError(null);
        addFuelingVolumeText.setError(null);

        boolean formError = false;
        View focusView = null;
        if (addFuelingMilageText.getText() == null || addFuelingMilageText.getText().toString()
                .equals("")) {
            addFuelingMilageText.setError("Cannot be blank");
            focusView = addFuelingMilageText;
            formError = true;
        }

        if (addFuelingTotalCostText.getText() == null || addFuelingTotalCostText.getText()
                .toString().equals("")) {
            addFuelingTotalCostText.setError("Cannot be blank");
            focusView = addFuelingTotalCostText;
            formError = true;
        }

        if (addFuelingVolumeText.getText() == null || addFuelingVolumeText.getText().toString()
                .equals("")) {
            addFuelingVolumeText.setError("Cannot be blank");
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

        if (commentText.getText() != null) {
            String comment = commentText.getText().toString();
            if (comment != null && !comment.isEmpty()) {
                fueling.setComment(comment);
            }
        }

        // upload the fueling
        uploadFueling(fueling);
    }

    /**
     * @param fueling the fueling to upload.
     */
    private void uploadFueling(final Fueling fueling) {
        subscription.add(bgWorker.schedule(() -> {
            try {
                daoProvider.getFuelingDAO().createFueling(fueling);
                LOG.info("Fueling successfully uploaded");
            } catch (NotConnectedException e) {
                e.printStackTrace();
            } catch (ResourceConflictException e) {
                e.printStackTrace();
            } catch (UnauthorizedException e) {
                e.printStackTrace();
            }
        }));
    }

    /**
     * Downloads the fuelings
     */
    private void downloadFuelings() {
        subscription.add(daoProvider.getFuelingDAO().getFuelingsObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<Fueling>>() {
                    @Override
                    public void onCompleted() {
                        LOG.info("Download of fuelings completed");
                    }

                    @Override
                    public void onError(Throwable e) {
                        LOG.error(e.getMessage(), e);
                    }

                    @Override
                    public void onNext(List<Fueling> result) {
                        // Add all remote fuelings
                        fuelings.addAll(result);

                        // Sort the list of fuelings
                        Collections.sort(fuelings);

                        // Redraw everything
                        fuelingListAdapter.notifyDataSetChanged();
                    }
                }));
    }

    /**
     * Shows the AddFuelingCard
     */
    private void showAddFuelingCard() {
        ECAnimationUtils.animateHideView(this, newFuelingFab, R.anim.fade_out, () -> {
            ECAnimationUtils.animateShowView(this, addFuelingCard, R.anim.fade_in);
        });
    }

    /**
     * Hides the AddFuelingCard
     */
    private void hideAddFuelingCard() {
        ECAnimationUtils.animateHideView(this, addFuelingCard, R.anim.fade_out, () -> {
            ECAnimationUtils.animateShowView(LogbookActivity.this, newFuelingFab, R.anim.fade_in);
        });
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

    private void setVolumeValue(double volume) {
        addFuelingVolumeText.setText(DECIMAL_FORMATTER_2.format(volume) + " l");
    }

    private void setPricePerLitreValue(double price) {
        addFuelingPricePerLitreText.setText(DECIMAL_FORMATTER_3.format(price) + " €/l");
    }

    private void setTotalPriceValue(double value) {
        addFuelingTotalCostText.setText(DECIMAL_FORMATTER_2.format(value) + " €");
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
                if(!matcher.matches()){
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

    private class CarSpinnerAdapter extends ArrayAdapter<Car>{

        /**
         * Constructor.
         *
         * @param context the context of the current scope
         * @param resource the resource id
         * @param objects the car objects to show
         */
        public CarSpinnerAdapter(Context context, int resource, List<Car> objects) {
            super(context, resource, objects);
        }
    }
}
