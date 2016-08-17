package daniel309.bbqbuddy.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import daniel309.bbqbuddy.MainActivity;
import daniel309.bbqbuddy.R;


public class ReadingsFragment extends Fragment {

    public static final String[] FOOD_TEMPERATURE_SELECTION = new String[] {
            "Internal temperature",
            "55°C - Filet, rare",
            "60°C - Salmon",
            "61°C - Filet, medium",
            "62°C - Tuna",
            "74°C - Ground meat",
            "78°C - Poultry",
            "82°C - Beef brisket",
            "85°C - Spare ribs",
            "93°C - Pulled pork",
            "99°C - Baked potato"
    };

    private static final String TAG = ReadingsFragment.class.getSimpleName();

    public TextView mProbe1Temp;
    public TextView mProbe2Temp;
    public TextView mBatteryPercent;
    public ProgressBar mBatteryProgress;
    public TextView mBTStatus;
    public TextView mProbe1Alarm;
    public TextView mProbe2Alarm;
    public TextView mProbe1Countdown;
    public TextView mProbe2Countdown;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.readings_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(view.getContext(),
                android.R.layout.simple_spinner_dropdown_item, FOOD_TEMPERATURE_SELECTION);

        // status stuff
        mBatteryPercent = (TextView) view.findViewById(R.id.battery_percent);
        mBatteryProgress = (ProgressBar) view.findViewById(R.id.battery_progress);
        mBTStatus = (TextView) view.findViewById(R.id.bt_status);

        // Probe 1 setup
        mProbe1Countdown = (TextView) view.findViewById(R.id.probe1_countdown);
        mProbe1Temp = (TextView) view.findViewById(R.id.probe1_temp);
        Spinner probe1Default = (Spinner) view.findViewById(R.id.probe1_default);
        mProbe1Alarm = (TextView) view.findViewById(R.id.probe1_alarm);

        mProbe1Alarm.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) ||
                        actionId == EditorInfo.IME_ACTION_DONE) {
                    if (v.getText().length() > 0) {
                        float alarm = Float.parseFloat(v.getText().toString());
                        ((MainActivity) getActivity()).mSettings.setProbe1Alarm(alarm);
                        ((MainActivity) getActivity()).mGraphFragment.modifyProbe1LimitLine(alarm,
                                v.getText() + " °C");
                    }
                    else {
                        ((MainActivity) getActivity()).mSettings.setProbe1Alarm(0f);
                        ((MainActivity) getActivity()).mGraphFragment.modifyProbe1LimitLine(0f, "");
                    }
                }
                return false;
            }
        });

        probe1Default.setAdapter(arrayAdapter);
        probe1Default.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    mProbe1Alarm.setText(FOOD_TEMPERATURE_SELECTION[position].substring(0,
                            FOOD_TEMPERATURE_SELECTION[position].indexOf('°')));

                    float alarm = Float.parseFloat("0" + mProbe1Alarm.getText());
                    ((MainActivity) getActivity()).mGraphFragment.modifyProbe1LimitLine(alarm,
                            FOOD_TEMPERATURE_SELECTION[position]);
                    ((MainActivity) getActivity()).mSettings.setProbe1Alarm(alarm);
                }
                else {
                    mProbe1Alarm.setText("");
                    mProbe1Countdown.setText("--:--");
                    ((MainActivity) getActivity()).mGraphFragment.modifyProbe1LimitLine(0f, "");
                    ((MainActivity) getActivity()).mSettings.setProbe1Alarm(0f);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });


        // Probe 2 Setup
        mProbe2Countdown = (TextView) view.findViewById(R.id.probe2_countdown);
        mProbe2Temp = (TextView) view.findViewById(R.id.probe2_temp);
        Spinner probe2Default = (Spinner) view.findViewById(R.id.probe2_default);
        mProbe2Alarm = (TextView) view.findViewById(R.id.probe2_alarm);

        mProbe2Alarm.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) ||
                        actionId == EditorInfo.IME_ACTION_DONE) {
                    if (v.getText().length() > 0) {
                        float alarm = Float.parseFloat(v.getText().toString());
                        ((MainActivity) getActivity()).mSettings.setProbe2Alarm(alarm);
                        ((MainActivity) getActivity()).mGraphFragment.modifyProbe2LimitLine(alarm,
                                v.getText() + " °C");
                    }
                    else {
                        ((MainActivity) getActivity()).mSettings.setProbe2Alarm(0f);
                        ((MainActivity) getActivity()).mGraphFragment.modifyProbe2LimitLine(0f, "");
                    }
                }
                return false;
            }
        });

        probe2Default.setAdapter(arrayAdapter);
        probe2Default.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    mProbe2Alarm.setText(FOOD_TEMPERATURE_SELECTION[position].substring(0,
                            FOOD_TEMPERATURE_SELECTION[position].indexOf('°')));

                    float alarm = Float.parseFloat("0" + mProbe2Alarm.getText());
                    ((MainActivity) getActivity()).mGraphFragment.modifyProbe2LimitLine(alarm,
                            FOOD_TEMPERATURE_SELECTION[position]);
                    ((MainActivity) getActivity()).mSettings.setProbe2Alarm(alarm);
                }
                else {
                    mProbe2Alarm.setText("");
                    mProbe2Countdown.setText("--:--");
                    ((MainActivity) getActivity()).mGraphFragment.modifyProbe2LimitLine(0f, "");
                    ((MainActivity) getActivity()).mSettings.setProbe2Alarm(0f);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            //Restore the fragment's state
            mProbe1Temp.setText(savedInstanceState.getCharSequence("mProbe1Temp"));
            mProbe2Temp.setText(savedInstanceState.getCharSequence("mProbe2Temp"));
            mBatteryPercent.setText(savedInstanceState.getCharSequence("mBatteryPercent"));
            mBatteryProgress.setProgress(savedInstanceState.getInt("mBatteryProgress"));
            mBTStatus.setText(savedInstanceState.getCharSequence("mBTStatus"));
            mProbe1Alarm.setText(savedInstanceState.getCharSequence("mProbe1Alarm"));
            mProbe2Alarm.setText(savedInstanceState.getCharSequence("mProbe2Alarm"));
            mProbe1Countdown.setText(savedInstanceState.getCharSequence("mProbe1Countdown"));
            mProbe2Countdown.setText(savedInstanceState.getCharSequence("mProbe2Countdown"));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //Save the fragment's state
        outState.putCharSequence("mProbe1Temp", mProbe1Temp.getText());
        outState.putCharSequence("mProbe2Temp", mProbe2Temp.getText());
        outState.putCharSequence("mBatteryPercent", mBatteryPercent.getText());
        outState.putInt("mBatteryProgress", mBatteryProgress.getProgress());
        outState.putCharSequence("mBTStatus", mBTStatus.getText());
        outState.putCharSequence("mProbe1Alarm", mProbe1Alarm.getText());
        outState.putCharSequence("mProbe2Alarm", mProbe2Alarm.getText());
        outState.putCharSequence("mProbe1Countdown", mProbe1Countdown.getText());
        outState.putCharSequence("mProbe2Countdown", mProbe2Countdown.getText());
    }

}
