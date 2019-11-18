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
package org.envirocar.app.services.autoconnect;

import androidx.lifecycle.LifecycleObserver;

import org.envirocar.app.R;

/**
 * @author dewall
 */
public interface AutoRecordingStrategy extends LifecycleObserver {

    interface Factory {
        AutoRecordingStrategy create();
    }

    enum AutoRecordingState {
        BLUETOOTH_DISABLED {
            @Override
            public int getTitleRes() {
                return R.string.notification_autorecording_precondition_error_title;
            }

            @Override
            public int getSubTextRes() {
                return R.string.notification_autorecording_bluetooth_disabled_subtext;
            }
        },
        GPS_DISABLED {
            @Override
            public int getTitleRes() {
                return R.string.notification_autorecording_precondition_error_title;
            }

            @Override
            public int getSubTextRes() {
                return R.string.notification_autorecording_gps_disabled_subtext;
            }
        },
        CAR_NOT_SELECTED {
            @Override
            public int getTitleRes() {
                return R.string.notification_autorecording_precondition_error_title;
            }

            @Override
            public int getSubTextRes() {
                return R.string.notification_autorecording_no_car_selected_subtext;
            }
        },
        OBD_NOT_SELECTED {
            @Override
            public int getTitleRes() {
                return R.string.notification_autorecording_precondition_error_title;
            }

            @Override
            public int getSubTextRes() {
                return R.string.notification_autorecording_no_obd_selected_subtext;
            }
        },
        ACTIVE {
            @Override
            public int getTitleRes() {
                return R.string.notification_autorecording_active_title;
            }

            @Override
            public int getSubTextRes() {
                return R.string.notification_autorecording_active_subtext;
            }
        };

        public int getTitleRes() {
            return 0;
        }

        public int getSubTextRes() {
            return 0;
        }

        public int getIconRes() {
            return R.drawable.ic_launcher_notification;
        }
    }

    interface AutoRecordingCallback {

        /**
         * @param preconditionType
         */
        void onPreconditionUpdate(AutoRecordingState preconditionType);

        /**
         * Called when the specific requirements of the recordingtype has been met for starting a track.
         */
        void onRecordingTypeConditionsMet();
    }

    boolean preconditionsFulfilled();

    void run(AutoRecordingCallback callback);

    void stop();

}
