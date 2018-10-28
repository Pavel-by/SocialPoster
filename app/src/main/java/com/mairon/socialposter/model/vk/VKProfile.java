package com.mairon.socialposter.model.vk;

import com.mairon.socialposter.model.SocialProfile;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class VKProfile extends SocialProfile {
    private Integer ID;
}