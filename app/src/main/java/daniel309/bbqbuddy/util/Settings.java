package daniel309.bbqbuddy.util;

import java.io.Serializable;

import daniel309.bbqbuddy.MainActivity;


public class Settings implements Serializable {
    public Settings(MainActivity main) {
        mMainActivity = main;
    }

    public enum Probes {
        PROBE1,
        PROBE2
    }

    public boolean mVibrateOnAlarm = true;
    public boolean mDemoMode = false;
    public int mBatteryPercent = 100;
    public float mProbe1Temp = -99.9f;
    public float mProbe2Temp = -99.9f;

    private int mLEDBrightness = 35;
    private boolean mPowerSaveEnabled = false;
    private float mProbe1Alarm = 0.0f;
    private float mProbe2Alarm = 0.0f;
    private transient MainActivity mMainActivity;


    public synchronized void setMainActivity(MainActivity main) {
        mMainActivity = main;
    }

    public synchronized void setLEDBrightness(int percent) {
        mLEDBrightness = percent;
        if (mMainActivity.mBluetoothService != null) {
            mMainActivity.mBluetoothService.sendMessage(percent, getPowerSaveEnabled());
        }
    }
    public synchronized int getLEDBrightness() {
        return mLEDBrightness;
    }

    public synchronized void setPowerSaveEnabled(boolean enabled) {
        mPowerSaveEnabled = enabled;
        if (mMainActivity.mBluetoothService != null) {
            mMainActivity.mBluetoothService.sendMessage(getLEDBrightness(), enabled);
        }
    }
    public synchronized boolean getPowerSaveEnabled() { return mPowerSaveEnabled; }

    public synchronized void setProbe1Alarm(float alarm) {
        mProbe1Alarm = alarm;

        // setup countdown timer
        if (mMainActivity.mProbe1CountdownTimer != null) {
            mMainActivity.mProbe1CountdownTimer.cancel();
        }
        if (alarm > 0f && mProbe1Temp > -99.9f) {
            mMainActivity.mProbe1CountdownTimer =
                    mMainActivity.mTemperatureHistoryManager.calculateCountdownTimerForProbe(
                            Probes.PROBE1,
                            mProbe1Temp, mProbe1Alarm, mMainActivity);
            mMainActivity.mProbe1CountdownTimer.start();
        }
    }
    public synchronized float getProbe1Alarm() {
        return mProbe1Alarm;
    }


    public synchronized void setProbe2Alarm(float alarm) {
        mProbe2Alarm = alarm;

        // setup countdown timer
        if (mMainActivity.mProbe2CountdownTimer != null) {
            mMainActivity.mProbe2CountdownTimer.cancel();
        }
        if (alarm > 0f && mProbe2Temp > -99.9f) {
            mMainActivity.mProbe2CountdownTimer =
                    mMainActivity.mTemperatureHistoryManager.calculateCountdownTimerForProbe(
                            Probes.PROBE2,
                            mProbe2Temp, mProbe2Alarm, mMainActivity);
            mMainActivity.mProbe2CountdownTimer.start();
        }
    }
    public synchronized float getProbe2Alarm() {
        return mProbe2Alarm;
    }
}
