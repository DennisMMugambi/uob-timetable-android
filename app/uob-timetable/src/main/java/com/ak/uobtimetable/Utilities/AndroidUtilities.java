package com.ak.uobtimetable.Utilities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.text.Html;
import android.text.Spanned;
import android.util.TypedValue;
import android.view.View;

import com.ak.uobtimetable.BuildConfig;
import com.ak.uobtimetable.R;
import com.ak.uobtimetable.Utilities.Logging.Logger;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;

import javax.security.auth.x500.X500Principal;

/**
 * Contains various Android-specific utility methods.
 */
public class AndroidUtilities {

    public enum NetworkType { None, Infrastructure, Cellular }

    private static PackageInfo getPackageInfo(Context context){

        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public static boolean hasNetwork(Context context){

        ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    public static NetworkType getNetwork(Context context){

        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if (isConnected == false)
            return NetworkType.None;

        int type = activeNetwork.getType();

        if (type == ConnectivityManager.TYPE_WIFI || type == ConnectivityManager.TYPE_ETHERNET)
            return NetworkType.Infrastructure;

        return NetworkType.Cellular;
    }

    public static String getNetworkRaw(Context context){

        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if (isConnected == false)
            return null;

        int type = activeNetwork.getType();

        try {
            return GeneralUtilities.getConstantName(ConnectivityManager.class, type, "TYPE_");
        } catch (Exception e) {
            return null;
        }
    }

    public static int buildVersionCode(Context context) {

        try {
            return getPackageInfo(context).versionCode;
        }
        catch (Exception e) {
            return -1;
        }
    }

    public static String buildVersionName(Context context) {

        try {
            return getPackageInfo(context).versionName;
        } catch (Exception e) {
            return "";
        }
    }

    public static int apiLevel(){

        return android.os.Build.VERSION.SDK_INT;
    }

    public static String apiLevelName(){

        try {
            return GeneralUtilities.getConstantName(Build.VERSION_CODES.class, apiLevel(), "");
        } catch (Exception e){
            return "Unknown";
        }
    }

    public static int targetApiLevel(Context context){

        try {
            return getPackageInfo(context).applicationInfo.targetSdkVersion;
        }
        catch (Exception e) {
            return -1;
        }
    }

    public static Date buildDate(){

        return new Date(BuildConfig.TIMESTAMP);
    }

    public static Date packageUpdateDate(Context context){

        try {
            long ms = getPackageInfo(context).firstInstallTime;
            return new Date(ms);
        } catch (Exception e){
            return new Date();
        }
    }

    @SuppressWarnings("deprecation")
    public static Spanned fromHtml(String source) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(source);
        }
    }

    public static void trySetElevation(View control, float elevation){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            control.setElevation(elevation);
    }

    public static void openPlayStorePage(Activity activity){

        String appPackageName = activity.getPackageName();
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException anfe) {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
        }
    }

    public static boolean isTabletLayout(Context content){

        return content.getResources().getBoolean(R.bool.tablet_width);
    }

    public static boolean isReleaseSigned(Context context){

        try {
            PackageManager packageManager = context.getPackageManager();
            X500Principal DEBUG_DN = new X500Principal("CN=Android Debug,O=Android,C=US");
            Signature raw = packageManager.getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES).signatures[0];
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate)cf.generateCertificate(new ByteArrayInputStream(raw.toByteArray()));
            boolean debug = cert.getSubjectX500Principal().equals(DEBUG_DN);
            return debug == false;
        } catch (Exception e){
            Logger.getInstance().error("AndroidUtilities", "Failed to detect signing mode: " + GeneralUtilities.nestedThrowableToString(e));
            return true;
        }
    }
}
