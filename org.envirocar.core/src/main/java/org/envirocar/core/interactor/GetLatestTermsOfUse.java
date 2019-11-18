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
