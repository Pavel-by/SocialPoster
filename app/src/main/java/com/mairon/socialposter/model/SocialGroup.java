package com.mairon.socialposter.model;

import android.graphics.Bitmap;

import lombok.Data;

@Data
public class SocialGroup {
    private String imageUrl;
    private Bitmap image;
    private String name;
    private int membersCount;
}
