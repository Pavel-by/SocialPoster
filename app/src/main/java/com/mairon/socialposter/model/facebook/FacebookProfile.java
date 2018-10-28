package com.mairon.socialposter.model.facebook;

import com.mairon.socialposter.model.SocialProfile;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Generated;
import lombok.NoArgsConstructor;
import lombok.ToString;


@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class FacebookProfile extends SocialProfile {
    private String ID;
}
