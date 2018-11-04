package com.mairon.socialposter.model.vk;

import com.mairon.socialposter.model.SocialGroup;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false, of = {"ID"})
public class VKGroup extends SocialGroup {
    private Integer ID;
}
