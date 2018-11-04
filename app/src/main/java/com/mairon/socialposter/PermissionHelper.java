package com.mairon.socialposter;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;

import java.util.HashMap;
import java.util.Map;

public class PermissionHelper {

    private static Map<String, String> requestPermissionMessage = new HashMap<String, String>() {{
        put(Manifest.permission.INTERNET, "Доступ к интернету");
        put(Manifest.permission.ACCESS_NETWORK_STATE, "Доступ к состоянию сети");
        put(Manifest.permission.READ_EXTERNAL_STORAGE, "Доступ к файлам на устройстве");
    }};

    public static void requestPermission(
            final Context context,
            final int requestCode,
            final String permission
    )
    {
        if (hasPermissions(context, permission)) return;
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                (Activity) context,
                permission))
        {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
            alertBuilder.setCancelable(true);
            alertBuilder.setTitle("Необходимо разрешение");
            alertBuilder.setMessage(getRequestPermissionMessage(permission));
            alertBuilder.setPositiveButton(android.R.string.yes,
                                           new DialogInterface.OnClickListener() {
                                               public void onClick(DialogInterface dialog, int which) {
                                                   ActivityCompat.requestPermissions((Activity) context,
                                                                                     new String[] { permission },
                                                                                     requestCode);
                                               }
                                           });
            AlertDialog alert = alertBuilder.create();
            alert.show();
        } else {
            ActivityCompat
                    .requestPermissions(
                            (Activity) context,
                            new String[]{permission},
                            requestCode);
        }
    }

    public static boolean hasPermissions(
            Context context,
            String... permissions
    )
    {
        int currentAPIVersion = Build.VERSION.SDK_INT;
        if (currentAPIVersion >= android.os.Build.VERSION_CODES.M) {
            boolean hasPermissions = true;
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(context,
                                                      permission) !=
                    PackageManager.PERMISSION_GRANTED)
                {
                    hasPermissions = false;
                    break;
                }
            }
            return hasPermissions;
        } else {
            return true;
        }
    }

    public static String getRequestPermissionMessage(String permission) {
        if (requestPermissionMessage.containsKey(permission))
            return requestPermissionMessage.get(permission);
        return permission;
    }
}
