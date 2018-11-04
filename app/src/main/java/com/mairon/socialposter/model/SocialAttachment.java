package com.mairon.socialposter.model;

import android.graphics.Bitmap;

import java.io.File;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

public class SocialAttachment {
    @Getter
    private final SocialAttachmentType    type;
    @Getter
    @Setter
    private Bitmap photo;
    @Getter
    @Setter
    private File file;

    public SocialAttachment(SocialAttachmentType type) {
        this.type = type;
    }

    public SocialAttachment(File file) {
        this.file = file;
        this.type = SocialAttachmentType.FILE;
    }

    public SocialAttachment(Bitmap photo) {
        this.photo = photo;
        this.type = SocialAttachmentType.PHOTO;
    }

    public SocialAttachment(String filePath) {
        this.file = new File(filePath);
        this.type = SocialAttachmentType.FILE;
    }
}
