/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.quicksettings;

import static com.android.internal.util.cm.QSConstants.TILE_CUSTOM;
import static com.android.internal.util.cm.QSConstants.TILE_CUSTOM_KEY;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.util.cm.LockscreenTargetUtils;
import com.android.internal.util.cm.QSConstants;
import com.android.internal.util.slim.AppHelper;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.quicksettings.QuickSettingsUtil.TileInfo;
import com.android.settings.slim.util.IconPicker;
import com.android.settings.slim.util.IconPicker.OnIconPickListener;
import com.android.settings.slim.util.ShortcutPickerHelper;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class QuickSettingsTiles extends Fragment implements View.OnClickListener,
        View.OnLongClickListener, ShortcutPickerHelper.OnPickListener, OnIconPickListener {

    private static final String TAG = "QuickSettingsTiles";

    private static final int MENU_RESET = Menu.FIRST;

    private static final int DLG_RESET              = 0;
    private static final int DLG_CUSTOM_TILE        = 1;
    private static final int DLG_CUSTOM_TILE_EXTRAS = 2;
    private static final int DLG_SHOW_LIST          = 3;

    private static final int NUMBER_ACTIONS         = 5;

    private DraggableGridView mDragView;
    private ViewGroup mContainer;
    private LayoutInflater mInflater;
    private Resources mSystemUiResources;
    private TileAdapter mTileAdapter;
    private static boolean mConfigRibbon;
    private ShortcutPickerHelper mPicker;
    private IconPicker mIconPicker;
    private File mTemporaryImage;

    private ImageButton[] mDialogIcon = new ImageButton[NUMBER_ACTIONS];
    private Button[] mDialogLabel = new Button[NUMBER_ACTIONS];

    private Drawable mEmptyIcon;
    private String mEmptyLabel;

    private String mCurrentCustomTile = null;

    private boolean mShortPress = true;

    private int mCurrentAction = 0;

    private int mTileTextSize;
    private int mTileTextPadding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mDragView = new DraggableGridView(getActivity());
        mContainer = container;
        mContainer.setClipChildren(false);
        mContainer.setClipToPadding(false);
        mInflater = inflater;

        mIconPicker = new IconPicker(getActivity(), this);
        mPicker = new ShortcutPickerHelper(getActivity(), this);
        mEmptyLabel = getResources().getString(R.string.lockscreen_target_empty);
        mEmptyIcon = getResources().getDrawable(R.drawable.ic_empty);
        mTemporaryImage = new File(getActivity().getCacheDir() + "/custom_tile.tmp");

        QuickSettingsUtil.removeUnsupportedTiles(getActivity());

        // We have both a panel and the ribbon config, see which one we are using
        Bundle args = getArguments();
        if (args != null) {
            mConfigRibbon = args.getBoolean("config_ribbon");
        }

        PackageManager pm = getActivity().getPackageManager();
        if (pm != null) {
            try {
                mSystemUiResources = pm.getResourcesForApplication("com.android.systemui");
            } catch (Exception e) {
                mSystemUiResources = null;
            }
        }
        int panelWidth = getItemFromSystemUi("notification_panel_width", "dimen");
        if (panelWidth > 0) {
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(panelWidth,
                    FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER_HORIZONTAL);
            mDragView.setLayoutParams(params);
        }
        int cellGap = getItemFromSystemUi("quick_settings_cell_gap", "dimen");
        if (cellGap != 0) {
            mDragView.setCellGap(cellGap);
        }
        int columnCount = Settings.System.getIntForUser(getActivity().getContentResolver(),
                Settings.System.QUICK_TILES_PER_ROW, 3,
                UserHandle.USER_CURRENT);
        // do not allow duplication on tablets or any device which do not have
        // flipsettings
        boolean duplicateOnLandScape = Settings.System.getIntForUser(
                getActivity().getContentResolver(),
                Settings.System.QUICK_TILES_PER_ROW_DUPLICATE_LANDSCAPE,
                1, UserHandle.USER_CURRENT) == 1
                        && mSystemUiResources.getBoolean(mSystemUiResources.getIdentifier(
                        "com.android.systemui:bool/config_hasFlipSettingsPanel", null, null))
                        && isLandscape();

        if (columnCount != 0) {
            mDragView.setColumnCount(duplicateOnLandScape ? (columnCount * 2) : columnCount);
            mTileTextSize = mDragView.getTileTextSize(columnCount);
            mTileTextPadding = mDragView.getTileTextPadding(columnCount);
        }
        mTileAdapter = new TileAdapter(getActivity(), mConfigRibbon);
        return mDragView;
    }

    private int getItemFromSystemUi(String name, String type) {
        if (mSystemUiResources != null) {
            int resId = (int) mSystemUiResources.getIdentifier(name, type, "com.android.systemui");
            if (resId > 0) {
                try {
                    if (type.equals("dimen")) {
                        return (int) mSystemUiResources.getDimension(resId);
                    } else {
                        return mSystemUiResources.getInteger(resId);
                    }
                } catch (NotFoundException e) {
                }
            }
        }
        return 0;
    }

    void genTiles() {
        mDragView.removeAllViews();
        ArrayList<String> tiles = QuickSettingsUtil.getTileListFromString(
                QuickSettingsUtil.getCurrentTiles(getActivity(), mConfigRibbon));
        for (String tileindex : tiles) {
            QuickSettingsUtil.TileInfo tile = null;
            if (tileindex.contains(TILE_CUSTOM)) {
                tile = QuickSettingsUtil.TILES.get(TILE_CUSTOM);
            } else {
                tile = QuickSettingsUtil.TILES.get(tileindex);
            }
            if (tile != null) {
                addTile(tile.getTitleResId(), tile.getIcon(), 0, false);
            }
        }
        addTile(R.string.profiles_add, null, R.drawable.ic_menu_add, false);
    }

    /**
     * Adds a tile to the dragview
     * @param titleId - string id for tile text in systemui
     * @param iconSysId - resource id for icon in systemui
     * @param iconRegId - resource id for icon in local package
     * @param newTile - whether a new tile is being added by user
     */
    void addTile(int titleId, String iconSysId, int iconRegId, boolean newTile) {
        View tileView = null;
        if (iconRegId != 0) {
            tileView = (View) mInflater.inflate(R.layout.quick_settings_tile_generic, null, false);
            final TextView name = (TextView) tileView.findViewById(R.id.text);
            final ImageView iv = (ImageView) tileView.findViewById(R.id.image);
            name.setText(titleId);
            name.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTileTextSize);
            name.setPadding(0, mTileTextPadding, 0, 0);
            iv.setImageDrawable(getResources().getDrawable(iconRegId));
        } else {
            final boolean isUserTile =
                    titleId == QuickSettingsUtil.TILES.get(QSConstants.TILE_USER).getTitleResId();
            if (mSystemUiResources != null && iconSysId != null) {
                int resId = mSystemUiResources.getIdentifier(iconSysId, null, null);
                if (resId > 0) {
                    try {
                        Drawable d = mSystemUiResources.getDrawable(resId);
                        tileView = null;
                        if (isUserTile) {
                            tileView = (View) mInflater.inflate(
                                    R.layout.quick_settings_tile_user, null, false);
                            ImageView iv = (ImageView) tileView.findViewById(R.id.user_imageview);
                            TextView tv = (TextView) tileView.findViewById(R.id.tile_textview);
                            tv.setText(titleId);
                            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTileTextSize);
                            iv.setImageDrawable(d);
                        } else {
                            tileView = (View) mInflater.inflate(
                                    R.layout.quick_settings_tile_generic, null, false);
                            final TextView name = (TextView) tileView.findViewById(R.id.text);
                            final ImageView iv = (ImageView) tileView.findViewById(R.id.image);
                            name.setText(titleId);
                            name.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTileTextSize);
                            name.setPadding(0, mTileTextPadding, 0, 0);
                            iv.setImageDrawable(d);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if (tileView != null) {
            if (titleId == QuickSettingsUtil.TILES.get(
                            QSConstants.TILE_CUSTOM).getTitleResId()) {

                ImageView settings =  (ImageView) tileView.findViewById(R.id.settings);
                if (settings != null) {
                    settings.setVisibility(View.VISIBLE);
                }
            }
            mDragView.addView(tileView, newTile
                    ? mDragView.getChildCount() - 1 : mDragView.getChildCount());
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        genTiles();
        mDragView.setOnRearrangeListener(new DraggableGridView.OnRearrangeListener() {
            public void onRearrange(int oldIndex, int newIndex) {
                ArrayList<String> tiles = QuickSettingsUtil.getTileListFromString(
                        QuickSettingsUtil.getCurrentTiles(getActivity(), mConfigRibbon));
                String oldTile = tiles.get(oldIndex);
                tiles.remove(oldIndex);
                tiles.add(newIndex, oldTile);
                QuickSettingsUtil.saveCurrentTiles(getActivity(),
                        QuickSettingsUtil.getTileStringFromList(tiles), mConfigRibbon);
            }
            @Override
            public void onDelete(int index) {
                ArrayList<String> tiles = QuickSettingsUtil.getTileListFromString(
                        QuickSettingsUtil.getCurrentTiles(getActivity(), mConfigRibbon));
                if (tiles.get(index).contains(TILE_CUSTOM)) {
                    QuickSettingsUtil.deleteCustomTile(
                            getActivity(), findCustomKey(tiles.get(index)));
                }
                tiles.remove(index);
                QuickSettingsUtil.saveCurrentTiles(getActivity(),
                        QuickSettingsUtil.getTileStringFromList(tiles), mConfigRibbon);
            }
        });
        mDragView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                ArrayList<String> tiles = QuickSettingsUtil.getTileListFromString(
                        QuickSettingsUtil.getCurrentTiles(getActivity(), mConfigRibbon));
                if (arg2 != mDragView.getChildCount() - 1) {
                    if (arg2 == -1) {
                        return;
                    }
                    if (tiles.get(arg2).contains(TILE_CUSTOM)) {
                        mCurrentCustomTile = findCustomKey(tiles.get(arg2));
                        showDialogInner(DLG_CUSTOM_TILE);
                    }
                    return;
                }
                showDialogInner(DLG_SHOW_LIST);
            }
        });

        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Utils.isPhone(getActivity())) {
            mContainer.setPadding(20, 0, 20, 0);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        menu.add(0, MENU_RESET, 0, R.string.profile_reset_title)
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
                return false;
        }
    }

    private boolean isLandscape() {
        final boolean isLandscape =
            Resources.getSystem().getConfiguration().orientation
                    == Configuration.ORIENTATION_LANDSCAPE;
        return isLandscape;
    }

    private String findCustomKey(String tile) {
        String[] split = tile.split(TILE_CUSTOM_KEY);
        return split[1];
    }

    private View customTileDialogView(String tileKey) {
        View view = View.inflate(getActivity(), R.layout.custom_tile_dialog, null);

        mCurrentCustomTile = tileKey;

        mDialogIcon[0] = (ImageButton) view.findViewById(R.id.icon);
        mDialogLabel[0] = (Button) view.findViewById(R.id.action);
        mDialogIcon[1] = (ImageButton) view.findViewById(R.id.icon_two);
        mDialogLabel[1] = (Button) view.findViewById(R.id.action_two);
        mDialogIcon[2] = (ImageButton) view.findViewById(R.id.icon_three);
        mDialogLabel[2] = (Button) view.findViewById(R.id.action_three);
        mDialogIcon[3] = (ImageButton) view.findViewById(R.id.icon_four);
        mDialogLabel[3] = (Button) view.findViewById(R.id.action_four);
        mDialogIcon[4] = (ImageButton) view.findViewById(R.id.icon_five);
        mDialogLabel[4] = (Button) view.findViewById(R.id.action_five);
        ImageButton reset = (ImageButton) view.findViewById(R.id.reset);
        ImageButton resetTwo = (ImageButton) view.findViewById(R.id.reset_two);
        ImageButton resetThree = (ImageButton) view.findViewById(R.id.reset_three);
        ImageButton resetFour = (ImageButton) view.findViewById(R.id.reset_four);
        ImageButton resetFive = (ImageButton) view.findViewById(R.id.reset_five);

        setDialogIconsAndText(0);
        setDialogIconsAndText(1);
        setDialogIconsAndText(2);
        setDialogIconsAndText(3);
        setDialogIconsAndText(4);

        for (int i = 0; i < NUMBER_ACTIONS; i++) {
            mDialogIcon[i].setOnClickListener(QuickSettingsTiles.this);
            mDialogLabel[i].setOnClickListener(QuickSettingsTiles.this);
            mDialogLabel[i].setOnLongClickListener(QuickSettingsTiles.this);
        }

        reset.setOnClickListener(QuickSettingsTiles.this);
        resetTwo.setOnClickListener(QuickSettingsTiles.this);
        resetThree.setOnClickListener(QuickSettingsTiles.this);
        resetFour.setOnClickListener(QuickSettingsTiles.this);
        resetFive.setOnClickListener(QuickSettingsTiles.this);

        return view;
    }

    private void setDialogIconsAndText(int index) {
        mDialogLabel[index].setText(returnFriendlyName(index));
        mDialogIcon[index].setImageDrawable(returnPackageDrawable(index));
    }

    private String returnFriendlyName(int index) {
        String uri = QuickSettingsUtil.getActionsAtIndex(getActivity(),
                index, 0, mCurrentCustomTile);
        String longpressUri = QuickSettingsUtil.getActionsAtIndex(getActivity(),
                index, 1, mCurrentCustomTile);

        if (uri == null && longpressUri == null) {
            return mEmptyLabel;
        } else if (uri == null && longpressUri != null) {
            return getResources().getString(R.string.custom_tile_long_press)
                    + " " + AppHelper.getFriendlyNameForUri(
                    getActivity(), getActivity().getPackageManager(), longpressUri);
        } else if (uri != null && longpressUri == null) {
            return AppHelper.getFriendlyNameForUri(
                    getActivity(), getActivity().getPackageManager(), uri);
        } else {
            return AppHelper.getFriendlyNameForUri(
                    getActivity(), getActivity().getPackageManager(), uri)
                    + "\n" + getResources().getString(R.string.custom_tile_long_press)
                    + " "+ AppHelper.getFriendlyNameForUri(
                    getActivity(), getActivity().getPackageManager(), longpressUri);
        }
    }

    private Drawable returnPackageDrawable(int index) {
        String uri = QuickSettingsUtil.getActionsAtIndex(getActivity(),
                index, 0, mCurrentCustomTile);

        if (uri == null ) {
            // Check if long action exists, and use it instead
            uri = QuickSettingsUtil.getActionsAtIndex(getActivity(),
                index, 1, mCurrentCustomTile);
        }

        Drawable icon = null;
        if (uri != null) {
            String iconUri = QuickSettingsUtil.getActionsAtIndex(getActivity(),
                index, 2, mCurrentCustomTile);
            if (iconUri != null && iconUri.length() > 0) {
                File f = new File(Uri.parse(iconUri).getPath());
                if (f.exists()) {
                    icon = new BitmapDrawable(
                            getResources(), f.getAbsolutePath());
                }
            } else {
                try {
                    Intent intent = Intent.parseUri(uri, 0);
                    icon = LockscreenTargetUtils.getDrawableFromIntent(getActivity(), intent);
                } catch (URISyntaxException e) {
                    Log.wtf(TAG, "Invalid uri: " + uri);
                }
            }
        }

        if (icon == null) {
            return mEmptyIcon;
        } else {
            return icon;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == IconPicker.REQUEST_PICK_SYSTEM
                || requestCode == IconPicker.REQUEST_PICK_GALLERY
                || requestCode == IconPicker.REQUEST_PICK_ICON_PACK) {
            mIconPicker.onActivityResult(requestCode, resultCode, data);
        } else if (requestCode != Activity.RESULT_CANCELED
                && resultCode != Activity.RESULT_CANCELED) {
            mPicker.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void iconPicked(int requestCode, int resultCode, Intent intent) {
        Drawable iconDrawable = null;
        if (requestCode == IconPicker.REQUEST_PICK_GALLERY) {
            if (resultCode == Activity.RESULT_OK) {
                if (mTemporaryImage.length() == 0 || !mTemporaryImage.exists()) {
                    Toast.makeText(getActivity(),
                            getResources().getString(R.string.shortcut_image_not_valid),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                File imageFile = new File(getActivity().getFilesDir(),
                        "/custom_tile_" + System.currentTimeMillis() + ".png");
                String path = imageFile.getAbsolutePath();
                mTemporaryImage.renameTo(imageFile);
                imageFile.setReadable(true, false);
                iconDrawable = LockscreenTargetUtils.getDrawableFromFile(
                        getActivity(), path);

                deleteCustomIcon();  // Delete current icon if it exists before saving new.
                QuickSettingsUtil.saveCustomActions(getActivity(), mCurrentAction, 2,
                        path, mCurrentCustomTile);

                setDialogIconsAndText(mCurrentAction);
            } else {
                if (mTemporaryImage.exists()) {
                    mTemporaryImage.delete();
                }
            }
        }
    }

    @Override
    public void shortcutPicked(String uri, String friendlyName, boolean isApplication) {
        if (uri == null) {
            return;
        }

        Drawable icon = null;
        try {
            Intent intent = Intent.parseUri(uri, 0);
            icon = LockscreenTargetUtils.getDrawableFromIntent(getActivity(), intent);
        } catch (URISyntaxException e) {
            Log.wtf(TAG, "Invalid uri: " + uri);
        }

        boolean changeIcon = false;
        int setting = 0;

        if (mShortPress) {
            changeIcon = true;
        } else {
            changeIcon = QuickSettingsUtil.getActionsAtIndex(getActivity(),
                    mCurrentAction, 0, mCurrentCustomTile) == null;
            setting = 1;
        }

        if (changeIcon) {
            deleteCustomIcon();
        }

        QuickSettingsUtil.saveCustomActions(getActivity(),
                mCurrentAction, setting, uri, mCurrentCustomTile);

        setDialogIconsAndText(mCurrentAction);
    }

    private void deleteCustomIcon() {
        String path = QuickSettingsUtil.getActionsAtIndex(getActivity(),
                mCurrentAction, 2, mCurrentCustomTile);

        if (path != null) {
            File f = new File(path);
            if (f != null && f.exists()) {
                f.delete();
            }
        }
        QuickSettingsUtil.saveCustomActions(getActivity(),
                mCurrentAction, 2, " ", mCurrentCustomTile);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.icon:
                prepareCustomIcon(0);
                break;
            case R.id.icon_two:
                prepareCustomIcon(1);
                break;
            case R.id.icon_three:
                prepareCustomIcon(2);
                break;
            case R.id.icon_four:
                prepareCustomIcon(3);
                break;
            case R.id.icon_five:
                prepareCustomIcon(4);
                break;
            case R.id.action:
                prepareCustomAction(0, true);
                break;
            case R.id.action_two:
                prepareCustomAction(1, true);
                break;
            case R.id.action_three:
                prepareCustomAction(2, true);
                break;
            case R.id.action_four:
                prepareCustomAction(3, true);
                break;
            case R.id.action_five:
                prepareCustomAction(4, true);
                break;
            case R.id.reset:
                deleteAction(0);
                break;
            case R.id.reset_two:
                deleteAction(1);
                break;
            case R.id.reset_three:
                deleteAction(2);
                break;
            case R.id.reset_four:
                deleteAction(3);
                break;
            case R.id.reset_five:
                deleteAction(4);
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch(v.getId()) {
            case R.id.action:
                prepareCustomAction(0, false);
                break;
            case R.id.action_two:
                prepareCustomAction(1, false);
                break;
            case R.id.action_three:
                prepareCustomAction(2, false);
                break;
            case R.id.action_four:
                prepareCustomAction(3, false);
                break;
            case R.id.action_five:
                prepareCustomAction(4, false);
                break;
        }
        return true;
    }

    private void prepareCustomIcon(int action) {
        mCurrentAction = action;
        String uri = QuickSettingsUtil.getActionsAtIndex(getActivity(),
                mCurrentAction, 0, mCurrentCustomTile);

        if (uri == null ) {
            // Check if long action exists, and use it instead
            uri = QuickSettingsUtil.getActionsAtIndex(getActivity(),
                    mCurrentAction, 1, mCurrentCustomTile);
        }

        if (uri != null) {
            try {
                mTemporaryImage.createNewFile();
                mTemporaryImage.setWritable(true, false);
                mIconPicker.pickGallery(getId(), mTemporaryImage);
            } catch (IOException e) {
                Log.d(TAG, "Could not create temporary icon", e);
            }
        } else {
            Toast.makeText(getActivity(), R.string.custom_tile_null_warning,
                    Toast.LENGTH_LONG).show();
        }
    }

    private void prepareCustomAction(int action, boolean shortpress) {
        mCurrentAction = action;
        mShortPress = shortpress;
        mPicker.pickShortcut(getId());
    }

    private void deleteAction(int action) {
        mCurrentAction = action;
        QuickSettingsUtil.deleteCustomActions(
                getActivity(), mCurrentAction, mCurrentCustomTile);
        setDialogIconsAndText(mCurrentAction);
    }

    private void showDialogInner(int id) {
        DialogFragment newFragment =
                MyAlertDialogFragment.newInstance(id, mCurrentCustomTile);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(int id, String tileKey) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            args.putString("tileKey", tileKey);
            frag.setArguments(args);
            return frag;
        }

        QuickSettingsTiles getOwner() {
            return (QuickSettingsTiles) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            final String tileKey = getArguments().getString("tileKey");
            switch (id) {
                case DLG_RESET:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.reset)
                    .setMessage(R.string.tiles_reset_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            QuickSettingsUtil.resetTiles(getActivity(), mConfigRibbon);
                            getOwner().genTiles();
                        }
                    })
                    .create();
                case DLG_SHOW_LIST:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.tile_choose_title)
                    .setNegativeButton(R.string.cancel, null)
                    .setAdapter(getOwner().mTileAdapter, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, final int position) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    ArrayList<String> curr =
                                            QuickSettingsUtil.getTileListFromString(
                                                QuickSettingsUtil.getCurrentTiles(getActivity(), mConfigRibbon));
                                    if (getOwner().mTileAdapter.getTileId(
                                            position).contains(TILE_CUSTOM)) {
                                        curr.add(getOwner().mTileAdapter.getTileId(position)
                                                + TILE_CUSTOM_KEY + System.currentTimeMillis());
                                    } else {
                                        curr.add(getOwner().mTileAdapter.getTileId(position));
                                    }
                                    QuickSettingsUtil.saveCurrentTiles(getActivity(),
                                            QuickSettingsUtil.getTileStringFromList(curr), mConfigRibbon);
                                }
                            }).start();
                            TileInfo info = QuickSettingsUtil.TILES.get(
                                    getOwner().mTileAdapter.getTileId(position));
                            getOwner().addTile(info.getTitleResId(), info.getIcon(), 0, true);
                        }
                    })
                    .create();
                case DLG_CUSTOM_TILE:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.custom_tile_dialog_title)
                    .setView(getOwner().customTileDialogView(tileKey))
                    .setCancelable(false)
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            if (QuickSettingsUtil.getCustomExtras(getActivity(),
                                    Settings.System.CUSTOM_TOGGLE_EXTRAS,
                                    tileKey) == null) {
                                QuickSettingsUtil.saveCustomExtras(getActivity(),
                                        Integer.toString(1), tileKey);
                            }
                            getOwner().showDialogInner(DLG_CUSTOM_TILE_EXTRAS);
                        }
                    })
                    .create();
                case DLG_CUSTOM_TILE_EXTRAS:
                    int actions = 0;
                    boolean matchIncluded = false;
                    String checkClick = null;
                    for (int i = 0; i < getOwner().NUMBER_ACTIONS; i++) {
                        checkClick = QuickSettingsUtil.getActionsAtIndex(getActivity(),
                                i, 0, tileKey);
                        if (checkClick != null) {
                            actions++;
                        }
                    }
                    // User selected multiple click actions
                    // Only now is this preference relevant
                    if (actions > 1) {
                        matchIncluded = true;
                    }

                    String setting = QuickSettingsUtil.getCustomExtras(getActivity(),
                            Settings.System.CUSTOM_TOGGLE_EXTRAS,
                            tileKey);
                    final boolean[] checkBox = new boolean[matchIncluded ? 2 : 1];
                    if (setting != null) {
                        switch (Integer.parseInt(setting)) {
                            case 0:
                                checkBox[0] = false;
                                if (matchIncluded) {
                                    checkBox[1] = false;
                                }
                                break;
                            case 1:
                                checkBox[0] = true;
                                if (matchIncluded) {
                                    checkBox[1] = false;
                                }
                                break;
                            case 2:
                                checkBox[0] = false;
                                if (matchIncluded) {
                                    checkBox[1] = true;
                                }
                                break;
                            case 3:
                                checkBox[0] = true;
                                if (matchIncluded) {
                                    checkBox[1] = true;
                                }
                                break;
                        }
                    } else {
                        checkBox[0] = false;
                        if (matchIncluded) {
                            checkBox[1] = false;
                        }
                    }

                    final String[] entry = new String[matchIncluded ? 2 : 1];

                    entry[0] = getResources().getString(
                            R.string.custom_toggle_collapse_check);
                    if (matchIncluded) {
                        entry[1] = getResources().getString(
                                R.string.custom_toggle_match_state_check);
                    }

                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.custom_toggle_extras)
                    .setNegativeButton(R.string.cancel, null)
                    .setMultiChoiceItems(entry, checkBox,
                        new  DialogInterface.OnMultiChoiceClickListener() {
                        public void onClick(DialogInterface dialog, int indexSelected,
                                boolean isChecked) {
                            checkBox[indexSelected] = isChecked;
                        }
                    })
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            int userValue = 0;
                            for (int i = 0; i < checkBox.length; i++) {
                                boolean addIt = checkBox[i];
                                switch (i) {
                                    case 0:
                                        if (addIt) {
                                            userValue += 1;
                                        }
                                        break;
                                    case 1:
                                        if (addIt) {
                                            userValue += 2;
                                        }
                                        break;
                                }
                            }
                            QuickSettingsUtil.saveCustomExtras(getActivity(),
                                    Integer.toString(userValue), tileKey);
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

    private static class TileAdapter extends ArrayAdapter<String> {
        private static class Entry {
            public final TileInfo tile;
            public final String tileTitle;
            public Entry(TileInfo tile, String tileTitle) {
                this.tile = tile;
                this.tileTitle = tileTitle;
            }
        }

        private Entry[] mTiles;
        private boolean mIsRibbon;

        public TileAdapter(Context context, boolean isRibbon) {
            super(context, android.R.layout.simple_list_item_1);
            mTiles = new Entry[getCount()];
            mIsRibbon = isRibbon;
            loadItems(context.getResources());
            sortItems();
        }

        private void loadItems(Resources resources) {
            int index = 0;
            for (TileInfo t : QuickSettingsUtil.TILES.values()) {
                mTiles[index++] = new Entry(t, resources.getString(t.getTitleResId()));
            }
        }

        private void sortItems() {
            final Collator collator = Collator.getInstance();
            collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
            collator.setStrength(Collator.PRIMARY);
            Arrays.sort(mTiles, new Comparator<Entry>() {
                @Override
                public int compare(Entry e1, Entry e2) {
                    return collator.compare(e1.tileTitle, e2.tileTitle);
                }
            });
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            v.setEnabled(isEnabled(position));
            return v;
        }

        @Override
        public int getCount() {
            return QuickSettingsUtil.TILES.size();
        }

        @Override
        public String getItem(int position) {
            return mTiles[position].tileTitle;
        }

        public String getTileId(int position) {
            return mTiles[position].tile.getId();
        }

        @Override
        public boolean isEnabled(int position) {
            String id = mTiles[position].tile.getId();
            String currentTiles = QuickSettingsUtil.getCurrentTiles(getContext(), mIsRibbon);
            ArrayList<String> tiles = QuickSettingsUtil.getTileListFromString(currentTiles);
            if (TILE_CUSTOM.equals(mTiles[position].tile.getId())) {
                return true;
            }
            for (String tile : tiles) {
                if (tile.equals(id)) {
                    return false;
                }
            }
            return true;
        }
    }
}
