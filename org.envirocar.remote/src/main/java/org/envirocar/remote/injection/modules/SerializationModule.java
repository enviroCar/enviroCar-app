/**
 * Copyright (C) 2013 - 2019 the enviroCar community
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
package org.envirocar.remote.injection.modules;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.envirocar.core.entity.Announcement;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Fueling;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.PrivacyStatement;
import org.envirocar.core.entity.TermsOfUse;
import org.envirocar.core.entity.Track;
import org.envirocar.core.entity.User;
import org.envirocar.core.entity.UserStatistic;
import org.envirocar.core.entity.UserStatistics;
import org.envirocar.remote.serde.AnnouncementSerde;
import org.envirocar.remote.serde.CarListSerde;
import org.envirocar.remote.serde.CarSerde;
import org.envirocar.remote.serde.FuelingListSerde;
import org.envirocar.remote.serde.FuelingSerde;
import org.envirocar.remote.serde.MeasurementSerde;
import org.envirocar.remote.serde.PrivacyStatementListSerde;
import org.envirocar.remote.serde.PrivacyStatementSerde;
import org.envirocar.remote.serde.RemoteTrackListSerde;
import org.envirocar.remote.serde.TermsOfUseListSerde;
import org.envirocar.remote.serde.TermsOfUseSerde;
import org.envirocar.remote.serde.TrackSerde;
import org.envirocar.remote.serde.UserSerde;
import org.envirocar.remote.serde.UserStatisticsSerde;

import java.util.List;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Contains the GSON Serialization/Deserialization instances.
 *
 * @author dewall
 */
@Module
public class SerializationModule {

    @Provides
    @Singleton
    protected UserSerde provideUserSerializer() {
        return new UserSerde();
    }

    @Provides
    @Singleton
    protected UserStatisticsSerde provideUserStatisticDeserializer() {
        return new UserStatisticsSerde();
    }

    @Provides
    @Singleton
    protected TypeToken<List<Car>> provideCarListTypeToken() {
        return new TypeToken<List<Car>>() {
        };
    }

    @Provides
    @Singleton
    protected CarListSerde provideCarListDeserializer() {
        return new CarListSerde();
    }

    @Provides
    @Singleton
    protected TrackSerde provideTrackSerializer() {
        return new TrackSerde();
    }

    @Provides
    @Singleton
    protected MeasurementSerde provideMeasurementSerializer() {
        return new MeasurementSerde();
    }

    @Provides
    @Singleton
    protected TypeToken<List<Track>> provideTrackListTypeToken() {
        return new TypeToken<List<Track>>() {
        };
    }

    @Provides
    @Singleton
    protected RemoteTrackListSerde provideRemoteTrackListDeserializer() {
        return new RemoteTrackListSerde();
    }

    @Provides
    @Singleton
    protected TermsOfUseSerde provideTermsOfUseSerializer() {
        return new TermsOfUseSerde();
    }

    @Provides
    @Singleton
    protected TypeToken<List<TermsOfUse>> provideTermsOfUseListTypeToken() {
        return new TypeToken<List<TermsOfUse>>() {
        };
    }

    @Provides
    @Singleton
    protected TypeToken<List<PrivacyStatement>> providePrivacyStatementListTypeToken() {
        return new TypeToken<List<PrivacyStatement>>() {
        };
    }

    @Provides
    @Singleton
    protected PrivacyStatementListSerde providePrivacyStatementListDeserializer(){
        return new PrivacyStatementListSerde();
    }

    @Provides
    @Singleton
    protected TermsOfUseListSerde provideTermsOfUseListSerializer() {
        return new TermsOfUseListSerde();
    }

    @Provides
    @Singleton
    protected AnnouncementSerde provideAnnouncementSerializer() {
        return new AnnouncementSerde();
    }

    @Provides
    @Singleton
    protected FuelingSerde provideFuelingSerializer() {
        return new FuelingSerde();
    }

    @Provides
    @Singleton
    protected TypeToken<List<Fueling>> provideFuelingListTypeToken() {
        return new TypeToken<List<Fueling>>() {
        };
    }

    @Provides
    @Singleton
    protected FuelingListSerde provideFuelingListSerializer() {
        return new FuelingListSerde();
    }

    @Provides
    @Singleton
    protected Gson provideGson(UserSerde userSerializer, UserStatisticsSerde userStatisticDeserializer, TypeToken<List<Car>> carListTypeToken,
                               CarListSerde carListDeserializer, TrackSerde trackSerializer, MeasurementSerde measurementSerializer,
                               TypeToken<List<Track>> trackListTypeToken, RemoteTrackListSerde remoteTrackListDeserializer, TermsOfUseSerde termsOfUseSerializer,
                               TypeToken<List<TermsOfUse>> termsOfUseListTypeToken, TermsOfUseListSerde termsOfUseListSerializer, AnnouncementSerde announcementSerializer,
                               FuelingSerde fuelingSerializer, TypeToken<List<Fueling>> fuelingListTypeToken, FuelingListSerde fuelingListSerializer,
                               TypeToken<List<PrivacyStatement>> privacyStatementTypeToken, PrivacyStatementListSerde privacyStatementListDeserializer,
                               PrivacyStatementSerde privacyStatementSerde) {
        return new GsonBuilder()
                .registerTypeAdapter(User.class, userSerializer)
                .registerTypeAdapter(UserStatistics.class, userStatisticDeserializer)
                .registerTypeAdapter(Car.class, new CarSerde())
                .registerTypeAdapter(carListTypeToken.getType(), carListDeserializer)
                .registerTypeAdapter(Track.class, trackSerializer)
                .registerTypeAdapter(Measurement.class, measurementSerializer)
                .registerTypeAdapter(trackListTypeToken.getType(), remoteTrackListDeserializer)
                .registerTypeAdapter(TermsOfUse.class, termsOfUseSerializer)
                .registerTypeAdapter(termsOfUseListTypeToken.getType(), termsOfUseListSerializer)
                .registerTypeAdapter(PrivacyStatement.class, privacyStatementSerde)
                .registerTypeAdapter(privacyStatementTypeToken.getType(), privacyStatementListDeserializer)
                .registerTypeAdapter(Announcement.class, announcementSerializer)
                .registerTypeAdapter(Fueling.class, fuelingSerializer)
                .registerTypeAdapter(fuelingListTypeToken.getType(), fuelingListSerializer)
                .registerTypeAdapter(UserStatistic.class, new UserStatisticsSerde())
                .create();
    }
}
