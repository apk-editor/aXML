package com.apk.axmleditor.dialogs;

import android.content.Context;
import android.view.View;

import com.apk.axmleditor.BuildConfig;
import com.apk.axmleditor.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textview.MaterialTextView;

/*
 * Created by APK Explorer & Editor <apkeditor@protonmail.com> on January 05, 2026
 */
public class AboutDialog extends MaterialAlertDialogBuilder {

    public AboutDialog(Context context) {
        super(context);

        View root = View.inflate(context, com.apk.axmleditor.R.layout.layout_about, null);
        MaterialTextView mAppTile = root.findViewById(R.id.title);
        String titleText = context.getString(R.string.app_name) + " " + BuildConfig.VERSION_NAME;
        mAppTile.setText(titleText);

        setView(root);
        show();
    }

}