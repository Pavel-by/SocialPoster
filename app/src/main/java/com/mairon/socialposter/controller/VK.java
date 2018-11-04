package com.mairon.socialposter.controller;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.mairon.socialposter.data.FileHelper;
import com.mairon.socialposter.data.VKPreferences;
import com.mairon.socialposter.model.SocialAttachment;
import com.mairon.socialposter.model.SocialPost;
import com.mairon.socialposter.model.SocialProfile;
import com.mairon.socialposter.model.vk.VKGroup;
import com.mairon.socialposter.model.vk.VKProfile;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCallback;
import com.vk.sdk.VKScope;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKBatchRequest;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiUserFull;
import com.vk.sdk.api.photo.VKImageParameters;
import com.vk.sdk.api.photo.VKUploadImage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.Getter;

public class VK implements SocialController {

    private final String TAG = "VK";

    public final static String PREF_GROUPS_TO_POST      = "vk_groups_list";
    public final static int    ERROR_FULL_GROUPS_RELOAD = 101892;
    public final static int    INVALID_GROUP_ID         = 0;

    public interface FetchGroupsListener {
        void onError(Error error);

        void onSuccess(
                ArrayList<VKGroup> before,
                List<VKGroup> newGroups
        );
    }

    private final int ONE_REQUEST_GROUPS_COUNT = 200;

    private        Activity                 activity;
    private        VKPreferences            preferences;
    /**
     * Права, запрашиваемые приложением
     */
    private        String[]                 scopes                  = {
            VKScope.WALL,
            VKScope.PHOTOS,
            VKScope.DOCS
    };
    /**
     * Поля, которые дополнительно запрашиваются при получении профиля пользователя
     */
    private        String                   userDataFields
                                                                    = TextUtils.join(",", new String[]{
            VKApiUserFull.FIELD_PHOTO_100
    });
    private static VKProfile                profile                 = new VKProfile();
    /**
     * Список групп, в которых состоит пользователь (подгружается постепенно)
     */
    private static ArrayList<VKGroup>       userGroupList           = new ArrayList<>();
    /**
     * Группы, которые были загружены вне основного списка {@link #userGroupList}. Например, при
     * получении списка групп, зарегистрированных для постов, не все группы могут оказаться в
     * загруженных группах пользователя и придется загружать новые.
     */
    private static Set<VKGroup>             additionalGroupList     = new HashSet<>();
    /**
     * Общее количество групп, в которых состоит пользователь
     */
    private static int                      userGroupTotalCount     = -1;
    /**
     * Определяет, выполняется ли в данный момент запрос на получение групп
     */
    @Getter
    private static boolean                  isFetchGroupsInProgress = false;
    /**
     * Список слушателей, зарегистрированных на получение текущего результата запроса на обновление
     * групп (очищается после выполнения каждого запроса)
     */
    private static Set<FetchGroupsListener> fetchGroupsListeners    = new HashSet<>();

    public VK(Activity activity) {
        this.activity = activity;
        this.preferences = new VKPreferences(activity);
    }

    @Override
    public void signIn() {
        profile.clear();
        VKSdk.login(activity, scopes);
    }

    @Override
    public void signOut() {
        profile.clear();
        VKSdk.logout();
    }

    @Override
    public boolean isSignedIn() {
        return VKSdk.isLoggedIn();
    }

    @Override
    public void getUserData(final ResponseListener<SocialProfile> responseListener) {
        if (!profile.isExpired()) {
            responseListener.onSuccess(profile);
        } else {
            VKRequest request = new VKRequest("users.get", VKParameters.from(
                    VKApiConst.FIELDS, userDataFields
            ));
            request.executeWithListener(new VKRequest.VKRequestListener() {
                @Override
                public void onComplete(VKResponse response) {
                    try {
                        JSONObject json = response.json.getJSONArray("response").getJSONObject(0);
                        profile.setExpired(false);
                        profile.setID(json.getInt("id"));
                        profile.setFirstName(json.getString("first_name"));
                        profile.setLastName(json.getString("last_name"));
                        profile.setImageUrl(json.getString(VKApiUserFull.FIELD_PHOTO_100));
                        responseListener.onSuccess(profile);
                    } catch (Exception e) {
                        responseListener.onError(new Error(0, "Ошибка при обработке ответа сервера"));
                    }
                }

                @Override
                public void onError(VKError error) {
                    responseListener.onError(new Error(error.errorCode, error.errorMessage));
                }
            });
        }
    }

    /**
     * Постит во все группы, выбранные пользователем
     *
     * @param post             Информация о посте
     * @param responseListener Слушатель результата
     */
    @Override
    public void post(
            final SocialPost post,
            final ResponseListener<SocialPost> responseListener
    )
    {
        final Integer[] loadedAttachmentCount = {0};
        final Integer[] notLoadedAttachmentCount = {0};
        final List<String> attachmentsArray = new ArrayList<>();
        ResponseListener<String> attachmentListener = new ResponseListener<String>() {
            @Override
            public void onSuccess(String response) {
                attachmentsArray.add(response);
                loadedAttachmentCount[0]++;
                if (loadedAttachmentCount[0] + notLoadedAttachmentCount[0] == post.getAttachments().size()) {
                    String[] groups = getGroupsToPost().split(",");
                    VKRequest[] requests = new VKRequest[groups.length];
                    for (int i = 0; i < groups.length; i++) {
                        VKRequest request = new VKRequest("wall.post", VKParameters.from(
                                "owner_id", "-" + groups[i],
                                "message", post.getText(),
                                "attachments", TextUtils.join(",", attachmentsArray.toArray())
                        ));
                        requests[i] = request;
                    }
                    VKBatchRequest batch = new VKBatchRequest(requests);
                    batch.executeWithListener(new VKBatchRequest.VKBatchRequestListener() {
                        @Override
                        public void onComplete(VKResponse[] responses) {
                            responseListener.onSuccess(post);
                        }

                        @Override
                        public void onError(VKError error) {
                            responseListener.onError(new Error(error.errorCode, error.errorMessage));
                        }
                    });
                }
            }

            @Override
            public void onError(Error error) {
                notLoadedAttachmentCount[0]++;
            }
        };
        for (SocialAttachment attachment : post.getAttachments()) {
            uploadAttachment(attachment, attachmentListener);
        }
    }

    public void uploadAttachment(
            final SocialAttachment attachment,
            final ResponseListener<String> responseListener
    )
    {
        switch (attachment.getType()) {
            case FILE: {
                VKRequest uploadDocRequest = VKApi.docs()
                        .uploadDocRequest(attachment.getFile());//, groupId);

                uploadDocRequest.executeWithListener(new VKRequest.VKRequestListener() {
                    @Override
                    public void onComplete(VKResponse response) {
                        Log.e(TAG, response.json.toString());
                        try {
                            JSONObject json = response.json.getJSONArray("response")
                                    .getJSONObject(0);
                            responseListener.onSuccess(
                                    "doc"
                                    + json.getInt("owner_id")
                                    + "_"
                                    + json.getInt("id")
                            );
                        } catch (JSONException e) {
                            e.printStackTrace();
                            responseListener.onError(new Error(0, "Ошибка обработки ответа"));
                        }
                    }

                    @Override
                    public void onError(VKError error) {
                        Log.e(TAG, "Error: " + error.errorMessage + "; " + error.errorReason);
                        Log.e(TAG, error.toString());
                        responseListener.onError(new Error(error.errorCode,
                                                           "Error: " + error.errorMessage + "; " +
                                                           error.errorReason));
                    }
                });
                break;
            }
            case PHOTO: {
                Log.e(TAG, "PHOTO");
                final VKRequest request = VKApi.photos().getWallUploadServer();
                request.executeWithListener(new VKRequest.VKRequestListener() {
                    @Override
                    public void onComplete(VKResponse response) {
                        Log.e(TAG, "GOT UPLOAD URL");
                        try {
                            //Получили ссылку, куда загружать файл
                            final String filePath
                                    = new VKUploadImage(attachment.getPhoto(), new VKImageParameters())
                                    .getTmpFile()
                                    .getAbsolutePath();
                            String uploadUrl = response.json.getJSONObject("response")
                                    .getString("upload_url");
                            uploadAndSaveFile(uploadUrl, filePath, responseListener);
                        } catch (Exception e) {
                            e.printStackTrace();
                            responseListener.onError(new Error(0, "Parse 1"));
                        }
                    }

                    @Override
                    public void onError(VKError error) {
                        responseListener.onError(new Error(error.errorCode, error.errorMessage));
                    }
                });
            }
        }
    }

    private void uploadAndSaveFile(String uploadUrl, String filePath, final ResponseListener<String> responseListener) {
        FileHelper.uploadFile(
                uploadUrl,
                filePath,
                new FileHelper.RequestProperties("photo"),
                new ResponseListener<FileHelper.Response>() {
                    @Override
                    public void onSuccess(FileHelper.Response response) {
                        Log.e(TAG, "UPLOADED FILE");
                        //Успешно загрузили файл
                        try {
                            Log.e(TAG, "Message: " +
                                       response.getMessage());
                            JSONObject json
                                    = new JSONObject(response.getMessage());
                            //Сохраняем файл
                            VKApi.photos()
                                    .saveWallPhoto(VKParameters.from(
                                            "photo", json.getString("photo"),
                                            "server", json.getString("server"),
                                            "hash", json.getString("hash")
                                    ))
                                    .executeWithListener(new VKRequest.VKRequestListener() {
                                        @Override
                                        public void onComplete(VKResponse response) {
                                            //Успешно сохранили фотографию
                                            try {
                                                JSONObject json
                                                        = response.json
                                                        .getJSONArray("response")
                                                        .getJSONObject(0);
                                                responseListener.onSuccess(
                                                        "photo"
                                                        + json.getInt("owner_id")
                                                        + "_" + json.getInt("id")
                                                );
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                                responseListener.onError(new Error(0, "Ошибка парминга ответа"));
                                            }
                                        }

                                        @Override
                                        public void onError(VKError error) {
                                            responseListener.onError(new Error(error.errorCode, error
                                                    .toString()));
                                        }
                                    });
                        } catch (JSONException e) {
                            responseListener.onError(new Error(0, "Ошибка парсинга ответа"));
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(Error error) {
                        responseListener.onError(error);
                    }
                });
    }

    public boolean isAllGroupsLoaded() {
        return userGroupList.size() == userGroupTotalCount;
    }

    public int getUserGroupTotalCount() {
        return userGroupTotalCount;
    }

    public int getLoadedUserGroupsCount() {
        return userGroupList.size();
    }

    /**
     * Запросить дополнение текущего списка групп пользователя
     *
     * @param fetchListener Слушатель, который будет бызван по окончании запроса. В случае, если
     *                      будет обнаружено несоответствие между списком групп на сервере и списком
     *                      уже загруженных групп, будет выполнена полная перезагрузка всего списка.
     *                      В этом случае будет вызван метод onError слушателя с кодом ошибки {@link
     *                      #ERROR_FULL_GROUPS_RELOAD}
     */
    public void fetchUserGroups(FetchGroupsListener fetchListener) {
        if (fetchListener != null) fetchGroupsListeners.add(fetchListener);
        if (isFetchGroupsInProgress) {
            return;
        }
        isFetchGroupsInProgress = true;
        if (isAllGroupsLoaded()) {
            Log.e(TAG,
                  "All loaded. We will not update. Listeners count is " + userGroupList.size());
            notifyFetchGroupsEnded(userGroupList, new ArrayList<VKGroup>());
            isFetchGroupsInProgress = false;
        } else {
            Log.e(TAG,
                  "Loaded count: " + userGroupList.size() + " Total count: " + userGroupTotalCount);
            VKRequest request = new VKRequest(
                    "groups.get",
                    VKParameters.from(
                            "filter", "editor",
                            "extended", 1,
                            "offset", userGroupList.size(),
                            "count", ONE_REQUEST_GROUPS_COUNT,
                            "fields", "members_count"
                    )
            );
            request.executeWithListener(new VKRequest.VKRequestListener() {
                @Override
                public void onComplete(VKResponse response) {
                    try {
                        Log.e(TAG, response.json.toString());
                        JSONObject json = response.json.getJSONObject("response");
                        if (userGroupTotalCount >= 0 &&
                            userGroupTotalCount != json.getInt("count"))
                        {
                            notifyFetchGroupsEnded(new Error(
                                    ERROR_FULL_GROUPS_RELOAD,
                                    "Обнаружено несоответствие между группами на устройстве и на сервере. Выполняем обновление."
                            ), false);
                            userGroupTotalCount = json.getInt("count");
                            Log.e(TAG, "Set count to " + userGroupTotalCount);
                            userGroupList.clear();
                            isFetchGroupsInProgress = false;
                            fetchUserGroups(null);
                        } else {
                            userGroupTotalCount = json.getInt("count");
                            Log.e(TAG, "Set count to " + userGroupTotalCount);
                            List<VKGroup> newGroups = new ArrayList<>();
                            JSONArray items = json.getJSONArray("items");
                            for (int i = 0; i < items.length(); i++) {
                                JSONObject item = items.getJSONObject(i);
                                VKGroup group = new VKGroup();
                                group.setID(item.getInt("id"));
                                group.setImageUrl(item.getString("photo_100"));
                                group.setName(item.getString("name"));
                                group.setMembersCount(item.getInt("members_count"));
                                newGroups.add(group);
                            }
                            ArrayList<VKGroup> prevGroups = new ArrayList<>(userGroupList);
                            userGroupList.addAll(newGroups);
                            notifyFetchGroupsEnded(prevGroups, newGroups);
                            isFetchGroupsInProgress = false;
                        }
                    } catch (JSONException e) {
                        isFetchGroupsInProgress = false;
                        e.printStackTrace();
                        Log.e(TAG, "Не удалось обработать ответ сервера.");
                        Log.e(TAG, e.getMessage());
                        notifyFetchGroupsEnded(new Error(0, "Не удалось обработать ответ сервера."));
                    }
                }

                @Override
                public void onError(VKError error) {
                    isFetchGroupsInProgress = false;
                    notifyFetchGroupsEnded(new Error(error.errorCode, error.errorMessage));
                }
            });
        }
    }

    /**
     * Получить список слушателей, зарегистрированных на получение результатов запроса новых групп
     *
     * @return Список слушателей
     */
    public Collection<FetchGroupsListener> getFetchGroupsListeners() {
        return fetchGroupsListeners;
    }

    private void notifyFetchGroupsEnded(
            ArrayList<VKGroup> before,
            List<VKGroup> added
    )
    {
        for (FetchGroupsListener listener : fetchGroupsListeners) {
            listener.onSuccess(before, added);
        }
        fetchGroupsListeners.clear();
    }

    private void notifyFetchGroupsEnded(Error error) {
        notifyFetchGroupsEnded(error, true);
    }

    private void notifyFetchGroupsEnded(
            Error error,
            boolean clearListeners
    )
    {
        for (FetchGroupsListener listener : fetchGroupsListeners) {
            listener.onError(error);
        }
        if (clearListeners) fetchGroupsListeners.clear();
    }

    public ArrayList<VKGroup> getLoadedUserGroupsList()
    {
        return userGroupList;
    }

    public String getGroupsToPost() {
        return preferences.getGroupsToPost();
    }

    public void setGroupsToPost(String groupsToPost) {
        preferences.setGroupsToPost(groupsToPost);
    }

    public void setGroupsToPost(Collection<VKGroup> groupsToPost) {
        if (groupsToPost == null) {
            return;
        }
        setGroupsToPost(joinGroups(groupsToPost));
    }

    private String joinGroups(Collection<VKGroup> groups) {
        if (groups.size() == 0) {
            return "";
        }
        String[] array = new String[groups.size()];
        int i = 0;
        for (VKGroup group : groups) {
            array[i++] = group.getID().toString();
        }
        return TextUtils.join(",", array);
    }

    /**
     * Получить список групп, в которые пользователь разрешил делать посты
     *
     * @param responseListener Список групп, в которые пользователь разрешил делать посты
     */
    public void getGroupsToPost(final ResponseListener<ArrayList<VKGroup>> responseListener) {
        Log.e(TAG, "getGroupsToPost");
        final ArrayList<VKGroup> result = new ArrayList<>();
        String groupsString = preferences.getGroupsToPost();
        if (groupsString == null || groupsString.length() == 0){
            responseListener.onSuccess(result);
            return;
        }
        List<String> ids = new ArrayList<>(Arrays.asList(groupsString.split(",")));
        for (int i = 0; i < ids.size(); ) {
            if (ids.get(i).equals("")) {
                i++;
                continue;
            }
            VKGroup group = getGroupFromLoaded(Integer.parseInt(ids.get(i)));
            if (group != null) {
                result.add(group);
                ids.remove(i);
                Log.e(TAG, "Remove at " + i);
            } else i++;
        }
        if (ids.size() > 0) {
            Log.e(TAG, "Loading");
            fetchAdditionalGroups(TextUtils.join(",", ids), new FetchGroupsListener() {
                @Override
                public void onError(Error error) {
                    responseListener.onError(error);
                }

                @Override
                public void onSuccess(
                        ArrayList<VKGroup> before,
                        List<VKGroup> newGroups
                )
                {
                    result.addAll(newGroups);
                    responseListener.onSuccess(result);
                }
            });
        } else {
            Log.e(TAG, "onSuccess");
            responseListener.onSuccess(result);
        }
    }

    public void addGroupToPost(int groupId) {
        if (groupId == INVALID_GROUP_ID) return;
        String groups = getGroupsToPost();
        if (groups.length() > 0) groups += ",";
        groups += groupId;
        setGroupsToPost(groups);
    }

    private void fetchAdditionalGroups(
            String groups,
            final FetchGroupsListener listener
    )
    {
        VKRequest request = new VKRequest(
                "groups.getById",
                VKParameters.from(
                        "group_ids", groups,
                        "fields", "members_count"
                )
        );
        request.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                try {
                    List<VKGroup> groups = new ArrayList<>();
                    JSONArray items = response.json.getJSONArray("response");
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.getJSONObject(i);
                        VKGroup group = new VKGroup();
                        group.setID(item.getInt("id"));
                        group.setName(item.getString("name"));
                        group.setMembersCount(item.getInt("members_count"));
                        group.setImageUrl(item.getString("photo_100"));
                        groups.add(group);
                    }
                    ArrayList<VKGroup> before = new ArrayList<>(additionalGroupList);
                    additionalGroupList.addAll(groups);
                    listener.onSuccess(before, groups);
                } catch (JSONException e) {
                    e.printStackTrace();
                    listener.onError(new Error(0, "Не удалось обработать ответ сервера"));
                }
            }

            @Override
            public void onError(VKError error) {
                listener.onError(new Error(error.errorCode, error.errorMessage));
            }
        });
    }

    public VKGroup getGroupFromLoaded(int id) {
        for (VKGroup group : userGroupList) if (group.getID() == id) return group;
        for (VKGroup group : additionalGroupList) if (group.getID() == id) return group;
        return null;
    }

    public boolean onActivityResult(
            int requestCode,
            int resultCode,
            Intent data,
            VKCallback<VKAccessToken> listener
    )
    {
        return VKSdk.onActivityResult(requestCode, resultCode, data, listener);
    }
}
