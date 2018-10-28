package com.mairon.socialposter;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class Preferences {

    public static String VK_TOKEN;

    private Activity context;
    private SharedPreferences sharedPreferences;

    public Preferences (Activity context) {
        this.context = context;
        this.sharedPreferences = context.getPreferences(Context.MODE_PRIVATE);
    }

}
