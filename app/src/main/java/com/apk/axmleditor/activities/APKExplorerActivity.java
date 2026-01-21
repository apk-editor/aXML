package com.apk.axmleditor.activities;

import static android.view.View.VISIBLE;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.android.apksig.apk.ApkFormatException;
import com.apk.axml.APKParser;
import com.apk.axmleditor.R;
import com.apk.axmleditor.Utils.APKSigner;
import com.apk.axmleditor.Utils.Async;
import com.apk.axmleditor.Utils.FilesViewModel;
import com.apk.axmleditor.Utils.Utils;
import com.apk.axmleditor.adapters.PagerAdapter;
import com.apk.axmleditor.dialogs.ProgressDialog;
import com.apk.axmleditor.fragments.APKInfoFragment;
import com.apk.axmleditor.fragments.FilesFragment;
import com.apk.axmleditor.serializable.FilesEntry;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textview.MaterialTextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/*
 * Created by APK Explorer & Editor <apkeditor@protonmail.com> on January 05, 2025
 */
public class APKExplorerActivity extends AppCompatActivity {

    private AppCompatImageButton mBuild, mIcon;
    private MaterialTextView mDescription, mTitle;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private String mPackageName = null;

    public static final String NAME_INTENT = "name", PACKAGE_NAME_INTENT = "package_name";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apkexplorer);

        mIcon = findViewById(R.id.icon);
        mBuild = findViewById(R.id.build);
        mTitle = findViewById(R.id.title);
        mDescription = findViewById(R.id.description);
        tabLayout = findViewById(R.id.tab_Layout);
        viewPager = findViewById(R.id.view_pager);

        String name = getIntent().getStringExtra(NAME_INTENT);
        mPackageName = getIntent().getStringExtra(PACKAGE_NAME_INTENT);
        Uri mUri = getIntent().getData();

        File imageFile = new File(getExternalCacheDir(), "image.png");
        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
        if (imageFile.exists() && bitmap != null) {
            mIcon.setImageBitmap(bitmap);
        }

        if (name != null) {
            mTitle.setText(name);
        }

        mBuild.setAlpha(
                isModified(new File(Utils.getExportPath(this), mPackageName)) ? 1f : 0.5f
        );

        mBuild.setOnClickListener(v -> {
            if (!isModified(new File(Utils.getExportPath(v.getContext()), mPackageName))) {
                Utils.toast(R.string.modifications_none_toast, v.getContext()).show();
                return;
            }
            new Async() {
                private final Activity activity = APKExplorerActivity.this;
                private final File unSignedAPK = new File(getExternalCacheDir(), "unSigned.apk");
                private ProgressDialog progressDialog;

                @Override
                public void onPreExecute() {
                    progressDialog = new ProgressDialog(activity);
                    progressDialog.setProgressStatus(R.string.applying_modifications_message);
                    progressDialog.startDialog();
                }

                private byte[] fileToByteArray(File file) throws IOException {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    FileInputStream fis = new FileInputStream(file);

                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = fis.read(buffer)) != -1) {
                        bos.write(buffer, 0, read);
                    }

                    fis.close();
                    return bos.toByteArray();
                }

                private void generateUnSignedAPK(Uri uri) throws IOException {
                    ZipInputStream zis = new ZipInputStream(getContentResolver().openInputStream(uri));
                    FileOutputStream fos = new FileOutputStream(unSignedAPK);
                    ZipOutputStream zos = new ZipOutputStream(fos);

                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        String name = entry.getName();
                        ZipEntry newEntry = new ZipEntry(name);

                        File projectPath = new File(Utils.getExportPath(activity), mPackageName);
                        File replacementFile = new File(projectPath, name);

                        byte[] data;
                        if (replacementFile.exists()) {
                            data = fileToByteArray(replacementFile);
                        } else {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            byte[] buffer = new byte[4096];
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                baos.write(buffer, 0, len);
                            }
                            data = baos.toByteArray();
                        }

                        if (name.equals("resources.arsc") || name.startsWith("lib/")
                                || name.startsWith("assets/") || name.startsWith("res/")
                                || name.startsWith("r/") || name.startsWith("R/")) {

                            newEntry.setMethod(ZipEntry.STORED);
                            newEntry.setSize(data.length);
                            newEntry.setCompressedSize(data.length);

                            CRC32 crc = new CRC32();
                            crc.update(data);
                            newEntry.setCrc(crc.getValue());
                        } else {
                            newEntry.setMethod(ZipEntry.DEFLATED);
                        }

                        zos.putNextEntry(newEntry);
                        zos.write(data);
                        zos.closeEntry();
                        zis.closeEntry();
                    }

                    zis.close();
                    zos.close();
                }


                @Override
                public void doInBackground() {
                    if (mUri != null) {
                        try {
                            generateUnSignedAPK(mUri);
                        } catch (IOException ignored) {
                        }
                    }

                    if (unSignedAPK.exists()) {
                        try {
                            new APKSigner().sign(unSignedAPK, new File(Utils.getExportPath(activity),
                                    mPackageName + "-aXMLEditor.apk"), activity);
                        } catch (ApkFormatException | InvalidKeyException | IOException |
                                 NoSuchAlgorithmException | SignatureException ignored) {
                        }

                        unSignedAPK.delete();
                    }
                }

                @Override
                public void onPostExecute() {
                    progressDialog.dismissDialog();
                    new MaterialAlertDialogBuilder(activity)
                            .setIcon(R.mipmap.ic_launcher)
                            .setTitle(R.string.app_name)
                            .setMessage(R.string.modified_apk_generated_message)
                            .setCancelable(false)
                            .setNegativeButton(R.string.cancel, (dialogInterface, i) -> finish()).show();
                }
            }.execute();
        });

        loadUI(mUri).execute();

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    private Async loadUI(Uri uri) {
        return new Async() {
            private final Activity activity = APKExplorerActivity.this;
            private final ArrayList<FilesEntry> imageFiles = new ArrayList<>(), miscFiles = new ArrayList<>(), textFiles = new ArrayList<>(), xmlFiles = new ArrayList<>();
            private ProgressDialog progressDialog;

            @Override
            public void onPreExecute() {
                progressDialog = new ProgressDialog(activity);
                progressDialog.setProgressStatus(R.string.loading);
                progressDialog.startDialog();
            }

            private byte[] inputStreamToBitmapBytes(InputStream inputStream) {
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
                return bos.toByteArray();
            }

            public void getCategories(ZipInputStream zis) throws IOException {
                ZipEntry entry;

                while ((entry = zis.getNextEntry()) != null) {

                    // Skip directories
                    if (entry.isDirectory()) {
                        continue;
                    }

                    String name = entry.getName().toLowerCase();

                    if (name.equalsIgnoreCase("AndroidManifest.xml") || name.contains("res/") && name.endsWith(".xml")) {
                        xmlFiles.add(new FilesEntry(null, entry.getName()));
                    } else if (Utils.isImageFile(name)) {
                        imageFiles.add(new FilesEntry(inputStreamToBitmapBytes(zis), entry.getName()));
                    } else if (Utils.isTextFile(name)) {
                        textFiles.add(new FilesEntry(null, entry.getName()));
                    } else {
                        miscFiles.add(new FilesEntry(null, entry.getName()));
                    }

                    zis.closeEntry();
                }
            }

            @Override
            public void doInBackground() {
                if (uri != null) {
                    try (ZipInputStream zis = new ZipInputStream(getContentResolver().openInputStream(uri))) {
                        getCategories(zis);
                    } catch (IOException ignored) {
                    }
                }
            }

            @Override
            public void onPostExecute() {
                progressDialog.dismissDialog();
                PagerAdapter adapter = new PagerAdapter(APKExplorerActivity.this);

                APKParser mAPKParser = new APKParser();
                if (mAPKParser.isParsed()) {
                    mDescription.setText(mPackageName);

                    adapter.addFragment(new APKInfoFragment(), getString(R.string.page_title_apk_info));
                    adapter.addFragment(getFilesFragment(xmlFiles, "xml", mPackageName, uri), getString(R.string.page_title_axml));
                    adapter.addFragment(getFilesFragment(textFiles, "text", mPackageName, uri), getString(R.string.page_title_texts));
                    adapter.addFragment(getFilesFragment(imageFiles, "image", mPackageName, uri), getString(R.string.page_title_images));
                    adapter.addFragment(getFilesFragment(miscFiles, "misc", mPackageName, uri), getString(R.string.page_title_misc));

                    viewPager.setAdapter(adapter);

                    mIcon.setVisibility(VISIBLE);
                    mTitle.setVisibility(VISIBLE);
                    mDescription.setVisibility(VISIBLE);
                    tabLayout.setVisibility(VISIBLE);
                    viewPager.setVisibility(VISIBLE);
                    mBuild.setVisibility(VISIBLE);

                    new TabLayoutMediator(tabLayout, viewPager,
                            (tab, position) -> tab.setText(adapter.getPageTitle(position))
                    ).attach();
                }
            }
        };
    }

    private boolean isModified(File parent) {
        File[] files = parent.listFiles(File::isFile);
        boolean hasFile = files != null && files.length > 0;
        return parent.exists() && hasFile;
    }

    private FilesFragment getFilesFragment(ArrayList<FilesEntry> filesEntry, String key, String packageName, Uri uri) {
        FilesViewModel vm = new ViewModelProvider(this).get(FilesViewModel.class);
        vm.setFiles(key, filesEntry);

        return FilesFragment.newInstance(key, packageName, uri);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBuild.setAlpha(
                isModified(new File(Utils.getExportPath(this), mPackageName)) ? 1f : 0.5f
        );
    }

}