package org.envirocar.core.interactor;

import org.envirocar.core.entity.TermsOfUse;
import org.envirocar.core.injection.InjectIOScheduler;
import org.envirocar.core.repository.TermsOfUseRepository;
import org.envirocar.core.utils.rx.Optional;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.Scheduler;

/**
 * @author dewall
 */
public class GetLatestTermsOfUse extends Interactor<Optional<TermsOfUse>, Void> {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private final TermsOfUseRepository termsOfUseRepository;

    /**
     * Cosntructor.
     *
     * @param observeOn   the thread to observe on.
     * @param subscribeOn the thread to subscribe on.
     */
    @Inject
    public GetLatestTermsOfUse(@InjectIOScheduler Scheduler observeOn, @InjectIOScheduler Scheduler subscribeOn, TermsOfUseRepository termsOfUseRepository) {
        super(observeOn, subscribeOn);
        this.termsOfUseRepository = termsOfUseRepository;
    }

    @Override
    protected Observable<Optional<TermsOfUse>> buildObservable(Void aVoid) {
        return termsOfUseRepository.getAllTermsOfUseObservable()
                .flatMap(list -> {
                    TermsOfUse latest = null;
                    for (TermsOfUse tou : list) {
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
                            termsOfUseRepository.getTermsOfUseObservable(latest.getId())
                                    .map(Optional::create);
                });
    }
}
