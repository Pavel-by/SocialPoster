package com.mairon.socialposter.data;

import com.mairon.socialposter.controller.SocialController;
import com.mairon.socialposter.model.SocialProfile;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;

/**
 * Класс, созданный исключительно для облегчения синхронизации между активити (например без него передача
 */
public class SocialStorage {

    public final static int VK = 1;
    public final static int FACEBOOK = 2;
    public final static int TWITTER = 3;

    private static SocialStorage socialStorageInstance;

    private SocialStorage() {
    }

    public static SocialStorage getInstance() {
        if (socialStorageInstance == null) socialStorageInstance = new SocialStorage();
        return socialStorageInstance;
    }

    @Getter
    public Map<Integer, SocialProfile> profiles = new HashMap<>();

    @Getter
    public Map<Integer, SocialController> controllers = new HashMap<>();
}
