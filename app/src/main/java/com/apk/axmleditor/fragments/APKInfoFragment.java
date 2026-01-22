package com.apk.axmleditor.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.apk.axml.APKParser;
import com.apk.axmleditor.R;
import com.apk.axmleditor.Utils.Async;
import com.apk.axmleditor.adapters.APKInfoAdapter;
import com.apk.axmleditor.serializable.APKInfoEntry;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/*
 * Created by APK Explorer & Editor <apkeditor@protonmail.com> on January 05, 2026
 */
public class APKInfoFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View mRootView = inflater.inflate(R.layout.layout_recyclerview, container, false);

        RecyclerView recyclerView = mRootView.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));

        new Async() {
            private APKInfoAdapter adapter;
            @Override
            public void onPreExecute() {
            }

            private String getAPKSize(long size) {
                final String[] units = {
                        "B", "KB", "MB", "GB", "TB"
                };
                int unitIndex = 0;
                double readableSize = size;
                while (readableSize >= 1024 && unitIndex < units.length - 1) {
                    readableSize /= 1024;
                    unitIndex++;
                }
                return String.format(Locale.getDefault(), "%.2f %s", readableSize, units[unitIndex]);
            }

            public String sdkToAndroidVersion(int sdkVersion) {
                switch (sdkVersion) {
                    case 36:
                        return "16 (BAKLAVA, " + sdkVersion + ")";
                    case 35:
                        return "15 (VANILLA_ICE_CREAM, " + sdkVersion + ")";
                    case 34:
                        return "14 (UPSIDE_DOWN_CAKE, " + sdkVersion + ")";
                    case 33:
                        return "13 (TIRAMISU, " + sdkVersion + ")";
                    case 32:
                        return "12.1 (S_V2, " + sdkVersion + ")";
                    case 31:
                        return "12 (S, " + sdkVersion + ")";
                    case 30:
                        return "11 (R, " + sdkVersion + ")";
                    case 29:
                        return "10 (Q, " + sdkVersion + ")";
                    case 28:
                        return "9 (P, " + sdkVersion + ")";
                    case 27:
                        return "8 (O_MR1, " + sdkVersion + ")";
                    case 26:
                        return "8.0 (0, " + sdkVersion + ")";
                    case 25:
                        return "7.1.1 (N_MRI, " + sdkVersion + ")";
                    case 24:
                        return "7.0 (N, " + sdkVersion + ")";
                    case 23:
                        return "6.0 (M, " + sdkVersion + ")";
                    case 22:
                        return "5.1 (LOLLIPOP_MR1, " + sdkVersion + ")";
                    case 21:
                        return "5.0 (LOLLIPOP, " + sdkVersion + ")";
                    case 20:
                        return "4.4 (KITKAT_WATCH, " + sdkVersion + ")";
                    case 19:
                        return "4.4 (KITKAT, " + sdkVersion + ")";
                    case 18:
                        return "4.3 (JELLY_BEAN_MR2, " + sdkVersion + ")";
                    case 17:
                        return "4.2 (JELLY_BEAN_MR1, " + sdkVersion + ")";
                    case 16:
                        return "4.1 (JELLY_BEAN, " + sdkVersion + ")";
                    case 15:
                        return "4.0.3 (ICE_CREAM_SANDWICH_MR1, " + sdkVersion + ")";
                    case 14:
                        return "4.0 (ICE_CREAM_SANDWICH, " + sdkVersion + ")";
                    case 13:
                        return "3.2 (HONEYCOMB_MR2, " + sdkVersion + ")";
                    case 12:
                        return "3.1 (HONEYCOMB_MR1, " + sdkVersion + ")";
                    case 11:
                        return "3.0 (HONEYCOMB, " + sdkVersion + ")";
                    case 10:
                        return "2.3.3 (GINGERBREAD_MR1, " + sdkVersion + ")";
                    case 9:
                        return "2.3 (GINGERBREAD, " + sdkVersion + ")";
                    case 8:
                        return "2.2 (FROYO, " + sdkVersion + ")";
                    case 7:
                        return "2.1 (ECLAIR_MR1, " + sdkVersion + ")";
                    case 6:
                        return "2.0.1 (ECLAIR_0_1, " + sdkVersion + ")";
                    case 5:
                        return "2.0 (ECLAIR, " + sdkVersion + ")";
                    case 4:
                        return "1.6 (DONUT, " + sdkVersion + ")";
                    case 3:
                        return "1.5 (CUPCAKE, " + sdkVersion + ")";
                    case 2:
                        return "1.1 (BASE_1_1, " + sdkVersion + ")";
                    case 1:
                        return "1.0 (BASE, " + sdkVersion + ")";
                    default:
                        return String.valueOf(sdkVersion);
                }
            }

            @Override
            public void doInBackground() {
                List<APKInfoEntry> apkInfoEntries = new ArrayList<>();
                APKParser mAPKParser = new APKParser();
                if (mAPKParser.isParsed()) {
                    if (mAPKParser.getVersionCode() != null && mAPKParser.getVersionName() != null) {
                        apkInfoEntries.add(new APKInfoEntry(null, "Version", mAPKParser.getVersionName() + " (" + mAPKParser.getVersionCode() + ")", false));
                    }
                    if (mAPKParser.getCompiledSDKVersion() != null) {
                        apkInfoEntries.add(new APKInfoEntry(null, "Compiled SDK", sdkToAndroidVersion(Integer.parseInt(mAPKParser.getCompiledSDKVersion())), false));
                    }
                    if (mAPKParser.getMinSDKVersion() != null) {
                        apkInfoEntries.add(new APKInfoEntry(null, "Min. SDK", sdkToAndroidVersion(Integer.parseInt(mAPKParser.getMinSDKVersion())), false));
                    }
                    if (mAPKParser.getTargetSDKVersion() != null) {
                        apkInfoEntries.add(new APKInfoEntry(null, "Target SDK", sdkToAndroidVersion(Integer.parseInt(mAPKParser.getTargetSDKVersion())), false));
                    }
                    if (mAPKParser.getAPKSize() != Integer.MIN_VALUE) {
                        apkInfoEntries.add(new APKInfoEntry(null, "Size", getAPKSize(mAPKParser.getAPKSize()), false));
                    }
                    if (mAPKParser.getPermissions() != null && !mAPKParser.getPermissions().isEmpty()) {
                        apkInfoEntries.add(new APKInfoEntry(null, "Permissions", mAPKParser.getPermissions().size() + " found. Click here to expand", true));
                    }
                    if (mAPKParser.getActivities() != null && !mAPKParser.getActivities().isEmpty()) {
                        apkInfoEntries.add(new APKInfoEntry(null, "Activities", mAPKParser.getActivities().size() + " found. Click here to expand", true));
                    }
                    if (mAPKParser.getReceivers() != null && !mAPKParser.getReceivers().isEmpty()) {
                        apkInfoEntries.add(new APKInfoEntry(null, "Receivers", mAPKParser.getReceivers().size() + " found. Click here to expand", true));
                    }
                    if (mAPKParser.getProviders() != null && !mAPKParser.getProviders().isEmpty()) {
                        apkInfoEntries.add(new APKInfoEntry(null, "Providers", mAPKParser.getProviders().size() + " found. Click here to expand", true));
                    }
                    if (mAPKParser.getServices() != null && !mAPKParser.getServices().isEmpty()) {
                        apkInfoEntries.add(new APKInfoEntry(null, "Services", mAPKParser.getServices().size() + " found. Click here to expand", true));
                    }
                    if (mAPKParser.getCertificate() != null) {
                        apkInfoEntries.add(new APKInfoEntry(null, "Certificate", mAPKParser.getCertificate(), false));
                    }
                }

                adapter = new APKInfoAdapter(apkInfoEntries);
            }

            @Override
            public void onPostExecute() {
                if (!isAdded()) return;
                recyclerView.setAdapter(adapter);
            }
        }.execute();

        requireActivity().getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!isAdded()) return;
                new MaterialAlertDialogBuilder(requireActivity())
                        .setIcon(R.mipmap.ic_launcher)
                        .setTitle(R.string.exist_project_title)
                        .setMessage(getString(R.string.exist_project_message, new APKParser().getPackageName()))
                        .setCancelable(false)
                        .setNeutralButton(R.string.cancel, (dialogInterface, i) -> {
                        })
                        .setPositiveButton(R.string.exit, (dialogInterface, i) -> requireActivity().finish())
                        .show();
            }
        });

        return mRootView;
    }
    
}