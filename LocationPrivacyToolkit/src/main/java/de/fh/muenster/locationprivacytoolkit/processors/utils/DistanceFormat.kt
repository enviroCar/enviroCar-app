package de.fh.muenster.locationprivacytoolkit.processors.utils

class DistanceFormat {
    companion object {
        fun formatDistance(distanceMeters: Int): String {
            if(distanceMeters == 0) return "Full Accuracy"
            return if (distanceMeters >= 10000) {
                String.format("%.0fkm", distanceMeters / 1000.0)
            } else if (distanceMeters >= 1000) {
                String.format("%.1fkm", distanceMeters / 1000.0)
            }else {
                String.format("%dm", distanceMeters)
            }
        }
    }
}