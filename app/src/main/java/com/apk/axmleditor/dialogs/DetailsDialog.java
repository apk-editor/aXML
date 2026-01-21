package com.apk.axmleditor.dialogs;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.view.View;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.apk.axml.APKParser;
import com.apk.axmleditor.R;
import com.apk.axmleditor.Utils.Async;
import com.apk.axmleditor.adapters.DetailsAdapter;
import com.apk.axmleditor.serializable.APKInfoEntry;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

/*
 * Created by APK Explorer & Editor <apkeditor@protonmail.com> on January 05, 2025
 */
public class DetailsDialog extends BottomSheetDialog {

    public DetailsDialog(String title, Context context) {
        super(context);

        View root = View.inflate(context, R.layout.layout_details, null);

        MaterialTextView textView = root.findViewById(R.id.title);
        RecyclerView recyclerView = root.findViewById(R.id.recycler_view);

        textView.setText(title);
        recyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        new Async() {
            private DetailsAdapter adapter;
            @Override
            public void onPreExecute() {
            }

            private String getTitle(String text) {
                int index = text.lastIndexOf('.');
                return (index == -1) ? text : text.substring(index + 1);
            }

            private List<APKInfoEntry> getActivities() {
                List<APKInfoEntry> data = new ArrayList<>();
                APKParser apkParser = new APKParser();
                for (ActivityInfo activities : apkParser.getActivities()) {
                    data.add(new APKInfoEntry(activities.loadIcon(context.getPackageManager()), activities.labelRes == 0 ? getTitle(activities.name) : activities.loadLabel(context.getPackageManager()).toString(), activities.name, false));
                }
                return data;
            }

            private List<APKInfoEntry> getPermissions() {
                List<APKInfoEntry> data = new ArrayList<>();
                APKParser apkParser = new APKParser();
                for (String permissions : apkParser.getPermissions()) {
                    data.add(new APKInfoEntry(null, getTitle(permissions), permissions, false));
                }
                return data;
            }

            private List<APKInfoEntry> getProviders() {
                List<APKInfoEntry> data = new ArrayList<>();
                APKParser apkParser = new APKParser();
                for (ProviderInfo providers : apkParser.getProviders()) {
                    data.add(new APKInfoEntry(providers.loadIcon(context.getPackageManager()), providers.labelRes == 0 ? getTitle(providers.name) : providers.loadLabel(context.getPackageManager()).toString(), providers.name, false));
                }
                return data;
            }

            private List<APKInfoEntry> getReceivers() {
                List<APKInfoEntry> data = new ArrayList<>();
                APKParser apkParser = new APKParser();
                for (ActivityInfo receivers : apkParser.getReceivers()) {
                    data.add(new APKInfoEntry(receivers.loadIcon(context.getPackageManager()), receivers.labelRes == 0 ? getTitle(receivers.name) : receivers.loadLabel(context.getPackageManager()).toString(), receivers.name, false));
                }
                return data;
            }

            private List<APKInfoEntry> getServices() {
                List<APKInfoEntry> data = new ArrayList<>();
                APKParser apkParser = new APKParser();
                for (ServiceInfo services : apkParser.getServices()) {
                    data.add(new APKInfoEntry(services.loadIcon(context.getPackageManager()), services.labelRes == 0 ? getTitle(services.name) : services.loadLabel(context.getPackageManager()).toString(), services.name, false));
                }
                return data;
            }

            @Override
            public void doInBackground() {
                if (title.equalsIgnoreCase("activities")) {
                    adapter = new DetailsAdapter(getActivities());
                } else if (title.equalsIgnoreCase("providers")) {
                    adapter = new DetailsAdapter(getProviders());
                } else if (title.equalsIgnoreCase("receivers")) {
                    adapter = new DetailsAdapter(getReceivers());
                } else if (title.equalsIgnoreCase("services")) {
                    adapter = new DetailsAdapter(getServices());
                } else if (title.equalsIgnoreCase("permissions")) {
                    adapter = new DetailsAdapter(getPermissions());
                }
            }

            @Override
            public void onPostExecute() {
                recyclerView.setAdapter(adapter);
            }
        }.execute();

        setContentView(root);
        show();
    }

}