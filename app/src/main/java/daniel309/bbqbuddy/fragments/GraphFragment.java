package daniel309.bbqbuddy.fragments;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.YAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;

import daniel309.bbqbuddy.MainActivity;
import daniel309.bbqbuddy.R;
import daniel309.bbqbuddy.persistence.TemperatureHistoryManager;


public class GraphFragment extends Fragment {
    public Button mStartOverButton;
    public Button mDeleteButton;
    public Spinner mDateSpinner;
    public ArrayAdapter<String> mDateSpinnerAdapter;

    public LineChart mGraph;
    public int mLastXValue = 0;
    public LimitLine mProbe1LimitLine;
    public LimitLine mProbe2LimitLine;


    private static final String TAG = GraphFragment.class.getSimpleName();
    public static final String CURRENT_EVENT_LABEL = "Current Event";


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.graph_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        MainActivity main = (MainActivity) getActivity();

        mStartOverButton = (Button) view.findViewById(R.id.start_over_button);
        mStartOverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TemperatureHistoryManager m = ((MainActivity) getActivity()).mTemperatureHistoryManager;
                String date = m.getCurrentBBQEventDate(); // save previous date, create new event
                m.createNewBBQEvent("unused");
                mDateSpinnerAdapter.insert(date, 1); //add old date one line below the top item
                mDateSpinner.getOnItemSelectedListener().onItemSelected(null, null, 0, 0); //move to item 0 and refresh graph
            }
        });

        mDeleteButton = (Button) view.findViewById(R.id.delete_button);
        mDeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDateSpinner.getSelectedItem() != null) {
                    String selected = mDateSpinner.getSelectedItem().toString();
                    ((MainActivity) getActivity()).mTemperatureHistoryManager.removeBBQEvent(selected); // remove all values

                    if (!CURRENT_EVENT_LABEL.equals(selected)) { // refresh graph + update spinner
                        mDateSpinner.getOnItemSelectedListener().onItemSelected(null, null,
                                Math.min(mDateSpinner.getSelectedItemPosition() + 1, mDateSpinner.getCount() - 1),
                                0); //refresh graph with item one position above current
                        mDateSpinnerAdapter.remove(selected); // remove current item from spinner + refresh
                    }
                    else { //refresh graph only
                        mDateSpinner.getOnItemSelectedListener().onItemSelected(null, null, mDateSpinner.getSelectedItemPosition(), 0); //stay on current item (item 0) and refresh graph
                    }
                }
            }
        });

        mDateSpinner = (Spinner) view.findViewById(R.id.date_spinner);
        ArrayList<String> startTimes = new ArrayList<>(10);
        startTimes.add(CURRENT_EVENT_LABEL);
        startTimes.addAll(main.mTemperatureHistoryManager.getAllBBQEventStartTimes());
        mDateSpinnerAdapter = new ArrayAdapter<String>(view.getContext(),
                android.R.layout.simple_spinner_dropdown_item, startTimes);
        mDateSpinner.setAdapter(mDateSpinnerAdapter);
        mDateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                LineData data = ((MainActivity) getActivity()).mTemperatureHistoryManager.getAllTemperaturesForBBQEvent(
                        mDateSpinnerAdapter.getItem(position), createEmptyLineData(), GraphFragment.this);
                mGraph.setData(data);
                mGraph.moveViewToX(data.getXValCount()); // also calls invalidate()
                mLastXValue = data.getXValCount();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });


        mGraph = (LineChart) view.findViewById(R.id.graph);
        //mGraph.setLogEnabled(true);

        // background and borders
        mGraph.setAutoScaleMinMaxEnabled(true);
        //mGraph.setDrawGridBackground(true);
        //mGraph.setDrawBorders(true);

        //touch
        mGraph.setTouchEnabled(true);
        mGraph.setDragEnabled(true);
        mGraph.setScaleEnabled(true);
        mGraph.setPinchZoom(true);
        mGraph.setDoubleTapToZoomEnabled(false);

        //data
        LineData data = ((MainActivity) getActivity()).mTemperatureHistoryManager.getAllTemperaturesForBBQEvent(
                mDateSpinner.getSelectedItem().toString(), createEmptyLineData(), this);
        mGraph.setData(data);

        //legend, only there when data is set
        mGraph.getLegend().setEnabled(true);
        mGraph.getLegend().setPosition(Legend.LegendPosition.RIGHT_OF_CHART_INSIDE);
        mGraph.getAxisRight().setEnabled(false);
        mGraph.getAxisLeft().setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        mGraph.getAxisLeft().setDrawGridLines(false);
        mGraph.getAxisLeft().setValueFormatter(new YAxisValueFormatter() {
            @Override
            public String getFormattedValue(float v, YAxis yAxis) {
                return String.format("%3.1f °C", v);
            }
        });
        mGraph.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        mGraph.getXAxis().setSpaceBetweenLabels(8);
        mGraph.getXAxis().setDrawGridLines(false);
        //mGraph.getXAxis().setAvoidFirstLastClipping(true);
        //mGraph.getXAxis().setLabelRotationAngle(-45);
        mGraph.setDescription("");

        //draw
        mGraph.invalidate();
    }


    public void modifyProbe1LimitLine(float newLimit, String text) {
        YAxis leftAxis = mGraph.getAxisLeft();

        if (mProbe1LimitLine != null) {
            leftAxis.removeLimitLine(mProbe1LimitLine);
            leftAxis.resetAxisMaxValue();
        }

        if (newLimit > 0f) {
            mProbe1LimitLine = new LimitLine(newLimit, text);
            mProbe1LimitLine.setLineColor(Color.parseColor("#5882FA")); //4DD2FF"));
            mProbe1LimitLine.setLineWidth(2f);
            mProbe1LimitLine.setTextColor(Color.BLACK);
            mProbe1LimitLine.setTextSize(12f);
            mProbe1LimitLine.enableDashedLine(5f, 1f, 1f);
            leftAxis.addLimitLine(mProbe1LimitLine);

            float max = 0f;
            for (LimitLine ll : leftAxis.getLimitLines()) {
                max = Math.max(ll.getLimit(), max);
            }
            leftAxis.setAxisMaxValue(max + 10f);
        }

        mGraph.invalidate();
    }

    public void modifyProbe2LimitLine(float newLimit, String text) {
        YAxis leftAxis = mGraph.getAxisLeft();

        if (mProbe2LimitLine != null) {
            leftAxis.removeLimitLine(mProbe2LimitLine);
            leftAxis.resetAxisMaxValue();
        }

        if (newLimit > 0f) {
            mProbe2LimitLine = new LimitLine(newLimit, text);
            mProbe2LimitLine.setLineColor(Color.parseColor("#FF4DD2"));
            mProbe2LimitLine.setLineWidth(2f);
            mProbe2LimitLine.setTextColor(Color.BLACK);
            mProbe2LimitLine.setTextSize(12f);
            mProbe2LimitLine.enableDashedLine(5f, 1f, 1f);
            leftAxis.addLimitLine(mProbe2LimitLine);

            float max = 0f;
            for (LimitLine ll : leftAxis.getLimitLines()) {
                max = Math.max(ll.getLimit(), max);
            }
            leftAxis.setAxisMaxValue(max + 10f);
        }

        mGraph.invalidate();
    }

    public LineData createEmptyLineData() {
        ArrayList<Entry> valsProbe1 = new ArrayList<>(1000);
        ArrayList<Entry> valsProbe2 = new ArrayList<>(1000);

        LineDataSet setProbe1 = new LineDataSet(valsProbe1, "Probe 1");
        setProbe1.setAxisDependency(YAxis.AxisDependency.LEFT);
        setProbe1.setColor(Color.parseColor("#0F4BFF"));
        setProbe1.setDrawCircles(false);
        setProbe1.setDrawValues(false);
        setProbe1.setDrawHighlightIndicators(false);

        LineDataSet setProbe2 = new LineDataSet(valsProbe2, "Probe 2");
        setProbe2.setAxisDependency(YAxis.AxisDependency.LEFT);
        setProbe2.setColor(Color.parseColor("#D24DFF"));
        setProbe2.setDrawCircles(false);
        setProbe2.setDrawValues(false);
        setProbe2.setDrawHighlightIndicators(false);

        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(setProbe1);
        dataSets.add(setProbe2);

        ArrayList<String> xVals = new ArrayList<>(1000);
        return new LineData(xVals, dataSets);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            //Restore the fragment's state
            mDateSpinner.setSelection(savedInstanceState.getInt("selectedDate"));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //Save the fragment's state
        outState.putInt("selectedDate", mDateSpinner.getSelectedItemPosition());
    }

}
