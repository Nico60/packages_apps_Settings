package com.android.settings.optipop.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v13.app.FragmentTabHost;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost.OnTabChangeListener;

import com.android.settings.R;

import com.android.settings.slim.HardwareKeysSettings;
import com.android.settings.slim.NavbarSettings;
import com.android.settings.slim.PieControl;

public class NavigationTabHostFragment extends Fragment implements OnTabChangeListener {

    private FragmentTabHost mTabHost;

    static String sLastTab;

    boolean deviceHardwareKeys;

    public NavigationTabHostFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        deviceHardwareKeys = getActivity().getResources()
                   .getBoolean(com.android.internal.R.integer.config_deviceHardwareKeys);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mTabHost = new FragmentTabHost(getActivity());
        mTabHost.setup(getActivity(), getChildFragmentManager(), R.id.container);

        if (deviceHardwareKeys) {
        mTabHost.addTab(mTabHost.newTabSpec("KeysSettings").setIndicator(getString(R.string.button_keys_title)),
                HardwareKeysSettings.class, null);
        }
        mTabHost.addTab(mTabHost.newTabSpec("NavbarSettings").setIndicator(getString(R.string.navigation_bar)),
                NavbarSettings.class, null);
        mTabHost.addTab(mTabHost.newTabSpec("PieControl").setIndicator(getString(R.string.pie_control_title)),
                PieControl.class, null);

        mTabHost.setOnTabChangedListener(this);
        return mTabHost;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sLastTab != null) {
            mTabHost.setCurrentTabByTag(sLastTab);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mTabHost = null;
    }

    @Override
    public void onTabChanged(String s) {
        sLastTab = s;
    }
}
