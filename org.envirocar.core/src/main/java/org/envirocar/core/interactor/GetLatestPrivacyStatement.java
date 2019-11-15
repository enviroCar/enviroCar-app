package org.envirocar.core.interactor;

import org.envirocar.core.entity.PrivacyStatement;
import org.envirocar.core.injection.InjectIOScheduler;
import org.envirocar.core.injection.InjectUIScheduler;
import org.envirocar.core.repository.PrivacyStatementRepository;
import org.envirocar.core.utils.rx.Optional;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.Scheduler;

/**
 * @author dewall
 */
public class GetLatestPrivacyStatement extends Interactor<Optional<PrivacyStatement>, Void> {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private final PrivacyStatementRepository privacyStatementRepository;

    /**
     * Cosntructor.
     *
     * @param observeOn   the thread to observe on.
     * @param subscribeOn the thread to subscribe on.
     */
    @Inject
    public GetLatestPrivacyStatement(@InjectUIScheduler Scheduler observeOn, @InjectIOScheduler Scheduler subscribeOn,
                                     PrivacyStatementRepository privacyStatementRepository) {
        super(observeOn, subscribeOn);
        this.privacyStatementRepository = privacyStatementRepository;
    }

    @Override
    protected Observable<Optional<PrivacyStatement>> buildObservable(Void aVoid) {
        return privacyStatementRepository.getPrivacyStatementsObservable()
                .flatMap(list -> {
                    PrivacyStatement latest = null;
                    for (PrivacyStatement tou : list) {
                        if (latest == null) {
                            latest = tou;
                        } else {
                            Date latestDate = DATE_FORMAT.parse(latest.getIssuedDate());
                            Date touDate = DATE_FORMAT.parse(tou.getIssuedDate());
                            if (touDate.after(latestDate)) {
                                latest = tou;
                            }
                        }
                    }
                    return latest == null ?
                            Observable.just(Optional.create(null)) :
                            privacyStatementRepository.getPrivacyStatementObservable(latest.getId())
                                    .map(Optional::create);
                });
    }
}
