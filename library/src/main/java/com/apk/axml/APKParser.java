package com.apk.axml;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import java.io.ByteArrayInputStream;
import java.io.File;
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
import java.util.Objects;

/*
 * Created by APK Explorer & Editor <apkeditor@protonmail.com> on January 25, 2023
 */
public class APKParser {

    private final Context mContext;
    private static Drawable mAppIcon = null;
    private static List<String> mPermissions = null;
    private static long mAPKSize = Integer.MIN_VALUE;
    private static String mAppName = null, mAPKPath = null, mCertificate = null, mCompileSDK = null, mManifest = null, mMinSDK = null,
            mPackageName = null, mVersionCode = null, mVersionName = null, mTarSDK = null;

    public APKParser(String path, Context context) {
        mAPKPath = path;
        mContext = context;
    }

    public boolean isParsed() {
        return mPackageName != null;
    }

    public static Drawable getAppIcon() {
        return mAppIcon;
    }

    public static List<String> getPermissions() {
        return mPermissions;
    }

    private static PackageInfo getPackageInfo(String apkPath, Context context) {
        return getPackageManager(context).getPackageArchiveInfo(apkPath, 0);
    }

    private static PackageManager getPackageManager(Context context) {
        return context.getPackageManager();
    }

    public static String getAppName() {
        return mAppName;
    }

    public static long getAPKSize() {
        return mAPKSize;
    }

    public static String getCompiledSDKVersion() {
        return mCompileSDK;
    }

    public static String getCertificate() {
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

    public static String getManifest() {
        return mManifest;
    }

    public static String getMinSDKVersion() {
        return mMinSDK;
    }

    public static String getPackageName() {
        return mPackageName;
    }

    public static String getTargetSDKVersion() {
        return mTarSDK;
    }

    public static String getVersionCode() {
        return mVersionCode;
    }

    public static String getVersionName() {
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

    public static String getCertificateDetails(Context context) {
        try {
            StringBuilder sb = new StringBuilder();
            X509Certificate[] certs = getX509Certificates(new File(mAPKPath), context);
            if (certs == null || certs.length < 1) {
                return null;
            }
            X509Certificate cert = certs[0];

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
            sb.append("\nPublic Key\n").append(publickey.toString().split("=")[1].split(",")[0]).append("\n");
            return sb.toString();
        } catch (CertificateException | NoSuchAlgorithmException ignored) {
            return null;
        }
    }

    public void parse() {
        mManifest = null;
        mCompileSDK = null;
        mTarSDK = null;
        mMinSDK = null;
        if (mPermissions == null) {
            mPermissions = new ArrayList<>();
        } else {
            mPermissions.clear();
        }
        mAppName = getPackageManager(mContext).getApplicationLabel(getPackageInfo(mAPKPath, mContext).applicationInfo).toString();
        mAppIcon = getPackageInfo(mAPKPath, mContext).applicationInfo.loadIcon(getPackageManager(mContext));
        mPackageName = getPackageInfo(mAPKPath, mContext).packageName;
        mVersionName = getPackageInfo(mAPKPath, mContext).versionName;
        mVersionCode = String.valueOf(getPackageInfo(mAPKPath, mContext).versionCode);
        mCertificate = getCertificateDetails(mContext);
        try {
            mManifest = aXMLDecoder.decodeManifest(new File(mAPKPath)).trim();
        } catch (Exception ignored) {
        }
        mAPKSize = new File(mAPKPath).length();
        if (mManifest != null) {
            for (String line : Objects.requireNonNull(mManifest).trim().split("\\r?\\n")) {
                if (line.trim().startsWith("android:name=\"") && line.contains(".permission.")) {
                    mPermissions.add(line.replace("android:name=", "").replace("\"", "").trim());
                } else if (line.trim().startsWith("android:compileSdkVersion=\"")) {
                    mCompileSDK = line.replace("android:compileSdkVersion=", "").replace("\"", "").trim();
                } else if (line.trim().startsWith("android:minSdkVersion=\"")) {
                    mMinSDK = line.replace("android:minSdkVersion=", "").replace("\"", "").trim();
                } else if (line.trim().startsWith("android:targetSdkVersion=\"")) {
                    mTarSDK = line.replace("android:targetSdkVersion=", "").replace("\"", "").trim();
                }
            }
        }
    }

}