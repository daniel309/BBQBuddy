package daniel309.bbqbuddy;


import android.app.Activity;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.text.SimpleDateFormat;


import daniel309.bbqbuddy.fragments.GraphFragment;
import daniel309.bbqbuddy.fragments.ReadingsFragment;
import daniel309.bbqbuddy.fragments.SettingsFragment;
import daniel309.bbqbuddy.util.AlarmCountdownTimer;
import daniel309.bbqbuddy.util.BluetoothService;
import daniel309.bbqbuddy.util.Settings;
import daniel309.bbqbuddy.persistence.TemperatureHistoryManager;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    // Intent request codes
    private static final int REQUEST_ENABLE_BT = 1;

    // fragment tags
    public static final String TAG_FRAGMENT_READINGS = "readings_frag";
    public static final String TAG_FRAGMENT_GRAPH = "graph_frag";
    public static final String TAG_FRAGMENT_SETTINGS = "settings_frag";
    //fragments
    private ReadingsFragment mReadingsFragment;
    private SettingsFragment mSettingsFragment;
    public GraphFragment mGraphFragment;
    private int mSelectedMenuItemID = 0;

    public TemperatureHistoryManager mTemperatureHistoryManager;
    public Settings mSettings;
    public AlarmCountdownTimer mProbe1CountdownTimer;
    public AlarmCountdownTimer mProbe2CountdownTimer;

    public BluetoothService mBluetoothService;
    public BluetoothAdapter mBluetoothAdapter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        long currentBBQEventID = -1;
        String currentBBQEventDate = "";

        if (savedInstanceState != null) {
            //Restore fragment instances
            mReadingsFragment = (ReadingsFragment)getFragmentManager().getFragment(savedInstanceState, "mReadingsFragment");
            mSettingsFragment = (SettingsFragment)getFragmentManager().getFragment(savedInstanceState, "mSettingsFragment");
            mGraphFragment = (GraphFragment)getFragmentManager().getFragment(savedInstanceState, "mGraphFragment");
            //Restore state
            mSelectedMenuItemID = savedInstanceState.getInt("mSelectedMenuItemID");
            mSettings = (Settings)savedInstanceState.getSerializable("mSettings");
            mSettings.setMainActivity(this);
            currentBBQEventID = savedInstanceState.getLong("CurrentBBQEventID");
            currentBBQEventDate = savedInstanceState.getString("CurrentBBQEventDate");
        }
        if (mReadingsFragment == null) {
            mReadingsFragment = new ReadingsFragment();
        }
        if (mSettingsFragment == null) {
            mSettingsFragment = new SettingsFragment();
        }
        if (mGraphFragment == null) {
            mGraphFragment = new GraphFragment();
        }

        if (mTemperatureHistoryManager == null) {
            mTemperatureHistoryManager = new TemperatureHistoryManager(getApplicationContext(),
                    currentBBQEventID, currentBBQEventDate);
        }
        if (mSettings == null) {
            mSettings = new Settings(this);
        }

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // add any fragment that does not exist already
        FragmentTransaction tx = getFragmentManager().beginTransaction();
        if (!mReadingsFragment.isAdded()) {
            tx.add(R.id.content_main, mReadingsFragment, TAG_FRAGMENT_READINGS);
            tx.hide(mReadingsFragment);
        }

        if (!mSettingsFragment.isAdded()) {
            tx.add(R.id.content_main, mSettingsFragment, TAG_FRAGMENT_SETTINGS);
            tx.hide(mSettingsFragment);
        }

        if (!mGraphFragment.isAdded()) {
            tx.add(R.id.content_main, mGraphFragment, TAG_FRAGMENT_GRAPH);
            tx.hide(mGraphFragment);
        }
        if (!tx.isEmpty()) tx.commit();

        //show+hide logic. this is to cache fragments as long as possible
        //some of them (like the graph) are quite expensive to create
        tx = getFragmentManager().beginTransaction();
        switch (mSelectedMenuItemID) {
            case R.id.nav_readings:
                tx.show(mReadingsFragment);
                tx.hide(mSettingsFragment);
                tx.hide(mGraphFragment);
                break;
            case R.id.nav_graph:
                tx.show(mGraphFragment);
                tx.hide(mSettingsFragment);
                tx.hide(mReadingsFragment);
                break;
            case R.id.nav_settings:
                tx.show(mSettingsFragment);
                tx.hide(mReadingsFragment);
                tx.hide(mGraphFragment);
                break;
        }
        if (tx.isEmpty()) {
            tx.show(mReadingsFragment); // show default fragment if nothing selected
            tx.hide(mSettingsFragment);
            tx.hide(mGraphFragment);
        }
        tx.commit();

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            ReadingsFragment frag = (ReadingsFragment) getFragmentManager().findFragmentByTag(TAG_FRAGMENT_READINGS);
            if (frag != null && frag.mBTStatus != null) frag.mBTStatus.setText("Disabled");
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // If BT is not on, request that it be enabled.
        if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
        else {
            startBluetoothService();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when the ACTION_REQUEST_ENABLE activity returns.

        startBluetoothService();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //Save the fragment instances
        getFragmentManager().putFragment(outState, "mReadingsFragment", mReadingsFragment);
        getFragmentManager().putFragment(outState, "mSettingsFragment", mSettingsFragment);
        getFragmentManager().putFragment(outState, "mGraphFragment", mGraphFragment);
        //save state
        outState.putInt("mSelectedMenuItemID", mSelectedMenuItemID);
        outState.putSerializable("mSettings", mSettings);
        outState.putLong("CurrentBBQEventID", mTemperatureHistoryManager.getCurrentBBQEventID());
        outState.putString("CurrentBBQEventDate", mTemperatureHistoryManager.getCurrentBBQEventDate());
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        mSelectedMenuItemID = item.getItemId();

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        if (mSelectedMenuItemID == R.id.nav_readings) {
            transaction.show(mReadingsFragment);
            transaction.hide(mSettingsFragment);
            transaction.hide(mGraphFragment);
        }
        else if (mSelectedMenuItemID == R.id.nav_graph) {
            transaction.show(mGraphFragment);
            transaction.hide(mReadingsFragment);
            transaction.hide(mSettingsFragment);
        }
        else if (mSelectedMenuItemID == R.id.nav_settings) {
            transaction.show(mSettingsFragment);
            transaction.hide(mReadingsFragment);
            transaction.hide(mGraphFragment);
        }
        transaction.addToBackStack(null);
        transaction.commit();

        // Highlight the selected item, update the title, and close the drawer
        item.setChecked(true);
        setTitle(item.getTitle());
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer != null) drawer.closeDrawer(GravityCompat.START);

        return true;
    }


    private void startBluetoothService() {
        if (mBluetoothService == null) {
            mBluetoothService = new BluetoothService(this, true); // use secure
        }

        if (mSettings.mDemoMode) {
            if (mBluetoothService.getState() != BluetoothService.State.Demo) {
                mBluetoothService.startDemo();
            }
        }
        else {
            if (mBluetoothService.getState() != BluetoothService.State.Connected &&
                    mBluetoothService.getState() != BluetoothService.State.Connecting &&
                    mBluetoothService.getState() != BluetoothService.State.Starting) {
                mBluetoothService.start();
            }
        }

    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    public final Handler mHandler = new Handler() {
        public int mNumTempEventsProcessed = 0;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BluetoothService.HANDLER_MESSAGE_STATE_CHANGED: {
                    String btState = msg.getData().getString(BluetoothService.KEY_STATE);
                    ReadingsFragment frag = (ReadingsFragment) getFragmentManager().findFragmentByTag(TAG_FRAGMENT_READINGS);
                    if (frag != null) frag.mBTStatus.setText(btState);
                    break;
                }
                case BluetoothService.HANDLER_MESSAGE_TOAST: {
                    Toast.makeText(MainActivity.this, msg.getData().getString(BluetoothService.KEY_TOAST),
                            Toast.LENGTH_LONG).show();
                    break;
                }
                case BluetoothService.HANDLER_MESSAGE_BBQ_TEMP: {
                    BluetoothService.BBQBuddyMessage bbq = (BluetoothService.BBQBuddyMessage) msg.getData().getSerializable(BluetoothService.KEY_BBQ);

                    int percentRounded = Math.round(bbq.batteryPercent / 5f) * 5; // round to the nearest 5%

                    //update settings
                    mSettings.mBatteryPercent = percentRounded;
                    mSettings.mProbe1Temp = bbq.probe1Temp;
                    mSettings.mProbe2Temp = bbq.probe2Temp;

                    // update readings frag
                    ReadingsFragment frag = (ReadingsFragment) getFragmentManager().findFragmentByTag(MainActivity.TAG_FRAGMENT_READINGS);
                    if (frag != null) {
                        if (bbq.probe1Temp > -99.9f) frag.mProbe1Temp.setText(String.format("%4.1f °C", bbq.probe1Temp));
                        else frag.mProbe1Temp.setText("---.- °C");

                        if (bbq.probe2Temp > -99.9f) frag.mProbe2Temp.setText(String.format("%4.1f °C", bbq.probe2Temp));
                        else frag.mProbe2Temp.setText("---.- °C");


                        frag.mBatteryPercent.setText(percentRounded + "%");
                        frag.mBatteryProgress.setProgress(percentRounded);
                    }

                    // update graph frag
                    GraphFragment frag1 = (GraphFragment) getFragmentManager().findFragmentByTag(MainActivity.TAG_FRAGMENT_GRAPH);
                    if (frag1 != null && frag1.mDateSpinner.getSelectedItemPosition() == 0) { //"current event" selected
                        if (bbq.probe1Temp > -99.9f || bbq.probe2Temp > -99.9f) {
                            LineData data = frag1.mGraph.getData();
                            data.addXValue(new SimpleDateFormat("HH:mm:ss").format(bbq.timeMeasured));

                            ILineDataSet probe1Set = data.getDataSetByIndex(0);
                            ILineDataSet probe2Set = data.getDataSetByIndex(1);
                            if (bbq.probe1Temp > -99.9f) {
                                data.addEntry(new Entry(bbq.probe1Temp, frag1.mLastXValue, probe1Set.getEntryCount()), 0);
                            }
                            if (bbq.probe2Temp > -99.9f) {
                                data.addEntry(new Entry(bbq.probe2Temp, frag1.mLastXValue, probe2Set.getEntryCount()), 1);
                            }

                            frag1.mLastXValue++;
                            data.notifyDataChanged();
                            frag1.mGraph.notifyDataSetChanged();
                            frag1.mGraph.moveViewToX(data.getXValCount());// also calls invalidate()
                        }
                    }

                    // re-calculate alarm timers
                    if (mNumTempEventsProcessed % 30 == 0) { // re-calculate every 30 events
                        if (mSettings.getProbe1Alarm() > 0) {
                            mSettings.setProbe1Alarm(mSettings.getProbe1Alarm()); // re-calculate
                        }
                        if (mSettings.getProbe2Alarm() > 0) {
                            mSettings.setProbe2Alarm(mSettings.getProbe2Alarm()); // re-calculate
                        }
                    }
                    // double-check if we unexpectedly hit the alarm
                    if (mNumTempEventsProcessed % 5 == 0) { // check every 5 events
                        if (mSettings.getProbe1Alarm() > 0 && mSettings.mProbe1Temp >= mSettings.getProbe1Alarm()) {
                            mSettings.setProbe1Alarm(mSettings.getProbe1Alarm()); // re-calculate
                        }
                        if (mSettings.getProbe2Alarm() > 0 && mSettings.mProbe2Temp >= mSettings.getProbe2Alarm()) {
                            mSettings.setProbe2Alarm(mSettings.getProbe2Alarm()); // re-calculate
                        }
                    }

                    mNumTempEventsProcessed++;
                    break;
                }
            }
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled
                    startBluetoothService();
                }
                else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(MainActivity.this, "Bluetooth is not enabled",
                            Toast.LENGTH_LONG).show();
                    ReadingsFragment frag = (ReadingsFragment) getFragmentManager().findFragmentByTag(TAG_FRAGMENT_READINGS);
                    if (frag != null) frag.mBTStatus.setText("Disabled");
                }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBluetoothService != null) {
            mBluetoothService.stop();
        }
        if (mTemperatureHistoryManager != null) {
            mTemperatureHistoryManager.closeDatabase();
        }
    }
}