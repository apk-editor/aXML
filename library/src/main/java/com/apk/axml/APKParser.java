package com.apk.axml;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.util.Base64;

import androidx.annotation.RequiresApi;

import com.apk.axml.aXMLUtils.Utils;
import com.apk.axml.serializableItems.ResEntry;
import com.apk.axml.serializableItems.XMLEntry;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/*
 * Created by APK Explorer & Editor <apkeditor@protonmail.com> on January 25, 2023
 */
public class APKParser {

    private static boolean mAppNameParsed = false;
    private static Drawable mAppIcon = null;
    private static List<String> mPermissions = null;
    private static long mAPKSize = Integer.MIN_VALUE;
    private static List<ActivityInfo> mActivities = null, mReceivers = null;
    private static List<ProviderInfo> mProviders = null;
    private static List<ResEntry> mResDecoded = null;
    private static List<ServiceInfo> mServices = null;
    private static List<XMLEntry> mManifest = null;
    private static String mApkPath = null, mAppName = null, mCertificate = null, mCompileSDK = null, mManifestAsString = null,
            mMinSDK = null, mPackageName = null, mVersionCode = null, mVersionName = null, mTarSDK = null;

    public APKParser() {
    }

    public boolean isParsed() {
        return mPackageName != null;
    }

    public Drawable getAppIcon() {
        return mAppIcon;
    }

    public File getApkFile() {
        return new File(mApkPath);
    }

    public List<ActivityInfo> getActivities() {
        return mActivities;
    }

    public List<ActivityInfo> getReceivers() {
        return mReceivers;
    }

    public List<ProviderInfo> getProviders() {
        return mProviders;
    }

    public List<ServiceInfo> getServices() {
        return mServices;
    }

    public List<String> getPermissions() {
        return mPermissions;
    }

    public String getApkPath() {
        return mApkPath;
    }

    public String getAppName() {
        return mAppName;
    }

    public long getAPKSize() {
        return mAPKSize;
    }

    public String getCompiledSDKVersion() {
        return mCompileSDK;
    }

    public String getCertificate() {
        return mCertificate;
    }

    public List<ResEntry> getDecodedResources() {
        return mResDecoded;
    }

    public List<XMLEntry> getManifest() {
        return mManifest;
    }

    public String getManifestAsString() {
        return mManifestAsString;
    }

    public String getMinSDKVersion() {
        return mMinSDK;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public String getTargetSDKVersion() {
        return mTarSDK;
    }

    public String getVersionCode() {
        return mVersionCode;
    }

    public String getVersionName() {
        return mVersionName;
    }

    private ZipFile getZipFile(String apkPath) throws IOException {
        return new ZipFile(apkPath);
    }

    private static Drawable getAppIcon(InputStream apkStream, String iconRef) throws IOException {
        if (iconRef.startsWith("res/") && (iconRef.endsWith(".png") || iconRef.endsWith(".webp"))) {
            try (ZipInputStream zis = new ZipInputStream(apkStream)) {

                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    String name = entry.getName();
                    if (name.contains(iconRef)) {
                        return Drawable.createFromStream(copyEntryStream(zis), name);
                    }
                }
            }
        }
        return null;
    }

    private static InputStream copyEntryStream(ZipInputStream zis) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] tmp = new byte[4096];
        int len;
        while ((len = zis.read(tmp)) != -1) {
            buffer.write(tmp, 0, len);
        }
        return new ByteArrayInputStream(buffer.toByteArray());
    }

    private static List<ActivityInfo> getActivities(String apkPath, Context context) {
        PackageInfo packageInfo = getPackageManager(context).getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES);
        if (packageInfo != null && packageInfo.activities != null) {
            return new ArrayList<>(Arrays.asList(packageInfo.activities));
        }
        return null;
    }

    private static List<ActivityInfo> getReceivers(String apkPath, Context context) {
        PackageInfo packageInfo = getPackageManager(context).getPackageArchiveInfo(apkPath, PackageManager.GET_RECEIVERS);
        if (packageInfo != null && packageInfo.receivers != null) {
            return new ArrayList<>(Arrays.asList(packageInfo.receivers));
        }
        return null;
    }

    private static List<ProviderInfo> getProviders(String apkPath, Context context) {
        PackageInfo packageInfo = getPackageManager(context).getPackageArchiveInfo(apkPath, PackageManager.GET_PROVIDERS);
        if (packageInfo != null && packageInfo.providers != null) {
            return new ArrayList<>(Arrays.asList(packageInfo.providers));
        }
        return null;
    }

    private static List<ServiceInfo> getServices(String apkPath, Context context) {
        PackageInfo packageInfo = getPackageManager(context).getPackageArchiveInfo(apkPath, PackageManager.GET_SERVICES);
        if (packageInfo != null && packageInfo.services != null) {
            return new ArrayList<>(Arrays.asList(packageInfo.services));
        }
        return null;
    }

    private static List<String> getPermissions(String apkPath, Context context) {
        PackageInfo packageInfo = getPackageManager(context).getPackageArchiveInfo(apkPath, PackageManager.GET_PERMISSIONS);
        if (packageInfo != null && packageInfo.requestedPermissions != null) {
            return new ArrayList<>(Arrays.asList(packageInfo.requestedPermissions));
        }
        return null;
    }

    private static PackageManager getPackageManager(Context context) {
        return context.getPackageManager();
    }

    private static String toHexString(byte[] bytes) {
        BigInteger bi = new BigInteger(1, bytes);
        return String.format("%0" + (bytes.length << 1) + "X", bi);
    }

    @RequiresApi(api = Build.VERSION_CODES.FROYO)
    public static String getCertificateDetails(X509Certificate cert) {
        try {
            StringBuilder sb = new StringBuilder();
            PublicKey publickey = cert.getPublicKey();
            sb.append("Subject: ").append(cert.getSubjectDN().getName()).append("\n");
            sb.append("Issuer: ").append(cert.getIssuerDN().getName()).append("\n");
            sb.append("Issued Date: ").append(cert.getNotBefore().toString()).append("\n");
            sb.append("Expiry Date: ").append(cert.getNotAfter().toString()).append("\n");
            sb.append("Algorithm: ").append(cert.getSigAlgName()).append(", Type: ").append(publickey.getFormat()).append(", Version: ").append(cert.getVersion()).append("\n");
            sb.append("Serial Number: ").append(cert.getSerialNumber().toString(16)).append("\n");
            sb.append("\nChecksums\n").append("MD5: ").append(getCertificateFingerprint(cert, "MD5").toLowerCase(Locale.ENGLISH)).append("\n");
            sb.append("SHA1: ").append(getCertificateFingerprint(cert, "SHA1").toLowerCase(Locale.ENGLISH)).append("\n");
            sb.append("SHA-256: ").append(getCertificateFingerprint(cert, "SHA-256").toLowerCase(Locale.ENGLISH)).append("\n");
            sb.append("\nPublic Key\n").append(Base64.encodeToString(publickey.getEncoded(), 0).replace("\n", "")).append("\n");
            return sb.toString();
        } catch (CertificateException | NoSuchAlgorithmException ignored) {
            return null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.FROYO)
    public static String getCertificateDetails(InputStream certStream) {
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("x509");
            X509Certificate cert = (X509Certificate) certificateFactory.generateCertificate(certStream);

            return getCertificateDetails(cert);
        } catch (CertificateException ignored) {
            return null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.FROYO)
    public static String getCertificateDetails(String rsaCertificatePath) {
        try {
            FileInputStream fileInputStream = new FileInputStream(rsaCertificatePath);
            return getCertificateDetails(fileInputStream);
        } catch (FileNotFoundException ignored) {
            return null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.FROYO)
    public static String getCertificateDetails(String apkPath, Context context) {
        try {
            X509Certificate[] certs = getX509Certificates(new File(apkPath), context);
            if (certs == null || certs.length < 1) {
                return null;
            }
            X509Certificate cert = certs[0];

            return getCertificateDetails(cert);
        } catch (CertificateException ignored) {
            return null;
        }
    }

    private static String getCertificateFingerprint(X509Certificate cert, String hashAlgorithm) throws NoSuchAlgorithmException, CertificateEncodingException {
        String hash;
        MessageDigest md = MessageDigest.getInstance(hashAlgorithm);
        byte[] rawCert = cert.getEncoded();
        hash = toHexString(md.digest(rawCert));
        md.reset();
        return hash;
    }

    private static X509Certificate[] getX509Certificates(File apkFile, Context context) throws CertificateException {
        X509Certificate[] certs;
        CertificateFactory certificateFactory;
        certificateFactory = CertificateFactory.getInstance("X509");

        PackageInfo packageInfo = null;
        if (apkFile != null && apkFile.exists()) {
            packageInfo = context.getPackageManager().getPackageArchiveInfo(apkFile.getAbsolutePath(), PackageManager.GET_SIGNATURES);
        }
        if (packageInfo != null) {
            certs = new X509Certificate[packageInfo.signatures.length];
            for (int i = 0; i < certs.length; i++) {
                byte[] cert = packageInfo.signatures[i].toByteArray();
                InputStream inStream = new ByteArrayInputStream(cert);
                certs[i] = (X509Certificate) certificateFactory.generateCertificate(inStream);
            }
            return certs;
        }
        return null;
    }

    private static void clean() {
        mAppNameParsed = false;
        mAppIcon = null;
        mAPKSize = Integer.MIN_VALUE;
        mAppName = null;
        mCertificate = null;
        mCompileSDK = null;
        mManifest = null;
        mManifestAsString = null;
        mMinSDK = null;
        mPackageName = null;
        mResDecoded = null;
        if (mPermissions == null) {
            mPermissions = new ArrayList<>();
        } else {
            mPermissions.clear();
        }
        mVersionCode = null;
        mVersionName = null;
        mTarSDK = null;
    }

    private static void extractManifestInfo(Uri apkUri, List<XMLEntry> manifestEntries, Context context) throws IOException {
        if (manifestEntries == null) return;

        for (XMLEntry entry : manifestEntries) {
            String tag = entry.getTag().trim();
            String value = entry.getValue();

            switch (tag) {
                case "package":
                    mPackageName = value;
                    break;
                case "android:label":
                    if (!mAppNameParsed && !value.startsWith("@")) {
                        mAppName = value;
                        mAppNameParsed = true;
                    }
                    break;
                case "android:name":
                    if (value != null && value.contains(".permission.")) {
                        mPermissions.add(value);
                    }
                    break;
                case "android:icon":
                    mAppIcon = getAppIcon(context.getContentResolver().openInputStream(apkUri), value);
                    break;
                case "android:versionName":
                    mVersionName = value;
                    break;
                case "android:versionCode":
                    mVersionCode = value;
                    break;
                case "android:compileSdkVersion":
                    mCompileSDK = value;
                    break;
                case "android:minSdkVersion":
                    mMinSDK = value;
                    break;
                case "android:targetSdkVersion":
                    mTarSDK = value;
                    break;
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void parse(String apkPath, Context context) {
        clean();

        PackageInfo packageInfo = getPackageManager(context).getPackageArchiveInfo(apkPath, PackageManager.GET_META_DATA);

        mApkPath = apkPath;
        mCertificate = getCertificateDetails(apkPath, context);

        mAPKSize = new File(apkPath).length();

        try {
            ZipFile mZipFile = getZipFile(apkPath);
            InputStream manifestStream = null, resStream = null;
            ZipEntry manifestEntry = mZipFile.getEntry("AndroidManifest.xml");
            ZipEntry resEntry = mZipFile.getEntry("resources.arsc");
            if (manifestEntry != null) {
                manifestStream = mZipFile.getInputStream(mZipFile.getEntry("AndroidManifest.xml"));
            }
            if (resEntry != null) {
                resStream = mZipFile.getInputStream(mZipFile.getEntry("resources.arsc"));
            }
            if (manifestStream != null) {
                mResDecoded = new ResourceTableParser(resStream).parse();
                mManifest = new aXMLDecoder(manifestStream, mResDecoded != null ? mResDecoded : null).decode();
                mManifestAsString = Utils.decodeAsString(mManifest);
            }
        } catch (Exception ignored) {}

        PackageManager pm = getPackageManager(context);
        ApplicationInfo ai = Objects.requireNonNull(packageInfo).applicationInfo;
        ai.sourceDir = apkPath;
        ai.publicSourceDir = apkPath;
        mAppName = pm.getApplicationLabel(Objects.requireNonNull(ai)).toString();
        mAppIcon = pm.getApplicationIcon(Objects.requireNonNull(ai));
        mActivities = getActivities(apkPath, context);
        mProviders = getProviders(apkPath, context);
        mReceivers = getReceivers(apkPath, context);
        mServices = getServices(apkPath, context);
        mPermissions = getPermissions(apkPath, context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mMinSDK = String.valueOf(ai.minSdkVersion);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mCompileSDK = String.valueOf(ai.compileSdkVersion);
        }
        mTarSDK = String.valueOf(ai.targetSdkVersion);

        mPackageName = packageInfo.packageName;
        mVersionName = packageInfo.versionName;
        mVersionCode = String.valueOf(packageInfo.versionCode);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void parse(Uri apkUri, Context context) {
        clean();
        String scheme = apkUri.getScheme();

        if ("file".equals(scheme)) {
            parse(apkUri.getPath(), context);
        } else if ("content".equals(scheme)) {
            if (Build.VERSION.SDK_INT >= 36) {
                ContentResolver resolver = context.getContentResolver();
                try (Cursor cursor = resolver.query(apkUri, new String[] {
                        OpenableColumns.DISPLAY_NAME
                }, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                        if (sizeIndex != -1) {
                            mAPKSize = cursor.getLong(sizeIndex);
                        }
                        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (nameIndex != -1) {
                            mAppName = cursor.getString(nameIndex);
                        }
                    }
                }

                try (ZipInputStream zis = new ZipInputStream(resolver.openInputStream(apkUri))) {
                    InputStream manifestStream = null;
                    InputStream resStream = null;
                    InputStream certStream = null;

                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        switch (entry.getName()) {
                            case "AndroidManifest.xml":
                                manifestStream = copyEntryStream(zis);
                                break;
                            case "resources.arsc":
                                resStream = copyEntryStream(zis);
                                break;
                            default:
                                if (entry.getName().startsWith("META-INF/") && entry.getName().endsWith(".RSA")) {
                                    certStream = copyEntryStream(zis);
                                }
                                break;
                        }
                    }

                    // Process collected streams
                    if (certStream != null) mCertificate = getCertificateDetails(certStream);
                    if (resStream != null) mResDecoded = new ResourceTableParser(resStream).parse();
                    if (manifestStream != null) {
                        mManifest = new aXMLDecoder(manifestStream, mResDecoded).decode();
                        mManifestAsString = Utils.decodeAsString(mManifest);
                    }

                    if (mManifest != null) {
                        extractManifestInfo(apkUri, mManifest, context);
                    }
                } catch (Exception ignored) {
                }
            } else {
                try (ParcelFileDescriptor fd = context.getContentResolver().openFileDescriptor(apkUri, "r")) {
                    parse("/proc/self/fd/" + Objects.requireNonNull(fd).getFd(), context);
                } catch (IOException ignored) {
                }
            }
        }
    }

}