/*
 * Copyright (C) 2009 The Android Open Source Project
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
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManagerGlobal;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class NavbarSettings extends SettingsPreferenceFragment  implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "NavBar";
    private static final String ENABLE_NAVIGATION_BAR = "enable_nav_bar";
    private static final String PREF_STYLE_DIMEN = "navbar_dimen_settings";
    private static final String CATEGORY_NAVBAR = "navigation_bar";
    private static final String CATEGORY_NAVRING = "navigation_ring";
    private static final String KEY_NAVIGATION_BAR_LEFT = "navigation_bar_left";

    CheckBoxPreference mNavigationBarLeftPref;
    CheckBoxPreference mEnableNavigationBar;
    PreferenceScreen mStyleDimenPreference;
    PreferenceScreen mNavigationBar;
    PreferenceScreen mNavigationBarRing;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.navbar_settings);
        PreferenceScreen prefScreen = getPreferenceScreen();

        mNavigationBar = (PreferenceScreen) findPreference(CATEGORY_NAVBAR);
        mNavigationBarRing = (PreferenceScreen) findPreference(CATEGORY_NAVRING);
        mStyleDimenPreference = (PreferenceScreen) findPreference(PREF_STYLE_DIMEN);
        mNavigationBarLeftPref = (CheckBoxPreference) findPreference(KEY_NAVIGATION_BAR_LEFT);

        // Booleans to enable/disable nav bar
        // overriding overlays
        boolean hasNavBarByDefault = getResources().getBoolean(
                com.android.internal.R.bool.config_showNavigationBar);
        boolean enableNavigationBar = Settings.System.getInt(getContentResolver(),
                Settings.System.NAVIGATION_BAR_SHOW, hasNavBarByDefault ? 1 : 0) == 1;
        mEnableNavigationBar = (CheckBoxPreference) findPreference(ENABLE_NAVIGATION_BAR);
        mEnableNavigationBar.setChecked(enableNavigationBar);
        mEnableNavigationBar.setOnPreferenceChangeListener(this);

        try {
            boolean hasNavBar = WindowManagerGlobal.getWindowManagerService().hasNavigationBar();

            if (hasNavBar) {
                prefScreen.removePreference(mEnableNavigationBar);
                prefScreen.removePreference(mNavigationBarLeftPref);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error getting navigation bar status");
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
         if (preference == mEnableNavigationBar) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_SHOW,
                    ((Boolean) objValue) ? 1 : 0);
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

}
