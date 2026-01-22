package com.apk.axmleditor;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

/*
 * Created by APK Explorer & Editor <apkeditor@protonmail.com> on January 22, 2026
 */
public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
    }

    protected void setContentViewWithInsets(@LayoutRes int layoutResId, int rootViewId) {
        setContentView(layoutResId);

        View root = findViewById(rootViewId);

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            view.setPadding(0, topInset, 0, 0);
            return insets;
        });
    }

}