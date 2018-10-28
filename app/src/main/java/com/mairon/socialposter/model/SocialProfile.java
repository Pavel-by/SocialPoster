package com.mairon.socialposter.model;

import android.graphics.Bitmap;

import lombok.Data;

@Data
public class SocialProfile {
    private boolean expired = true;
    private String firstName;
    private String lastName;
    private String imageUrl;
    private Bitmap image;

    public void clear() {
        expired = true;
        firstName = null;
        lastName = null;
        imageUrl = null;
        image = null;
    }
}
