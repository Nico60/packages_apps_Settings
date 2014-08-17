/*
 * Copyright (C) 2012 The CyanogenMod project
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

package com.android.settings.spirit.fragments;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.res.Resources;
import android.content.Context;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import com.android.settings.chameleonos.labs.identicons.IdenticonsSettings;
import com.android.settings.cyanogenmod.ProgressBar;
import com.android.settings.cyanogenmod.fragments.ScreenStateToggles;
import com.android.settings.cyanogenmod.fragments.WakeLockBlocker;

public class MiscSettings extends SettingsPreferenceFragment {

    private static final String TAG = "MiscUI";

    PagerTabStrip mPagerTabStrip;
    ViewPager mViewPager;

    String titleString[];

    ViewGroup mContainer;

    static Bundle mSavedState;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mContainer = container;

        View view = inflater.inflate(R.layout.settings_tab, container, false);
        mViewPager = (ViewPager) view.findViewById(R.id.viewPager);
        mPagerTabStrip = (PagerTabStrip) view.findViewById(R.id.pagerTabStrip);
        mPagerTabStrip.setTabIndicatorColorResource(android.R.color.white);

        StatusBarAdapter StatusBarAdapter = new StatusBarAdapter(getFragmentManager());
        mViewPager.setAdapter(StatusBarAdapter);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // After confirming PreferenceScreen is available, we call super.
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!Utils.isTablet(getActivity())) {
            mContainer.setPadding(0, 0, 0, 0);
        }
    }

    class StatusBarAdapter extends FragmentPagerAdapter {
        String titles[] = getTitles();
        private Fragment frags[] = new Fragment[titles.length];

        public StatusBarAdapter(FragmentManager fm) {
            super(fm);
            frags[0] = new IdenticonsSettings();
            frags[1] = new ProgressBar();
            frags[2] = new ScreenStateToggles();
            frags[3] = new WakeLockBlocker();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return titles[position];
        }

        @Override
        public Fragment getItem(int position) {
            return frags[position];
        }

        @Override
        public int getCount() {
            return frags.length;
        }
    }

    private String[] getTitles() {
        String titleString[];
        titleString = new String[] {
                    getString(R.string.identicons_title),
                    getString(R.string.progressbar_title),
                    getString(R.string.screen_state_toggles_title),
                    getString(R.string.wakelock_blocker)};
        return titleString;
    }
}
