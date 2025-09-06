package com.apk.axml;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Base64;

import com.apk.axml.serializableItems.ResEntry;
import com.apk.axml.serializableItems.XMLItems;

import java.io.ByteArrayInputStream;
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
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/*
 * Created by APK Explorer & Editor <apkeditor@protonmail.com> on January 25, 2023
 */
public class APKParser {

    private static boolean mAppNameParsed = false;
    private static Drawable mAppIcon = null;
    private static List<String> mPermissions = null;
    private static long mAPKSize = Integer.MIN_VALUE;
    private static List<ResEntry> mResDecoded = null;
    private static List<XMLItems> mManifest = null;
    private static String mApkPath = null, mAppName = null, mCertificate = null, mCompileSDK = null, mMinSDK = null,
            mPackageName = null, mVersionCode = null, mVersionName = null, mTarSDK = null;
    private static ZipFile mZipFile = null;

    public APKParser() {
    }

    public boolean isParsed() {
        return mPackageName != null;
    }

    public Drawable getAppIcon() {
        return mAppIcon;
    }

    private static Drawable getAppIcon(PackageInfo packageInfo, InputStream is, Context context) {
        if (mResDecoded != null) {
            return Drawable.createFromStream(is, null);
        }
        return mAppIcon = packageInfo.applicationInfo.loadIcon(getPackageManager(context));
    }

    public File getApkFile() {
        if (isParsed()) {
            return new File(mApkPath);
        }
        return null;
    }

    public List<String> getPermissions() {
        return mPermissions;
    }

    private static PackageInfo getPackageInfo(String apkPath, Context context) {
        return getPackageManager(context).getPackageArchiveInfo(apkPath, 0);
    }

    private static PackageManager getPackageManager(Context context) {
        return context.getPackageManager();
    }

    public String getApkPath() {
        if (isParsed()) {
            return mApkPath;
        }
        return null;
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

    private static String getCertificateFingerprint(X509Certificate cert, String hashAlgorithm) throws NoSuchAlgorithmException, CertificateEncodingException {
        String hash;
        MessageDigest md = MessageDigest.getInstance(hashAlgorithm);
        byte[] rawCert = cert.getEncoded();
        hash = toHexString(md.digest(rawCert));
        md.reset();
        return hash;
    }

    public List<ResEntry> getDecodedResources() {
        return mResDecoded;
    }

    public List<XMLItems> getManifest() {
        return mManifest;
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

    private static String toHexString(byte[] bytes) {
        BigInteger bi = new BigInteger(1, bytes);
        return String.format("%0" + (bytes.length << 1) + "X", bi);
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

    @TargetApi(Build.VERSION_CODES.FROYO)
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

    public static String getCertificateDetails(String rsaCertificatePath) {
        try {
            FileInputStream fileInputStream = new FileInputStream(rsaCertificatePath);
            CertificateFactory certificateFactory = CertificateFactory.getInstance("x509");
            X509Certificate cert = (X509Certificate) certificateFactory.generateCertificate(fileInputStream);

            return getCertificateDetails(cert);
        } catch (CertificateException | FileNotFoundException ignored) {
            return null;
        }
    }

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

    private static void clean() {
        mAppNameParsed = false;
        mAppIcon = null;
        mAPKSize = Integer.MIN_VALUE;
        mAppName = null;
        mCertificate = null;
        mCompileSDK = null;
        mManifest = null;
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

    private ZipFile getZipFile(String apkPath) throws IOException {
        return new ZipFile(apkPath);
    }

    public void parse(String apkPath, Context context) {
        clean();

        PackageInfo packageInfo = getPackageInfo(apkPath, context);

        mApkPath = apkPath;
        mCertificate = getCertificateDetails(apkPath, context);

        mAPKSize = new File(apkPath).length();

        try {
            mZipFile = getZipFile(apkPath);
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
            }
        } catch (Exception ignored) {}

        if (mManifest != null) {
            for (XMLItems items : mManifest) {
                if (items.getTag().trim().equals("android:label") && !mAppNameParsed) {
                    if (mResDecoded != null) {
                        mAppName = items.getValue(mResDecoded);
                    } else {
                        mAppName = getPackageManager(context).getApplicationLabel(packageInfo.applicationInfo).toString();
                    }
                    mAppNameParsed = true;
                } else if (items.getTag().trim().equals("android:icon")) {
                    try {
                        InputStream iconStream;
                        if (items.getValue(mResDecoded) != null) {
                            ZipEntry iconEntry = mZipFile.getEntry(items.getValue(mResDecoded));
                            if (iconEntry != null) {
                                iconStream = mZipFile.getInputStream(iconEntry);
                                if (iconStream != null) {
                                    mAppIcon = getAppIcon(packageInfo, iconStream, context);
                                }
                            }
                        }
                    } catch (IOException ignored) {
                        mAppIcon = packageInfo.applicationInfo.loadIcon(getPackageManager(context));
                    }
                } else if (items.getTag().trim().equals("android:name") && items.getValue().contains(".permission.")) {
                    mPermissions.add(items.getValue());
                } else if (items.getTag().trim().equals("android:compileSdkVersion")) {
                    mCompileSDK = items.getValue();
                } else if (items.getTag().trim().equals("android:minSdkVersion")) {
                    mMinSDK = items.getValue();
                } else if (items.getTag().trim().equals("android:targetSdkVersion")) {
                    mTarSDK = items.getValue();
                }
            }
        }

        mPackageName = packageInfo.packageName;
        mVersionName = packageInfo.versionName;
        mVersionCode = String.valueOf(packageInfo.versionCode);
    }

}