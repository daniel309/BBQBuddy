package daniel309.bbqbuddy.persistence;

import android.content.ContentValues;
import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import daniel309.bbqbuddy.BuildConfig;
import daniel309.bbqbuddy.MainActivity;
import daniel309.bbqbuddy.fragments.GraphFragment;
import daniel309.bbqbuddy.util.AlarmCountdownTimer;
import daniel309.bbqbuddy.util.BluetoothService;
import daniel309.bbqbuddy.util.Settings;


public class TemperatureHistoryManager {

    private static final String TAG = TemperatureHistoryManager.class.getSimpleName();
    private SQLiteDatabase mDatabase;
    private long mCurrentBBQEventID = 0;
    private String mCurrentBBQEventDate;

    public static final String SQLITE_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss"; //sqlite compatible date string


    public TemperatureHistoryManager(Context ctx, long currentBBQEventID, String currentBBQEventDate) {
        mDatabase = TemperatureHistoryDBHelper.getInstance(ctx).getWritableDatabase();

        if (currentBBQEventID > 0) {
            synchronized (this) {
                mCurrentBBQEventID = currentBBQEventID;
                mCurrentBBQEventDate = currentBBQEventDate;
            }
        }
        else {
            createNewBBQEvent("unused");
        }
    }

    public long createNewBBQEvent(String title) {
        String date = new SimpleDateFormat(SQLITE_DATETIME_FORMAT).format(Calendar.getInstance().getTime());
        ContentValues values = new ContentValues();
        values.put(TemperatureHistoryContract.BBQEvents.COLUMN_NAME_TITLE, title);
        values.put(TemperatureHistoryContract.BBQEvents.COLUMN_NAME_DATE, date);

        long newRowId = mDatabase.insert(
                TemperatureHistoryContract.BBQEvents.TABLE_NAME,
                null,
                values);

        synchronized (this) {
            mCurrentBBQEventID = newRowId;
            mCurrentBBQEventDate = date;
        }

        return newRowId;
    }


    private long getEventIDForDate(String date) {
        long ret = -1;

        if (GraphFragment.CURRENT_EVENT_LABEL.equals(date)) {
            return getCurrentBBQEventID();
        }

        Cursor c = mDatabase.rawQuery(
                "select " + TemperatureHistoryContract.BBQEvents._ID + " from " + TemperatureHistoryContract.BBQEvents.TABLE_NAME +
                        " where " + TemperatureHistoryContract.BBQEvents.COLUMN_NAME_DATE + " = ?",
                new String[]{date}
        );
        if (c.getCount() == 1) {
            c.moveToFirst();
            ret = c.getLong(c.getColumnIndex(TemperatureHistoryContract.BBQEvents._ID));
        }
        c.close();

        return ret;
    }

    public void removeBBQEvent(String date) {
        long id = getEventIDForDate(date);
        if (id != -1) {
            mDatabase.execSQL(
                    "delete from " + TemperatureHistoryContract.TemperatureHistory.TABLE_NAME + " where " +
                            TemperatureHistoryContract.TemperatureHistory.COLUMN_NAME_BBQEVENTID + " = ?",
                    new Long[]{id}
            );

            // only delete corresponding event if its not the current event
            if (!GraphFragment.CURRENT_EVENT_LABEL.equals(date)) {
                mDatabase.execSQL(
                        "delete from " + TemperatureHistoryContract.BBQEvents.TABLE_NAME + " where " +
                                TemperatureHistoryContract.BBQEvents._ID + " = ?",
                        new Long[]{id}
                );
            }
        }
    }

    public ArrayList<String> getAllBBQEventStartTimes() {
        ArrayList<String> ret = new ArrayList<>(20);

        Cursor c = mDatabase.rawQuery(
                "select " + TemperatureHistoryContract.BBQEvents.COLUMN_NAME_DATE + " from " + TemperatureHistoryContract.BBQEvents.TABLE_NAME +
                        " where " + TemperatureHistoryContract.BBQEvents._ID + " <> ? order by 1 desc",
                new String[]{
                        String.valueOf(getCurrentBBQEventID())
                }
        );

        c.moveToFirst();
        while (!c.isAfterLast()) {
            ret.add(c.getString(c.getColumnIndex(TemperatureHistoryContract.BBQEvents.COLUMN_NAME_DATE)));
            c.moveToNext();
        }
        c.close();

        return ret;
    }

    public LineData getAllTemperaturesForBBQEvent(String date, LineData data, GraphFragment frag) {
        long id = getEventIDForDate(date);
        if (id == -1) { // id for date not found
            return data;
        }

        Cursor c = mDatabase.rawQuery(
                "select " +
                        TemperatureHistoryContract.TemperatureHistory.COLUMN_NAME_PROBE1TEMP + "," +
                        TemperatureHistoryContract.TemperatureHistory.COLUMN_NAME_PROBE2TEMP + "," +
                        TemperatureHistoryContract.TemperatureHistory.COLUMN_NAME_PROBE1ALARM + "," +
                        TemperatureHistoryContract.TemperatureHistory.COLUMN_NAME_PROBE2ALARM + "," +
                        TemperatureHistoryContract.TemperatureHistory.COLUMN_NAME_DATEMEASURED + " " +
                        " from " + TemperatureHistoryContract.TemperatureHistory.TABLE_NAME +
                        " where " + TemperatureHistoryContract.TemperatureHistory.COLUMN_NAME_BBQEVENTID + " = ?",
                new String[]{
                        String.valueOf(id)
                }
        );

        int count = c.getCount();
        if (count > 0) {
            SimpleDateFormat f = new SimpleDateFormat(SQLITE_DATETIME_FORMAT);
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            int dateColIndex = c.getColumnIndex(TemperatureHistoryContract.TemperatureHistory.COLUMN_NAME_DATEMEASURED);
            int probe1Index = c.getColumnIndex(TemperatureHistoryContract.TemperatureHistory.COLUMN_NAME_PROBE1TEMP);
            int probe2Index = c.getColumnIndex(TemperatureHistoryContract.TemperatureHistory.COLUMN_NAME_PROBE2TEMP);
            int probe1AlarmIndex = c.getColumnIndex(TemperatureHistoryContract.TemperatureHistory.COLUMN_NAME_PROBE1ALARM);
            int probe2AlarmIndex = c.getColumnIndex(TemperatureHistoryContract.TemperatureHistory.COLUMN_NAME_PROBE2ALARM);
            float p1Alarm = 0f;
            float p2Alarm = 0f;

            ILineDataSet probe1Set = data.getDataSetByIndex(0);
            ILineDataSet probe2Set = data.getDataSetByIndex(1);

            c.moveToFirst();
            for (int i = 0; i < count; i++) {
                try {
                    Date d = f.parse(c.getString(dateColIndex));
                    float p1 = c.getFloat(probe1Index);
                    float p2 = c.getFloat(probe2Index);

                    if (p1 > -99.9 || p2 > -99.9) {
                        data.getXVals().add(sdf.format(d));
                        if (p1 > -99.9) {
                            probe1Set.addEntry(new Entry(p1, i));
                        }
                        if (p2 > -99.9) {
                            probe2Set.addEntry(new Entry(p2, i));
                        }
                    }

                    if (i > count - 5) { //little optimization here: only look at the last 4 alarm values,
                        // we only need the last one because we dont draw a graph, but a just limit line
                        p1Alarm = c.getFloat(probe1AlarmIndex);
                        p2Alarm = c.getFloat(probe2AlarmIndex);
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }

                c.moveToNext();
            }

            frag.modifyProbe1LimitLine(p1Alarm, p1Alarm + " °C");
            frag.modifyProbe2LimitLine(p2Alarm, p2Alarm + " °C");
        }
        c.close();

        return data;
    }

    public long addCurrentTemperatureEvent(BluetoothService.BBQBuddyMessage msg, float probe1Alarm, float probe2Alarm) {
        if (msg.probe1Temp <= -99.9 && msg.probe2Temp <= -99.9) {
            return -1;
        }

        ContentValues values = new ContentValues();
        values.put(TemperatureHistoryContract.TemperatureHistory.COLUMN_NAME_BBQEVENTID, getCurrentBBQEventID());
        values.put(TemperatureHistoryContract.TemperatureHistory.COLUMN_NAME_PROBE1TEMP, msg.probe1Temp);
        values.put(TemperatureHistoryContract.TemperatureHistory.COLUMN_NAME_PROBE2TEMP, msg.probe2Temp);
        values.put(TemperatureHistoryContract.TemperatureHistory.COLUMN_NAME_PROBE1ALARM, probe1Alarm);
        values.put(TemperatureHistoryContract.TemperatureHistory.COLUMN_NAME_PROBE2ALARM, probe2Alarm);
        values.put(TemperatureHistoryContract.TemperatureHistory.COLUMN_NAME_DATEMEASURED,
                new SimpleDateFormat(SQLITE_DATETIME_FORMAT).format(msg.timeMeasured));

        long newRowId = mDatabase.insert(
                TemperatureHistoryContract.TemperatureHistory.TABLE_NAME,
                null,
                values);

        return newRowId;
    }

    public AlarmCountdownTimer calculateCountdownTimerForProbe(Settings.Probes probeNo, float currentTemp, float alarmTemp, MainActivity main) {
        float delta = alarmTemp - currentTemp;
        float secsToGo = 0;
        final int SECONDS_INTERVAL = 60;
        final int SECONDS_SMOOTH = 6;

        Log.d(TAG, "calculating alarm for probe " + probeNo + ": current=" + currentTemp + ", alarm=" + alarmTemp);

        if(currentTemp >= alarmTemp) { // return a timer that fires immediately
            return new AlarmCountdownTimer(0, 1000, main, probeNo,
                    "Probe " + probeNo + " alarm temperature reached: " + alarmTemp + " °C");
        }

        String tempColName = "";
        switch (probeNo) {
            case PROBE1:
                tempColName = TemperatureHistoryContract.TemperatureHistory.COLUMN_NAME_PROBE1TEMP;
                break;
            case PROBE2:
                tempColName = TemperatureHistoryContract.TemperatureHistory.COLUMN_NAME_PROBE2TEMP;
                break;
        }

        String now = new SimpleDateFormat(SQLITE_DATETIME_FORMAT).format(Calendar.getInstance().getTime());

        Cursor c = mDatabase.rawQuery(
                "select avg(" + tempColName + ")" +
                        " from " +
                        TemperatureHistoryContract.TemperatureHistory.TABLE_NAME +
                        " where " +
                        TemperatureHistoryContract.TemperatureHistory.COLUMN_NAME_BBQEVENTID +
                        " = ? and " +
                        TemperatureHistoryContract.TemperatureHistory.COLUMN_NAME_DATEMEASURED +
                        " between datetime(?, ?) and datetime(?, ?)" +
                "union all " +
                "select avg(" + tempColName + ")" +
                        " from " +
                        TemperatureHistoryContract.TemperatureHistory.TABLE_NAME +
                        " where " +
                        TemperatureHistoryContract.TemperatureHistory.COLUMN_NAME_BBQEVENTID +
                        " = ? and " +
                        TemperatureHistoryContract.TemperatureHistory.COLUMN_NAME_DATEMEASURED +
                        " between datetime(?, ?) and datetime(?)"
                ,
                new String[]{
                        String.valueOf(getCurrentBBQEventID()),
                        now,
                        "-" + SECONDS_INTERVAL + " seconds",
                        now,
                        "-" + (SECONDS_INTERVAL - SECONDS_SMOOTH) + " seconds",
                        String.valueOf(getCurrentBBQEventID()),
                        now,
                        "-" + SECONDS_SMOOTH + " seconds",
                        now
                }
        );

        if (BuildConfig.DEBUG && c.getCount() != 2) {
            throw new AssertionError("c.getCount() != 2");
        }

        c.moveToFirst();
        float avgTemp1 = c.getFloat(0);
        c.moveToNext();
        float avgTemp2 = c.getFloat(0);
        c.close();

        Log.d(TAG, "avg1=" + avgTemp1 + ", avg2=" + avgTemp2 + ", delta=" + delta + ", interval=" + SECONDS_INTERVAL);

        //calculate a simple, linear approximation
        secsToGo = delta / ((avgTemp2 - avgTemp1) / (float)SECONDS_INTERVAL);

        Log.d(TAG, "approximated secs to go:" + secsToGo);

        return new AlarmCountdownTimer((int)secsToGo * 1000, 1000, main, probeNo,
                "Probe " + probeNo + " alarm temperature reached: " + alarmTemp + " °C");
    }



    public long getCurrentBBQEventID() {
        long ret;
        synchronized (this) {
            ret = mCurrentBBQEventID;
        }
        return ret;
    }

    public String getCurrentBBQEventDate() {
        String ret;
        synchronized (this) {
            ret = mCurrentBBQEventDate;
        }
        return ret;
    }

    public void closeDatabase() {
        mDatabase.close();
    }
}
