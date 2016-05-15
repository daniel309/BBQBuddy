package daniel309.bbqbuddy.persistence;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class TemperatureHistoryDBHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "TemperatureHistory.db";

    private static TemperatureHistoryDBHelper instance = null;


    private static final String SQL_CREATE_BBQEVENTS =
            "CREATE TABLE " + TemperatureHistoryContract.BBQEvents.TABLE_NAME + " (" +
                    TemperatureHistoryContract.BBQEvents._ID + " INTEGER PRIMARY KEY NOT NULL," +
                    TemperatureHistoryContract.BBQEvents.COLUMN_NAME_TITLE + " TEXT NOT NULL," +
                    TemperatureHistoryContract.BBQEvents.COLUMN_NAME_DATE + " TEXT NOT NULL" +
            " )";
    private static final String SQL_CREATE_TEMPHISTORY =
            "CREATE TABLE " + TemperatureHistoryContract.TemperatureHistory.TABLE_NAME + " (" +
                    TemperatureHistoryContract.TemperatureHistory._ID + " INTEGER PRIMARY KEY NOT NULL," +
                    TemperatureHistoryContract.TemperatureHistory.COLUMN_NAME_BBQEVENTID + " INTEGER NOT NULL," +
                    TemperatureHistoryContract.TemperatureHistory.COLUMN_NAME_PROBE1TEMP + " TEXT NOT NULL," +
                    TemperatureHistoryContract.TemperatureHistory.COLUMN_NAME_PROBE2TEMP + " TEXT NOT NULL," +
                    TemperatureHistoryContract.TemperatureHistory.COLUMN_NAME_PROBE1ALARM + " TEXT NOT NULL," +
                    TemperatureHistoryContract.TemperatureHistory.COLUMN_NAME_PROBE2ALARM + " TEXT NOT NULL," +
                    TemperatureHistoryContract.TemperatureHistory.COLUMN_NAME_DATEMEASURED + " TEXT NOT NULL" +
            " )";

    private static final String SQL_DROP_BBQEVENTS =
            "DROP TABLE IF EXISTS " + TemperatureHistoryContract.BBQEvents.TABLE_NAME;
    private static final String SQL_DROP_TEMPHISTORY =
            "DROP TABLE IF EXISTS " + TemperatureHistoryContract.TemperatureHistory.TABLE_NAME;


    private TemperatureHistoryDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized TemperatureHistoryDBHelper getInstance(Context context) {
        if (instance == null) {
            instance = new TemperatureHistoryDBHelper(context);
        }
        return instance;
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_BBQEVENTS);
        db.execSQL(SQL_CREATE_TEMPHISTORY);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // The upgrade policy is to simply to discard the data and start over
        db.execSQL(SQL_DROP_BBQEVENTS);
        db.execSQL(SQL_DROP_TEMPHISTORY);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
