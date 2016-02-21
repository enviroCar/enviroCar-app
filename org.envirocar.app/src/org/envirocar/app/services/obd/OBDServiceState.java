package org.envirocar.app.services.obd;

import org.envirocar.app.R;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public enum OBDServiceState implements OBDServiceStateContent{
    NO_CAR_SELECTED{
        @Override
        public int getTitle() {
            return R.string.notification_no_car_title;
        }

        @Override
        public int getSubText() {
            return R.string.notification_no_car_description;
        }

        @Override
        public int getIcon() {
            return super.getIcon();
        }
    },
    NO_OBD_SELECTED{
        @Override
        public int getTitle() {
            return R.string.notification_no_obd_title;
        }

        @Override
        public int getSubText() {
            return R.string.notification_no_obd_description;
        }

        @Override
        public int getIcon() {
            return super.getIcon();
        }
    },
    UNCONNECTED{
        @Override
        public int getTitle() {
            return R.string.notification_unconnected_title;
        }

        @Override
        public int getSubText() {
            return R.string.notification_unconnected_description;
        }

        @Override
        public int getIcon() {
            return super.getIcon();
        }
    },
    DISCOVERING{
        @Override
        public int getTitle() {
            return R.string.notification_discovering_title;
        }

        @Override
        public int getSubText() {
            return R.string.notification_discovering_description;
        }

        @Override
        public int getIcon() {
            return super.getIcon();
        }
    },
    CONNECTING{
        @Override
        public int getTitle() {
            return R.string.notification_connecting_title;
        }

        @Override
        public int getSubText() {
            return R.string.notification_connecting_description;
        }

        @Override
        public int getIcon() {
            return super.getIcon();
        }
    },
    CONNECTED{
        @Override
        public int getTitle() {
            return R.string.notification_connected_title;
        }

        @Override
        public int getSubText() {
            return R.string.notification_connected_description;
        }

        @Override
        public int getIcon() {
            return super.getIcon();
        }
    },
    STOPPING{
        @Override
        public int getTitle() {
            return R.string.notification_stopping_title;
        }

        @Override
        public int getSubText() {
            return R.string.notification_stopping_description;
        }

        @Override
        public int getIcon() {
            return super.getIcon();
        }
    };

    @Override
    public int getTitle() {
        return 0;
    }

    @Override
    public int getSubText() {
        return 0;
    }

    @Override
    public int getIcon() {
        return 0;
    }
}
