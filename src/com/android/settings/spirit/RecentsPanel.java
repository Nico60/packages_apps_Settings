/*
 * Copyright (C) 2013 SlimRoms Project
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

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.util.Helpers;
import com.android.internal.util.slim.DeviceUtils;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class RecentsPanel extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String TAG = "RecentsPanelSettings";

    private static final String RECENT_MENU_CLEAR_ALL = "recent_menu_clear_all";
    private static final String RECENT_MENU_CLEAR_ALL_LOCATION = "recent_menu_clear_all_location";

    private static final String RECENTS_USE_SLIM = "recents_use_slim";
    private static final String RECENT_PANEL_LEFTY_MODE = "recent_panel_lefty_mode";
    private static final String RECENT_PANEL_SCALE = "recent_panel_scale";
    private static final String RECENT_PANEL_EXPANDED_MODE = "recent_panel_expanded_mode";
    private static final String RECENT_PANEL_SHOW_TOPMOST = "recent_panel_show_topmost";
    private static final String RECENT_PANEL_BG_COLOR =	"recent_panel_bg_color";
    private static final String RECENT_CARD_BG_COLOR = "recent_card_bg_color";
    private static final String RECENT_CARD_TEXT_COLOR = "recent_card_text_color";

    private CheckBoxPreference mRecentClearAll;
    private CheckBoxPreference mRecentsUseSlim;
    private CheckBoxPreference mRecentPanelLeftyMode;
    private ListPreference mRecentClearAllPosition;
    private ListPreference mRecentPanelScale;
    private ListPreference mRecentPanelExpandedMode;
    private CheckBoxPreference mRecentsShowTopmost;
    private ColorPickerPreference mRecentPanelBgColor;
    private ColorPickerPreference mRecentCardBgColor;
    private ColorPickerPreference mRecentCardTextColor;

    private static final int MENU_RESET = Menu.FIRST;
    private static final int DEFAULT_BACKGROUND_COLOR = 0x00ffffff;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.recents_apps_panel);

        PreferenceScreen prefSet = getPreferenceScreen();

        boolean useSlimRecents = false;

        try {
            useSlimRecents = Settings.System.getInt(getContentResolver(), Settings.System.RECENTS_USE_SLIM) == 1;
        } catch(SettingNotFoundException e) {
               e.printStackTrace();
        }

        mRecentClearAll = (CheckBoxPreference) prefSet.findPreference(RECENT_MENU_CLEAR_ALL);
        mRecentClearAll.setChecked(Settings.System.getInt(getContentResolver(),
            Settings.System.SHOW_CLEAR_RECENTS_BUTTON, 1) == 1);
        mRecentClearAll.setOnPreferenceChangeListener(this);
        mRecentClearAll.setEnabled(!useSlimRecents);

        mRecentClearAllPosition = (ListPreference) prefSet.findPreference(RECENT_MENU_CLEAR_ALL_LOCATION);
        String recentClearAllPosition = Settings.System.getString(getContentResolver(), Settings.System.CLEAR_RECENTS_BUTTON_LOCATION);
        if (recentClearAllPosition != null) {
             mRecentClearAllPosition.setValue(recentClearAllPosition);
        }
        mRecentClearAllPosition.setOnPreferenceChangeListener(this);
        mRecentClearAllPosition.setEnabled(!useSlimRecents);

        // Slim recents
        mRecentsUseSlim = (CheckBoxPreference) prefSet.findPreference(RECENTS_USE_SLIM);
        mRecentsUseSlim.setChecked(useSlimRecents);
        mRecentsUseSlim.setOnPreferenceChangeListener(this);

        mRecentPanelLeftyMode = (CheckBoxPreference) findPreference(RECENT_PANEL_LEFTY_MODE);
        mRecentPanelLeftyMode.setOnPreferenceChangeListener(this);

        mRecentPanelScale = (ListPreference) findPreference(RECENT_PANEL_SCALE);
        mRecentPanelScale.setOnPreferenceChangeListener(this);

        mRecentPanelExpandedMode = (ListPreference) findPreference(RECENT_PANEL_EXPANDED_MODE);
        mRecentPanelExpandedMode.setOnPreferenceChangeListener(this);

        boolean enableRecentsShowTopmost = Settings.System.getInt(getContentResolver(),
                                      Settings.System.RECENT_PANEL_SHOW_TOPMOST, 0) == 1;
        mRecentsShowTopmost = (CheckBoxPreference) findPreference(RECENT_PANEL_SHOW_TOPMOST);
        mRecentsShowTopmost.setChecked(enableRecentsShowTopmost);
        mRecentsShowTopmost.setOnPreferenceChangeListener(this);

        final boolean recentLeftyMode = Settings.System.getInt(getContentResolver(),
                Settings.System.RECENT_PANEL_GRAVITY, Gravity.RIGHT) == Gravity.LEFT;
        mRecentPanelLeftyMode.setChecked(recentLeftyMode);

        final int recentScale = Settings.System.getInt(getContentResolver(),
                Settings.System.RECENT_PANEL_SCALE_FACTOR, 100);
        mRecentPanelScale.setValue(recentScale + "");

        final int recentExpandedMode = Settings.System.getInt(getContentResolver(),
                Settings.System.RECENT_PANEL_EXPANDED_MODE, 0);
        mRecentPanelExpandedMode.setValue(recentExpandedMode + "");

        // Recent panel background color
        mRecentPanelBgColor =
                (ColorPickerPreference) findPreference(RECENT_PANEL_BG_COLOR);
        mRecentPanelBgColor.setOnPreferenceChangeListener(this);
        final int intColor = Settings.System.getInt(getContentResolver(),
                Settings.System.RECENT_PANEL_BG_COLOR, 0x00ffffff);
        String hexColor = String.format("#%08x", (0x00ffffff & intColor));
        if (hexColor.equals("#00ffffff")) {
            mRecentPanelBgColor.setSummary(R.string.trds_default_color);
        } else {
            mRecentPanelBgColor.setSummary(hexColor);
        }
        mRecentPanelBgColor.setNewPreviewColor(intColor);

        // Recent card background color
        mRecentCardBgColor =
                (ColorPickerPreference) findPreference(RECENT_CARD_BG_COLOR);
        mRecentCardBgColor.setOnPreferenceChangeListener(this);
        final int intColorCard = Settings.System.getInt(getContentResolver(),
                Settings.System.RECENT_CARD_BG_COLOR, 0x00ffffff);
        String hexColorCard = String.format("#%08x", (0x00ffffff & intColorCard));
        if (hexColorCard.equals("#00ffffff")) {
            mRecentCardBgColor.setSummary(R.string.trds_default_color);
        } else {
            mRecentCardBgColor.setSummary(hexColorCard);
        }
        mRecentCardBgColor.setNewPreviewColor(intColorCard);

        // Recent card text color
        mRecentCardTextColor =
                (ColorPickerPreference) findPreference(RECENT_CARD_TEXT_COLOR);
        mRecentCardTextColor.setOnPreferenceChangeListener(this);
        final int intColorText = Settings.System.getInt(getContentResolver(),
                Settings.System.RECENT_CARD_TEXT_COLOR, 0x00ffffff);
        String hexColorText = String.format("#%08x", (0x00ffffff & intColorText));
        if (hexColorText.equals("#00ffffff")) {
            mRecentCardTextColor.setSummary(R.string.trds_default_color);
        } else {
            mRecentCardTextColor.setSummary(hexColorText);
        }
        mRecentCardTextColor.setNewPreviewColor(intColorText);

        // Enable options menu for color reset
        setHasOptionsMenu(true);

    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mRecentClearAll) {
            Settings.System.putInt(getContentResolver(), Settings.System.SHOW_CLEAR_RECENTS_BUTTON,
                    (Boolean) newValue ? 1 : 0);
            return true;
        } else if (preference == mRecentClearAllPosition) {
            Settings.System.putString(getContentResolver(), Settings.System.CLEAR_RECENTS_BUTTON_LOCATION, (String) newValue);
            return true;
        } else if (preference == mRecentsUseSlim) {
            boolean useSlimRecents = (Boolean) newValue;

            Settings.System.putInt(getContentResolver(), Settings.System.RECENTS_USE_SLIM,
                    useSlimRecents ? 1 : 0);

            // Give user information that Slim Recents needs restart SystemUI
            openSlimRecentsWarning();

            mRecentsUseSlim.setChecked(useSlimRecents);

            // Update default recents UI components
            mRecentClearAll.setEnabled(!useSlimRecents);
            mRecentClearAllPosition.setEnabled(!useSlimRecents);
            return true;
        } else if (preference == mRecentPanelScale) {
            int value = Integer.parseInt((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.RECENT_PANEL_SCALE_FACTOR, value);
            return true;
        } else if (preference == mRecentPanelExpandedMode) {
            int value = Integer.parseInt((String) newValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.RECENT_PANEL_EXPANDED_MODE, value);
            return true;
        } else if (preference == mRecentPanelLeftyMode) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.RECENT_PANEL_GRAVITY,
                    ((Boolean) newValue) ? Gravity.LEFT : Gravity.RIGHT);
            return true;
        } else if (preference == mRecentsShowTopmost) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.RECENT_PANEL_SHOW_TOPMOST,
                    ((Boolean) newValue) ? 1 : 0);
            return true;
        } else if (preference == mRecentPanelBgColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            if (hex.equals("#00ffffff")) {
                preference.setSummary(R.string.trds_default_color);
            } else {
                preference.setSummary(hex);
            }
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.RECENT_PANEL_BG_COLOR,
                    intHex);
            return true;
        } else if (preference == mRecentCardBgColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            if (hex.equals("#00ffffff")) {
                preference.setSummary(R.string.trds_default_color);
            } else {
                preference.setSummary(hex);
            }
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.RECENT_CARD_BG_COLOR,
                    intHex);
            return true;
        } else if (preference == mRecentCardTextColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            if (hex.equals("#00ffffff")) {
                preference.setSummary(R.string.trds_default_color);
            } else {
                preference.setSummary(hex);
            }
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.RECENT_CARD_TEXT_COLOR,
                    intHex);
            return true;
        }
        return false;
    }

    private void openSlimRecentsWarning() {
        new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.slim_recents_warning_title))
            .setMessage(getResources().getString(R.string.slim_recents_warning_message))
            .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    Helpers.restartSystemUI();
                }
            }).show();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset_default_message)
                .setIcon(R.drawable.ic_settings_backup)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                resetToDefault();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void resetToDefault() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle(R.string.shortcut_action_reset);
        alertDialog.setMessage(R.string.qs_style_reset_message);
        alertDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                resetValues();
            }
        });
        alertDialog.setNegativeButton(R.string.cancel, null);
        alertDialog.create().show();
    }

    private void resetValues() {
        Settings.System.putInt(getContentResolver(),
                Settings.System.RECENT_PANEL_BG_COLOR, DEFAULT_BACKGROUND_COLOR);
        mRecentPanelBgColor.setNewPreviewColor(DEFAULT_BACKGROUND_COLOR);
        mRecentPanelBgColor.setSummary(R.string.trds_default_color);
    }

}
