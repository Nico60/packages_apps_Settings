/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.spirit;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.net.TrafficStats;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.settings.chameleonos.SeekBarPreferenceChOS;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class NetworkIndicator extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String TAG = "NetworkIndicator";

    private static final String NETWORK_TRAFFIC_AUTOHIDE = "network_traffic_autohide";
    private static final String NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD = "network_traffic_autohide_threshold";
    private static final String NETWORK_TRAFFIC_PERIOD = "network_traffic_period";
    private static final String NETWORK_TRAFFIC_STATE = "network_traffic_state";
    private static final String NETWORK_TRAFFIC_UNIT = "network_traffic_unit";
    private static final String STATUS_BAR_NETWORK_ACTIVITY = "status_bar_network_activity";

    private CheckBoxPreference mNetTrafficAutohide;
    private CheckBoxPreference mStatusBarNetworkActivity;
    private ListPreference mNetTrafficState;
    private ListPreference mNetTrafficUnit;
    private ListPreference mNetTrafficPeriod;
    private SeekBarPreferenceChOS mNetTrafficAutohideThreshold;

    private int mNetTrafficVal;
    private int MASK_UP;
    private int MASK_DOWN;
    private int MASK_UNIT;
    private int MASK_PERIOD;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.network_indicator);

        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();

        loadResources();

        mStatusBarNetworkActivity =
                (CheckBoxPreference) prefSet.findPreference(STATUS_BAR_NETWORK_ACTIVITY);
        mStatusBarNetworkActivity.setChecked(Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_NETWORK_ACTIVITY, 0) == 1);
        mStatusBarNetworkActivity.setOnPreferenceChangeListener(this);

        mNetTrafficState = (ListPreference) prefSet.findPreference(NETWORK_TRAFFIC_STATE);
        mNetTrafficUnit = (ListPreference) prefSet.findPreference(NETWORK_TRAFFIC_UNIT);
        mNetTrafficPeriod = (ListPreference) prefSet.findPreference(NETWORK_TRAFFIC_PERIOD);

        mNetTrafficAutohide =
            (CheckBoxPreference) prefSet.findPreference(NETWORK_TRAFFIC_AUTOHIDE);
        mNetTrafficAutohide.setChecked((Settings.System.getInt(getContentResolver(),
                            Settings.System.NETWORK_TRAFFIC_AUTOHIDE, 0) == 1));
        mNetTrafficAutohide.setOnPreferenceChangeListener(this);

        mNetTrafficAutohideThreshold = (SeekBarPreferenceChOS) prefSet.findPreference(NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD);
        int netTrafficAutohideThreshold = Settings.System.getInt(resolver,
                    Settings.System.NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD, 10);
            mNetTrafficAutohideThreshold.setValue(netTrafficAutohideThreshold / 1);
            mNetTrafficAutohideThreshold.setOnPreferenceChangeListener(this);

        // TrafficStats will return UNSUPPORTED if the device does not support it.
        if (TrafficStats.getTotalTxBytes() != TrafficStats.UNSUPPORTED &&
                TrafficStats.getTotalRxBytes() != TrafficStats.UNSUPPORTED) {
            mNetTrafficVal = Settings.System.getInt(resolver, Settings.System.NETWORK_TRAFFIC_STATE, 0);
            int intIndex = mNetTrafficVal & (MASK_UP + MASK_DOWN);
            intIndex = mNetTrafficState.findIndexOfValue(String.valueOf(intIndex));
            if (intIndex <= 0) {
                mNetTrafficUnit.setEnabled(false);
                mNetTrafficPeriod.setEnabled(false);
                mNetTrafficAutohide.setEnabled(false);
                mNetTrafficAutohideThreshold.setEnabled(false);
            }
            mNetTrafficState.setValueIndex(intIndex >= 0 ? intIndex : 0);
            mNetTrafficState.setSummary(mNetTrafficState.getEntry());
            mNetTrafficState.setOnPreferenceChangeListener(this);

            mNetTrafficUnit.setValueIndex(getBit(mNetTrafficVal, MASK_UNIT) ? 1 : 0);
            mNetTrafficUnit.setSummary(mNetTrafficUnit.getEntry());
            mNetTrafficUnit.setOnPreferenceChangeListener(this);

            intIndex = (mNetTrafficVal & MASK_PERIOD) >>> 16;
            intIndex = mNetTrafficPeriod.findIndexOfValue(String.valueOf(intIndex));
            mNetTrafficPeriod.setValueIndex(intIndex >= 0 ? intIndex : 1);
            mNetTrafficPeriod.setSummary(mNetTrafficPeriod.getEntry());
            mNetTrafficPeriod.setOnPreferenceChangeListener(this);
        } else {
            prefSet.removePreference(findPreference(NETWORK_TRAFFIC_STATE));
            prefSet.removePreference(findPreference(NETWORK_TRAFFIC_UNIT));
            prefSet.removePreference(findPreference(NETWORK_TRAFFIC_PERIOD));
            prefSet.removePreference(findPreference(NETWORK_TRAFFIC_AUTOHIDE));
            prefSet.removePreference(findPreference(NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD));
        }

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mStatusBarNetworkActivity) {
             boolean value = (Boolean) objValue;
            Settings.System.putInt(resolver, Settings.System.STATUS_BAR_NETWORK_ACTIVITY,
                    value ? 1 : 0);
            return true;
        } else if (preference == mNetTrafficState) {
            int intState = Integer.valueOf((String)objValue);
            mNetTrafficVal = setBit(mNetTrafficVal, MASK_UP, getBit(intState, MASK_UP));
            mNetTrafficVal = setBit(mNetTrafficVal, MASK_DOWN, getBit(intState, MASK_DOWN));
            Settings.System.putInt(resolver, Settings.System.NETWORK_TRAFFIC_STATE, mNetTrafficVal);
            int index = mNetTrafficState.findIndexOfValue((String) objValue);
            mNetTrafficState.setSummary(mNetTrafficState.getEntries()[index]);
            if (intState == 0) {
                mNetTrafficUnit.setEnabled(false);
                mNetTrafficPeriod.setEnabled(false);
                mNetTrafficAutohide.setEnabled(false);
                mNetTrafficAutohideThreshold.setEnabled(false);
            } else {
                mNetTrafficUnit.setEnabled(true);
                mNetTrafficPeriod.setEnabled(true);
                mNetTrafficAutohide.setEnabled(true);
                mNetTrafficAutohideThreshold.setEnabled(true);
            }
            return true;
        } else if (preference == mNetTrafficUnit) {
            // 1 = Display as Byte/s; default is bit/s
            mNetTrafficVal = setBit(mNetTrafficVal, MASK_UNIT, ((String)objValue).equals("1"));
            Settings.System.putInt(resolver, Settings.System.NETWORK_TRAFFIC_STATE, mNetTrafficVal);
            int index = mNetTrafficUnit.findIndexOfValue((String) objValue);
            mNetTrafficUnit.setSummary(mNetTrafficUnit.getEntries()[index]);
            return true;
        } else if (preference == mNetTrafficPeriod) {
            int intState = Integer.valueOf((String)objValue);
            mNetTrafficVal = setBit(mNetTrafficVal, MASK_PERIOD, false) + (intState << 16);
            Settings.System.putInt(resolver, Settings.System.NETWORK_TRAFFIC_STATE, mNetTrafficVal);
            int index = mNetTrafficPeriod.findIndexOfValue((String) objValue);
            mNetTrafficPeriod.setSummary(mNetTrafficPeriod.getEntries()[index]);
            return true;
        } else if (preference == mNetTrafficAutohide) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(resolver, Settings.System.NETWORK_TRAFFIC_AUTOHIDE,
                    value ? 1 : 0);
            return true;
        } else if (preference == mNetTrafficAutohideThreshold) {
            int netTrafficAutohideThreshold = (Integer) objValue;
            Settings.System.putInt(getContentResolver(),
                    Settings.System.NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD, netTrafficAutohideThreshold * 1);
            return true;
        }
        return false;
    }

    private void loadResources() {
        Resources resources = getActivity().getResources();
        MASK_UP = resources.getInteger(R.integer.maskUp);
        MASK_DOWN = resources.getInteger(R.integer.maskDown);
        MASK_UNIT = resources.getInteger(R.integer.maskUnit);
        MASK_PERIOD = resources.getInteger(R.integer.maskPeriod);
    }

    // intMask should only have the desired bit(s) set
    private int setBit(int intNumber, int intMask, boolean blnState) {
        if (blnState) {
            return (intNumber | intMask);
        }
        return (intNumber & ~intMask);
    }

    private boolean getBit(int intNumber, int intMask) {
        return (intNumber & intMask) == intMask;
    }

}
