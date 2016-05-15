package daniel309.bbqbuddy.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.util.Log;

import daniel309.bbqbuddy.MainActivity;
import daniel309.bbqbuddy.R;
import daniel309.bbqbuddy.util.BluetoothService;


public class SettingsFragment extends Fragment {
    public SeekBar mLEDBrightnessSeek;
    public TextView mLEDBrightnessText;
    public Switch mVibrateSwitch;
    public Switch mDemoSwitch;
    public Switch mPowerSwitch;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.settings_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mLEDBrightnessSeek = (SeekBar) view.findViewById(R.id.led_seek);
        mLEDBrightnessText = (TextView) view.findViewById(R.id.led_percent);

        mLEDBrightnessSeek.setProgress(((MainActivity) getActivity()).mSettings.getLEDBrightness());
        mLEDBrightnessText.setText(String.valueOf(((MainActivity) getActivity()).mSettings.getLEDBrightness()) + "%");

        mLEDBrightnessSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
                mLEDBrightnessText.setText(progresValue + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                ((MainActivity) getActivity()).mSettings.setLEDBrightness(seekBar.getProgress());
                mLEDBrightnessText.setText(seekBar.getProgress() + "%");
            }
        });

        mVibrateSwitch = (Switch) view.findViewById(R.id.switch_vibrate);
        mVibrateSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ((MainActivity) getActivity()).mSettings.mVibrateOnAlarm = isChecked;
            }
        });

        mDemoSwitch = (Switch) view.findViewById(R.id.switch_demo);
        mDemoSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ((MainActivity)getActivity()).mSettings.mDemoMode = isChecked;

                BluetoothService s = ((MainActivity)getActivity()).mBluetoothService;
                if (isChecked) {
                    if (s != null) s.startDemo();
                }
                else {
                    if (s != null) s.stop();
                }
            }
        });

        mPowerSwitch = (Switch) view.findViewById(R.id.switch_power);
        mPowerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ((MainActivity) getActivity()).mSettings.setPowerSaveEnabled(isChecked);
            }
        });

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            //Restore the fragment's state
            mLEDBrightnessSeek.setProgress(savedInstanceState.getInt("mLEDBrightnessSeek"));
            mLEDBrightnessText.setText(savedInstanceState.getCharSequence("mLEDBrightnessText"));
            mVibrateSwitch.setChecked(savedInstanceState.getBoolean("mVibrateSwitch"));
            mDemoSwitch.setChecked(savedInstanceState.getBoolean("mDemoSwitch"));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //Save the fragment's state
        outState.putInt("mLEDBrightnessSeek", mLEDBrightnessSeek.getProgress());
        outState.putCharSequence("mLEDBrightnessText", mLEDBrightnessText.getText());
        outState.putBoolean("mVibrateSwitch", mVibrateSwitch.isChecked());
        outState.putBoolean("mDemoSwitch", mDemoSwitch.isChecked());
    }

}
