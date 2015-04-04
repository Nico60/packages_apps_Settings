
package com.android.settings.simpleaosp;

import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

import com.android.settings.AnimationScalePreference;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import android.view.IWindowManager;

public class NavigationBarSettings extends SettingsPreferenceFragment implements
OnPreferenceChangeListener, OnPreferenceClickListener {

    private static final String KEY_NAVIGATION_BAR_HEIGHT = "navigation_bar_height";
    private static final String SHOW_CLEAR_ALL_RECENTS = "show_clear_all_recents";
    private static final String RECENTS_CLEAR_ALL_LOCATION = "recents_clear_all_location";
    private static final String KILL_APP_LONGPRESS_BACK = "kill_app_longpress_back";
    private static final String LONG_PRESS_KILL_DELAY = "long_press_kill_delay";

    private SwitchPreference mRecentsClearAll;
    private ListPreference mRecentsClearAllLocation;

    private ListPreference mNavigationBarHeight;

    private SwitchPreference mKillAppLongpressBack;
    private AnimationScalePreference mKillAppLongpressDelay;
    
    private IWindowManager mWindowManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


		mWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));

        addPreferencesFromResource(R.xml.navigation_bar_settings);

		PreferenceScreen prefSet = getPreferenceScreen();
		ContentResolver resolver = getActivity().getContentResolver();

        mNavigationBarHeight = (ListPreference) findPreference(KEY_NAVIGATION_BAR_HEIGHT);
        mNavigationBarHeight.setOnPreferenceChangeListener(this);
        int statusNavigationBarHeight = Settings.System.getInt(getActivity().getApplicationContext()
                .getContentResolver(),
                Settings.System.NAVIGATION_BAR_HEIGHT, 48);
        mNavigationBarHeight.setValue(String.valueOf(statusNavigationBarHeight));
        mNavigationBarHeight.setSummary(mNavigationBarHeight.getEntry());

		mKillAppLongpressBack = findAndInitSwitchPref(KILL_APP_LONGPRESS_BACK);
		mKillAppLongpressBack.setOnPreferenceChangeListener(this);
        updateKillAppLongpressBackOptions();
     
        mKillAppLongpressDelay = findAndInitAnimationScalePreference(LONG_PRESS_KILL_DELAY);
        updateAnimationScaleValue(3, mKillAppLongpressDelay);
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (Utils.isMonkeyRunning()) {
            return false;
        }        
        if (preference == mKillAppLongpressBack) {
            writeKillAppLongpressBackOptions();
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        return false;
    }
    
    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mKillAppLongpressDelay ) {
            ((AnimationScalePreference) preference).click();
        }
        return false;
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mNavigationBarHeight) {
            int statusNavigationBarHeight = Integer.valueOf((String) objValue);
            int index = mNavigationBarHeight.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_HEIGHT, statusNavigationBarHeight);
            mNavigationBarHeight.setSummary(mNavigationBarHeight.getEntries()[index]);
        return true;
		} else if (preference == mRecentsClearAll) {
            boolean show = (Boolean) objValue;
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    Settings.System.SHOW_CLEAR_ALL_RECENTS, show ? 1 : 0, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mRecentsClearAllLocation) {
            int location = Integer.valueOf((String) objValue);
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    Settings.System.RECENTS_CLEAR_ALL_LOCATION, location, UserHandle.USER_CURRENT);
            updateRecentsLocation(location);
            return true;
		} else if (preference == mKillAppLongpressBack) {
            writeKillAppLongpressBackOptions();
            return true; 
		} else if (preference == mKillAppLongpressDelay) {
            writeAnimationScaleOption(3, mKillAppLongpressDelay, objValue);
            return true;
        }
        return false;
    }
    
	private SwitchPreference findAndInitSwitchPref(String key) {
        SwitchPreference pref = (SwitchPreference) findPreference(key);
        if (pref == null) {
            throw new IllegalArgumentException("Cannot find preference with key = " + key);
        }
        return pref;
    }

    private AnimationScalePreference findAndInitAnimationScalePreference(String key) {
        AnimationScalePreference pref = (AnimationScalePreference) findPreference(key);
        pref.setOnPreferenceChangeListener(this);
        pref.setOnPreferenceClickListener(this);
        return pref;
    }
    
    private void writeAnimationScaleOption(int which, AnimationScalePreference pref,
            Object newValue) {
        try {
            float scale = newValue != null ? Float.parseFloat(newValue.toString()) : 1;
            if (which == 3) {
                Settings.System.putIntForUser(getContentResolver(),
                        Settings.System.LONG_PRESS_KILL_DELAY, ((int) (scale * 1000)),
                        UserHandle.USER_CURRENT);
            } else {
                mWindowManager.setAnimationScale(which, scale);
            }
            updateAnimationScaleValue(which, pref);
        } catch (RemoteException ignored) {
        	throw new RuntimeException("An error occured while writing AnimationScalePreference new setting\n" +  ignored.getMessage());
        }
    }
    
    private void updateAnimationScaleValue(int which, AnimationScalePreference pref) {
        try {
            float scale;
            if (which == 3) {
                try {
                    scale = Settings.System.getIntForUser(getContentResolver(),
                            Settings.System.LONG_PRESS_KILL_DELAY,
                            UserHandle.USER_CURRENT);
                    scale = ((float) (scale) / 1000);
                } catch (Settings.SettingNotFoundException exc) {
                    scale = 1.0f;
                }
            } else {
                scale = mWindowManager.getAnimationScale(which);
                if (scale != 1) {
                    /* TODO: add a runtime exception */
                }
            }
            pref.setScale(scale);
        } catch (RemoteException ignored) {
        	throw new RuntimeException("An error occured while updating AnimationScalePreference.\n" +  ignored.getMessage());
        }
    }

    private void writeKillAppLongpressBackOptions() {
        Settings.System.putIntForUser(getActivity().getContentResolver(), Settings.System.KILL_APP_LONGPRESS_BACK, mKillAppLongpressBack.isChecked() ? 1 : 0, UserHandle.USER_CURRENT);
    }

    private void updateKillAppLongpressBackOptions() {
        try {
			mKillAppLongpressBack.setChecked(Settings.System.getIntForUser(getActivity().getContentResolver(), Settings.System.KILL_APP_LONGPRESS_BACK, UserHandle.USER_CURRENT)== 1);
        } catch (SettingNotFoundException e) {
			e.printStackTrace();
		}
	}

	private void updateRecentsLocation(int value) {
        ContentResolver resolver = getContentResolver();
        Resources res = getResources();
        int summary = -1;

        Settings.System.putInt(resolver, Settings.System.RECENTS_CLEAR_ALL_LOCATION, value);

        if (value == 0) {
            Settings.System.putInt(resolver, Settings.System.RECENTS_CLEAR_ALL_LOCATION, 0);
            summary = R.string.recents_clear_all_location_top_right;
        } else if (value == 1) {
            Settings.System.putInt(resolver, Settings.System.RECENTS_CLEAR_ALL_LOCATION, 1);
            summary = R.string.recents_clear_all_location_top_left;
 	} else if (value == 2) {
            Settings.System.putInt(resolver, Settings.System.RECENTS_CLEAR_ALL_LOCATION, 2);
            summary = R.string.recents_clear_all_location_bottom_right;
        } else if (value == 3) {
            Settings.System.putInt(resolver, Settings.System.RECENTS_CLEAR_ALL_LOCATION, 3);
            summary = R.string.recents_clear_all_location_bottom_left;
        }
        if (mRecentsClearAllLocation != null && summary != -1) {
            mRecentsClearAllLocation.setSummary(res.getString(summary));
        }
    }
}
