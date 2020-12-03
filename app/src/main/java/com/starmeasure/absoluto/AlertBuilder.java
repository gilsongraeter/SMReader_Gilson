package com.starmeasure.absoluto;

import android.app.Activity;
import android.content.Context;
import androidx.appcompat.app.AlertDialog;
import android.view.View;

public class AlertBuilder {
    public static AlertDialog createProgressDialog(Context context, String message) {
        Activity activity = (Activity) context;
        final View alertProgress = activity.getLayoutInflater()
                .inflate(R.layout.alert_progress, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(context,
                android.R.style.Theme_Material_Dialog_Alert);
        return builder
                .setMessage(message)
                .setCancelable(false)
                .setView(alertProgress)
                .create();
    }
}
