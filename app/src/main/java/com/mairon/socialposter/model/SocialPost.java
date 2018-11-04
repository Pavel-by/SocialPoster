package com.mairon.socialposter.model;

import android.graphics.Bitmap;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class SocialPost {
    private String       text;
    private List<SocialAttachment> attachments;
}
