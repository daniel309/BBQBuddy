package daniel309.bbqbuddy.persistence;

import android.provider.BaseColumns;


public final class TemperatureHistoryContract {
    public TemperatureHistoryContract() { }

    public static abstract class BBQEvents implements BaseColumns {
        public static final String TABLE_NAME = "bbqevents";
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_DATE = "date";
    }

    public static abstract class TemperatureHistory implements BaseColumns {
        public static final String TABLE_NAME = "temphistory";
        public static final String COLUMN_NAME_BBQEVENTID = "bbqeventid";
        public static final String COLUMN_NAME_PROBE1TEMP = "probe1temp";
        public static final String COLUMN_NAME_PROBE2TEMP = "probe2temp";
        public static final String COLUMN_NAME_PROBE1ALARM = "probe1alarm";
        public static final String COLUMN_NAME_PROBE2ALARM = "probe2alarm";
        public static final String COLUMN_NAME_DATEMEASURED = "datemeasured";
    }
}