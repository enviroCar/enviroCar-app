package org.envirocar.obd.commands;

public enum PID implements PIDEnumInstance {

//    FUEL_SYSTEM_STATUS {
//        @Override
//        public String getHexadecimalRepresentation() {
//            return "03";
//        }
//    },
    CALCULATED_ENGINE_LOAD {
        @Override
        public String getHexadecimalRepresentation() {
            return "04";
        }
    },
    SHORT_TERM_FUEL_TRIM_BANK_1 {
        @Override
        public String getHexadecimalRepresentation() {
            return "06";
        }
    },
    LONG_TERM_FUEL_TRIM_BANK_1 {
        @Override
        public String getHexadecimalRepresentation() {
            return "07";
        }
    },
    FUEL_PRESSURE {
        @Override
        public String getHexadecimalRepresentation() {
            return "0A";
        }
    },
    INTAKE_MAP {
        @Override
        public String getHexadecimalRepresentation() {
            return "0B";
        }
    },
    RPM {
        @Override
        public String getHexadecimalRepresentation() {
            return "0C";
        }
    },
    SPEED {
        @Override
        public String getHexadecimalRepresentation() {
            return "0D";
        }
    },
    INTAKE_AIR_TEMP {
        @Override
        public String getHexadecimalRepresentation() {
            return "0F";
        }
    },
    MAF {
        @Override
        public String getHexadecimalRepresentation() {
            return "10";
        }
    },
    TPS {
        @Override
        public String getHexadecimalRepresentation() {
            return "11";
        }
    },
    O2_LAMBDA_PROBE_1_VOLTAGE {
        @Override
        public String getHexadecimalRepresentation() {
            return "24";
        }
    },
    O2_LAMBDA_PROBE_2_VOLTAGE {
        @Override
        public String getHexadecimalRepresentation() {
            return "25";
        }
    },
    O2_LAMBDA_PROBE_3_VOLTAGE {
        @Override
        public String getHexadecimalRepresentation() {
            return "26";
        }
    },
    O2_LAMBDA_PROBE_4_VOLTAGE {
        @Override
        public String getHexadecimalRepresentation() {
            return "27";
        }
    },
    O2_LAMBDA_PROBE_5_VOLTAGE {
        @Override
        public String getHexadecimalRepresentation() {
            return "28";
        }
    },
    O2_LAMBDA_PROBE_6_VOLTAGE {
        @Override
        public String getHexadecimalRepresentation() {
            return "29";
        }
    },
    O2_LAMBDA_PROBE_7_VOLTAGE {
        @Override
        public String getHexadecimalRepresentation() {
            return "2A";
        }
    },
    O2_LAMBDA_PROBE_8_VOLTAGE {
        @Override
        public String getHexadecimalRepresentation() {
            return "2B";
        }
    }, O2_LAMBDA_PROBE_1_CURRENT {
        @Override
        public String getHexadecimalRepresentation() {
            return "34";
        };
    }
    , O2_LAMBDA_PROBE_2_CURRENT {
        @Override
        public String getHexadecimalRepresentation() {
            return "35";
        };
    }
    , O2_LAMBDA_PROBE_3_CURRENT {
        @Override
        public String getHexadecimalRepresentation() {
            return "36";
        };
    }
    , O2_LAMBDA_PROBE_4_CURRENT {
        @Override
        public String getHexadecimalRepresentation() {
            return "37";
        };
    }
    , O2_LAMBDA_PROBE_5_CURRENT {
        @Override
        public String getHexadecimalRepresentation() {
            return "38";
        };
    }
    , O2_LAMBDA_PROBE_6_CURRENT {
        @Override
        public String getHexadecimalRepresentation() {
            return "39";
        };
    }
    , O2_LAMBDA_PROBE_7_CURRENT {
        @Override
        public String getHexadecimalRepresentation() {
            return "3A";
        };
    }
    , O2_LAMBDA_PROBE_8_CURRENT {
        @Override
        public String getHexadecimalRepresentation() {
            return "3B";
        };
    }

}
