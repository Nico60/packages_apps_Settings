package com.android.settings;

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

import com.android.settings.AnimationScalePreference;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import android.view.IWindowManager;

public class UiCustomizationSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener, OnPreferenceClickListener {


    private IWindowManager mWindowManager;
	private static final String SETTINGS_TITLE = "User Interface general customization";
    private static final String SHOW_CLEAR_ALL_RECENTS = "show_clear_all_recents";
    private static final String RECENTS_CLEAR_ALL_LOCATION = "recents_clear_all_location";
    
    
    private static final String WINDOW_ANIMATION_SCALE_KEY = "window_animation_scale";
    private static final String TRANSITION_ANIMATION_SCALE_KEY = "transition_animation_scale";
    private static final String ANIMATOR_DURATION_SCALE_KEY = "animator_duration_scale";    


    private SwitchPreference mRecentsClearAll;
    private ListPreference mRecentsClearAllLocation;
    
	private AnimationScalePreference mWindowAnimationScale;
    private AnimationScalePreference mTransitionAnimationScale;
    private AnimationScalePreference mAnimatorDurationScale;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		mWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));

        addPreferencesFromResource(R.xml.ui_customization);

		PreferenceScreen prefSet = getPreferenceScreen();
		ContentResolver resolver = getActivity().getContentResolver();
		
		
		mWindowAnimationScale = findAndInitAnimationScalePreference(WINDOW_ANIMATION_SCALE_KEY);
        mTransitionAnimationScale = findAndInitAnimationScalePreference(TRANSITION_ANIMATION_SCALE_KEY);
        mAnimatorDurationScale = findAndInitAnimationScalePreference(ANIMATOR_DURATION_SCALE_KEY); 
		updateAnimationScaleOptions();

        mRecentsClearAll = (SwitchPreference) prefSet.findPreference(SHOW_CLEAR_ALL_RECENTS);
        mRecentsClearAll.setChecked(Settings.System.getIntForUser(resolver,
        Settings.System.SHOW_CLEAR_ALL_RECENTS, 0, UserHandle.USER_CURRENT) == 1);
        mRecentsClearAll.setOnPreferenceChangeListener(this);

        mRecentsClearAllLocation = (ListPreference) prefSet.findPreference(RECENTS_CLEAR_ALL_LOCATION);
        int location = Settings.System.getIntForUser(resolver,
                Settings.System.RECENTS_CLEAR_ALL_LOCATION, 3, UserHandle.USER_CURRENT);
        mRecentsClearAllLocation.setValue(String.valueOf(location));
        mRecentsClearAllLocation.setOnPreferenceChangeListener(this);                
        updateRecentsLocation(location);
    }

    private void updateAnimationScaleOptions() {
        updateAnimationScaleValue(0, mWindowAnimationScale);
        updateAnimationScaleValue(1, mTransitionAnimationScale);
        updateAnimationScaleValue(2, mAnimatorDurationScale);
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
            summary = R.string.recents_clear_all_location_top_center;
        } else if (value == 2) {
            Settings.System.putInt(resolver, Settings.System.RECENTS_CLEAR_ALL_LOCATION, 2);
            summary = R.string.recents_clear_all_location_top_left;
        } else if (value == 3) {
            Settings.System.putInt(resolver, Settings.System.RECENTS_CLEAR_ALL_LOCATION, 3);
            summary = R.string.recents_clear_all_location_bottom_right;
        } else if (value == 4) {
            Settings.System.putInt(resolver, Settings.System.RECENTS_CLEAR_ALL_LOCATION, 4);
            summary = R.string.recents_clear_all_location_bottom_center;
        } else if (value == 5) {
            Settings.System.putInt(resolver, Settings.System.RECENTS_CLEAR_ALL_LOCATION, 5);
            summary = R.string.recents_clear_all_location_bottom_left;
        }
        if (mRecentsClearAllLocation != null && summary != -1) {
            mRecentsClearAllLocation.setSummary(res.getString(summary));
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

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mWindowAnimationScale ||
                preference == mTransitionAnimationScale ||
                preference == mAnimatorDurationScale) {
            ((AnimationScalePreference) preference).click();
        }
        return false;
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
		if (preference == mWindowAnimationScale) {
            writeAnimationScaleOption(0, mWindowAnimationScale, objValue);
            return true;
        } else if (preference == mTransitionAnimationScale) {
            writeAnimationScaleOption(1, mTransitionAnimationScale, objValue);
            return true;
        } else if (preference == mAnimatorDurationScale) {
            writeAnimationScaleOption(2, mAnimatorDurationScale, objValue);
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
		} 
        return false;
    }
}	  
