/*
 * Copyright (C) 2013 Slimroms
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

package com.android.settings.quicksettings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.internal.util.slim.DeviceUtils;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.android.settings.widget.SeekBarPreference;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class QuickSettingsTilesStyle extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String TAG = "QuickSettingsTilesStyle";

    private static final String PREF_FLIP_QS_TILES = "flip_qs_tiles";

    private static final String KEY_CUSTOM_COLOR = "quick_tiles_custom_color";

    private static final String PREF_TILES_PER_ROW =
            "tiles_per_row";
    private static final String PREF_TILES_PER_ROW_DUPLICATE_LANDSCAPE =
            "tiles_per_row_duplicate_landscape";
    private static final String PREF_ADDITIONAL_OPTIONS =
            "quicksettings_tiles_style_additional_options";
    private static final String PREF_QUICK_TILES_BG_COLOR =
            "quick_tiles_bg_color";
    private static final String PREF_QUICK_TILES_BG_PRESSED_COLOR =
            "quick_tiles_bg_pressed_color";
    private static final String PREF_QUICK_TILES_TEXT_COLOR =
             "quick_tiles_text_color";
    private static final int DEFAULT_QUICK_TILES_BG_COLOR =
            0xff161616;
    private static final int DEFAULT_QUICK_TILES_BG_PRESSED_COLOR =
            0xff212121;
    private static final int DEFAULT_QUICK_TILES_TEXT_COLOR =
            0xffcccccc;

    private static final int MENU_RESET = Menu.FIRST;

    private static final int DLG_RESET = 0;

    private ListPreference mTilesPerRow;
    private CheckBoxPreference mDuplicateColumnsLandscape;
    private CheckBoxPreference mCustomColor;
    private ColorPickerPreference mQuickTilesBgColor;
    private ColorPickerPreference mQuickTilesBgPressedColor;
    private ColorPickerPreference mQuickTilesTextColor;
    private CheckBoxPreference mFlipQsTiles;

    private boolean mCheckPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        refreshSettings();
    }

    private PreferenceScreen refreshSettings() {
        mCheckPreferences = false;
        PreferenceScreen prefs = getPreferenceScreen();
        if (prefs != null) {
            prefs.removeAll();
        }

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.quicksettings_tiles_style);

        prefs = getPreferenceScreen();
        ContentResolver resolver = getContentResolver();

        PackageManager pm = getPackageManager();
        Resources systemUiResources;
        try {
            systemUiResources = pm.getResourcesForApplication("com.android.systemui");
        } catch (Exception e) {
            Log.e(TAG, "can't access systemui resources",e);
            return null;
        }

        int intColor;

        mFlipQsTiles = (CheckBoxPreference) findPreference(PREF_FLIP_QS_TILES);
        mFlipQsTiles.setChecked(Settings.System.getInt(resolver,
                Settings.System.QUICK_SETTINGS_TILES_FLIP, 0) == 1);

        mCustomColor = (CheckBoxPreference) findPreference(KEY_CUSTOM_COLOR);
        mCustomColor.setChecked(Settings.System.getInt(resolver,
                Settings.System.QUICK_TILES_CUSTOM_COLOR, 0) == 1);

        mQuickTilesBgColor = (ColorPickerPreference) findPreference(PREF_QUICK_TILES_BG_COLOR);
        mQuickTilesBgColor.setAlphaSliderEnabled(true);
        mQuickTilesBgColor.setNewPreviewColor(DEFAULT_QUICK_TILES_BG_COLOR);
        mQuickTilesBgColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.QUICK_TILES_BG_COLOR, -2);
        if (intColor == -2) {
            intColor = DEFAULT_QUICK_TILES_BG_COLOR;
        } else {
            mQuickTilesBgColor.setNewPreviewColor(intColor);
        }

        mQuickTilesBgPressedColor = (ColorPickerPreference) findPreference(PREF_QUICK_TILES_BG_PRESSED_COLOR);
        mQuickTilesBgPressedColor.setAlphaSliderEnabled(true);
        mQuickTilesBgPressedColor.setNewPreviewColor(DEFAULT_QUICK_TILES_BG_PRESSED_COLOR);
        mQuickTilesBgPressedColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.QUICK_TILES_BG_PRESSED_COLOR, -2);
        if (intColor == -2) {
            intColor = DEFAULT_QUICK_TILES_BG_PRESSED_COLOR;
        } else {
            mQuickTilesBgPressedColor.setNewPreviewColor(intColor);
        }

        mQuickTilesTextColor = (ColorPickerPreference) findPreference(PREF_QUICK_TILES_TEXT_COLOR);
        mQuickTilesTextColor.setAlphaSliderEnabled(true);
        mQuickTilesTextColor.setNewPreviewColor(DEFAULT_QUICK_TILES_TEXT_COLOR);
        mQuickTilesTextColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.QUICK_TILES_TEXT_COLOR, -2);
        if (intColor == -2) {
            intColor = DEFAULT_QUICK_TILES_TEXT_COLOR;
        } else {
            mQuickTilesTextColor.setNewPreviewColor(intColor);
        }

        mTilesPerRow = (ListPreference) prefs.findPreference(PREF_TILES_PER_ROW);
        int tilesPerRow = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.QUICK_TILES_PER_ROW, 3);
        mTilesPerRow.setValue(String.valueOf(tilesPerRow));
        mTilesPerRow.setSummary(mTilesPerRow.getEntry());
        mTilesPerRow.setOnPreferenceChangeListener(this);

        mDuplicateColumnsLandscape =
            (CheckBoxPreference) findPreference(PREF_TILES_PER_ROW_DUPLICATE_LANDSCAPE);
        mDuplicateColumnsLandscape.setChecked(Settings.System.getInt(
                getActivity().getContentResolver(),
                Settings.System.QUICK_TILES_PER_ROW_DUPLICATE_LANDSCAPE, 1) == 1);
        mDuplicateColumnsLandscape.setOnPreferenceChangeListener(this);

        PreferenceCategory additionalOptions =
            (PreferenceCategory) findPreference(PREF_ADDITIONAL_OPTIONS);
        if (!DeviceUtils.isPhone(getActivity())) {
            additionalOptions.removePreference(
                findPreference(PREF_TILES_PER_ROW_DUPLICATE_LANDSCAPE));
        }

        updateColorPrefs();
        setHasOptionsMenu(true);
        mCheckPreferences = true;
        return prefs;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset)
                .setIcon(R.drawable.ic_settings_backup) // use the backup icon
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                showDialogInner(DLG_RESET);
                return true;
             default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        ContentResolver resolver = getContentResolver();
        if (preference == mFlipQsTiles) {
            Settings.System.putInt(resolver,
                    Settings.System.QUICK_SETTINGS_TILES_FLIP,
                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
            return true;
        } else if (preference == mCustomColor) {
            Settings.System.putInt(resolver,
                    Settings.System.QUICK_TILES_CUSTOM_COLOR,
                    mCustomColor.isChecked() ? 1 : 0);
            updateColorPrefs();
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!mCheckPreferences) {
            return false;
        }
        if (preference == mTilesPerRow) {
            int index = mTilesPerRow.findIndexOfValue((String) newValue);
            int value = Integer.valueOf((String) newValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.QUICK_TILES_PER_ROW,
                    value);
            mTilesPerRow.setSummary(mTilesPerRow.getEntries()[index]);
            return true;
        } else if (preference == mDuplicateColumnsLandscape) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.QUICK_TILES_PER_ROW_DUPLICATE_LANDSCAPE,
                    (Boolean) newValue ? 1 : 0);
            return true;
        } else if (preference == mQuickTilesBgColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getContentResolver(),
                   Settings.System.QUICK_TILES_BG_COLOR,
                   intHex);
           return true;
        } else if (preference == mQuickTilesBgPressedColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getContentResolver(),
                   Settings.System.QUICK_TILES_BG_PRESSED_COLOR,
                   intHex);
           return true;
        } else if (preference == mQuickTilesTextColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.QUICK_TILES_TEXT_COLOR,
                    intHex);
            return true;

        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void updateColorPrefs() {
        if (mCustomColor.isChecked()) {
            mQuickTilesBgColor.setEnabled(true);
            mQuickTilesBgPressedColor.setEnabled(true);
            mQuickTilesTextColor.setEnabled(true);
        } else {
            mQuickTilesBgColor.setEnabled(false);
            mQuickTilesBgPressedColor.setEnabled(false);
            mQuickTilesTextColor.setEnabled(false);
        }
    }

    private void showDialogInner(int id) {
        DialogFragment newFragment = MyAlertDialogFragment.newInstance(id);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(int id) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            frag.setArguments(args);
            return frag;
        }

        QuickSettingsTilesStyle getOwner() {
            return (QuickSettingsTilesStyle) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DLG_RESET:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.reset)
                    .setMessage(R.string.qs_style_reset_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.System.putInt(getActivity().getContentResolver(),
                                    Settings.System.QUICK_TILES_BG_COLOR, -2);
                            Settings.System.putInt(getActivity().getContentResolver(),
                                    Settings.System.QUICK_TILES_BG_PRESSED_COLOR, -2);
                            Settings.System.putInt(getActivity().getContentResolver(),
                                    Settings.System.QUICK_TILES_TEXT_COLOR, -2);
                            getOwner().refreshSettings();
                        }
                    })
                    .create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }

        @Override
        public void onCancel(DialogInterface dialog) {
        }
     }

}
