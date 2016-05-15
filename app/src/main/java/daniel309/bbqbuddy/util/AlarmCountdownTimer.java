package daniel309.bbqbuddy.util;

import android.app.NotificationManager;
import android.content.Context;
import android.media.RingtoneManager;
import android.os.CountDownTimer;
import android.support.v4.app.NotificationCompat;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.util.Log;

import java.util.Enumeration;

import daniel309.bbqbuddy.MainActivity;
import daniel309.bbqbuddy.R;
import daniel309.bbqbuddy.fragments.ReadingsFragment;


public class AlarmCountdownTimer extends CountDownTimer {
    public AlarmCountdownTimer(long millisInFuture, long intervalInMillies, MainActivity main, Settings.Probes probe, String msg) {
        super(millisInFuture, intervalInMillies);
        Log.d(TAG, "New timer. future=" + millisInFuture + ", interval=" + intervalInMillies + ", probe=" + probe);

        mMillisInFuture = millisInFuture;
        mNotification = msg;
        mMainActivity = main;
        mProbeNumber = probe;
    }

    private static final String TAG = AlarmCountdownTimer.class.getSimpleName();

    //default visibility
    String mNotification;
    MainActivity mMainActivity;
    Settings.Probes mProbeNumber;
    long mMillisInFuture;


    @Override
    public void onTick(long millisUntilFinished) {
        ReadingsFragment frag = (ReadingsFragment) mMainActivity.getFragmentManager().findFragmentByTag(MainActivity.TAG_FRAGMENT_READINGS);
        if (frag != null) {
            String txt = String.format("%02d", millisUntilFinished/1000/60) + ":"
                       + String.format("%02d",(millisUntilFinished/1000)%60);

            switch (mProbeNumber) {
                case PROBE1: {
                    frag.mProbe1Countdown.setText(txt);
                    break;
                }
                case PROBE2: {
                    frag.mProbe2Countdown.setText(txt);
                    break;
                }
                //deliberately left out default: case, the com
            }
        }
    }

    @Override
    public void onFinish() {
        ReadingsFragment frag = (ReadingsFragment) mMainActivity.getFragmentManager().findFragmentByTag(MainActivity.TAG_FRAGMENT_READINGS);

        switch (mProbeNumber) {
            case PROBE1: {
                if (mMainActivity.mSettings.mProbe1Temp < mMainActivity.mSettings.getProbe1Alarm()) {
                    if (mMillisInFuture < 0) {
                        frag.mProbe1Countdown.setText("99:99"); // we have received a negative estimate for completion...
                    }
                    return; // skip notification
                }
                break;
            }
            case PROBE2: {
                if (mMainActivity.mSettings.mProbe2Temp < mMainActivity.mSettings.getProbe2Alarm()) {
                    if (mMillisInFuture < 0) {
                        frag.mProbe2Countdown.setText("99:99"); // we have received a negative estimate for completion...
                    }
                    return; // skip notification
                }
                break;
            }
        }

        if (frag != null) {
            //blink text
            Animation anim = new AlphaAnimation(0.0f, 1.0f);
            anim.setDuration(50); // frequency
            anim.setStartOffset(20);
            anim.setRepeatMode(Animation.REVERSE);
            anim.setRepeatCount(50);

            switch (mProbeNumber) {
                case PROBE1: {
                    frag.mProbe1Countdown.setText("00:00");
                    frag.mProbe1Countdown.startAnimation(anim);
                    frag.mProbe1Temp.startAnimation(anim);
                    break;
                }
                case PROBE2: {
                    frag.mProbe2Countdown.setText("00:00");
                    frag.mProbe2Countdown.startAnimation(anim);
                    frag.mProbe2Temp.startAnimation(anim);
                    break;
                }
            }
        }

        //notification
        NotificationManager mNotifyMgr = (NotificationManager) mMainActivity.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(mMainActivity.getApplicationContext())
                        .setSmallIcon(R.drawable.ic_menu_camera)
                        .setContentTitle("BBQ Buddy Alarm")
                        .setContentText(mNotification);
        builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
        if (mMainActivity.mSettings.mVibrateOnAlarm) {
            builder.setVibrate(new long[]{500, 500, 500, 500, 500, 500});
        }
        mNotifyMgr.notify(1, builder.build());
    }
}
