package com.mairon.socialposter.model.vk;

import com.mairon.socialposter.model.SocialPost;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data()
@EqualsAndHashCode(callSuper = true)
public class VKPost extends SocialPost {
    private Integer ID;
}
