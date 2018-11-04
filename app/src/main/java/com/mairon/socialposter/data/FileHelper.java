package com.mairon.socialposter.data;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import com.mairon.socialposter.controller.SocialController;
import com.vk.sdk.VKSdk;
import com.vk.sdk.VKUIHelper;
import com.vk.sdk.api.photo.VKImageParameters;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class FileHelper {

    public static void uploadFile(
            String uploadUrl,
            String filePath,
            RequestProperties requestProperties,
            SocialController.ResponseListener<Response> responseListener
    )
    {
        new UploadTask(uploadUrl, filePath, requestProperties, responseListener).execute();
    }

    /**
     * Method for return file path of Gallery image/ Document / Video / Audio
     *
     * @param context
     * @param uri
     *
     * @return path of the selected image file from gallery
     */
    public static String getPath(
            final Context context,
            final Uri uri
    )
    {

        // DocumentProvider
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT &&
            DocumentsContract.isDocumentUri(context, uri))
        {

            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/"
                           + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};

                return getDataColumn(context, contentUri, selection,
                                     selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }


    /**
     * Get the value of the data column for this Uri. This is useful for MediaStore Uris, and other
     * file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     *
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(
            Context context,
            Uri uri,
            String selection,
            String[] selectionArgs
    )
    {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection,
                                                        selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     *
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri
                                                                      .getAuthority());
    }

    /**
     * @param uri The Uri to check.
     *
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri
                                                                          .getAuthority());
    }

    /**
     * @param uri The Uri to check.
     *
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri
                                                                      .getAuthority());
    }

    /**
     * @param uri The Uri to check.
     *
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri
                                                                       .getAuthority());
    }

    private static class UploadTask extends AsyncTask<Void, Void, Response> {

        private SocialController.ResponseListener<Response> responseListener;
        private String                                      filePath;
        private String                                      uploadUrl;
        private RequestProperties requestProperties;

        UploadTask(
                String uploadUrl,
                String filePath,
                RequestProperties requestProperties,
                SocialController.ResponseListener<Response> responseListener
        )
        {
            this.uploadUrl = uploadUrl;
            this.filePath = filePath;
            this.requestProperties = requestProperties;
            this.responseListener = responseListener;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Response doInBackground(Void... voids) {

            int serverResponseCode = 0;

            HttpURLConnection connection;
            DataOutputStream dataOutputStream;
            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";


            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            int maxBufferSize = 1 * 1024 * 1024;
            File selectedFile = new File(filePath);


            String[] parts = filePath.split("/");
            final String fileName = parts[parts.length - 1];

            if (selectedFile.isFile()) {
                try {
                    FileInputStream fileInputStream = new FileInputStream(selectedFile);
                    URL url = new URL(uploadUrl);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);//Allow Inputs
                    connection.setDoOutput(true);//Allow Outputs
                    connection.setUseCaches(false);//Don't use a cached Copy
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Connection", "Keep-Alive");
                    connection.setRequestProperty("ENCTYPE", "multipart/form-data");
                    connection.setRequestProperty("Content-Type",
                                                  "multipart/form-data;boundary=" + boundary);
                    connection.setRequestProperty(requestProperties.fileFieldName, filePath);

                    //creating new dataoutputstream
                    dataOutputStream = new DataOutputStream(connection.getOutputStream());

                    //writing bytes to data outputstream
                    dataOutputStream.writeBytes(twoHyphens + boundary + lineEnd);
                    dataOutputStream.writeBytes(
                            "Content-Disposition: form-data; name=\""+ requestProperties.fileFieldName + "\";filename=\""
                            + filePath + "\"" + lineEnd);

                    dataOutputStream.writeBytes(lineEnd);

                    //returns no. of bytes present in fileInputStream
                    bytesAvailable = fileInputStream.available();
                    //selecting the buffer size as minimum of available bytes or 1 MB
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    //setting the buffer as byte array of size of bufferSize
                    buffer = new byte[bufferSize];

                    //reads bytes from FileInputStream(from 0th index of buffer to buffersize)
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                    //loop repeats till bytesRead = -1, i.e., no bytes are left to read
                    while (bytesRead > 0) {
                        //write the bytes read from inputstream
                        dataOutputStream.write(buffer, 0, bufferSize);
                        bytesAvailable = fileInputStream.available();
                        bufferSize = Math.min(bytesAvailable, maxBufferSize);
                        bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                    }

                    dataOutputStream.writeBytes(lineEnd);
                    dataOutputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                    serverResponseCode = connection.getResponseCode();
                    String serverResponseMessage = connection.getResponseMessage();
                    InputStream inputStream = connection.getInputStream();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader((connection.getInputStream())));
                    StringBuilder stringBuilder = new StringBuilder();
                    String output;
                    while ((output = bufferedReader.readLine()) != null) {
                        stringBuilder.append(output);
                    }
                    Response response = new Response(serverResponseCode, stringBuilder.toString());
                    //response code of 200 indicates the server status OK

                    //closing the input and output streams
                    fileInputStream.close();
                    dataOutputStream.flush();
                    dataOutputStream.close();
                    inputStream.close();
                    connection.disconnect();
                    return response;

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Log.e("VK", e.getMessage());
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    Log.e("VK", e.getMessage());

                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("VK", e.getMessage());
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Response response) {
            if (response != null && response.getCode() == HttpURLConnection.HTTP_OK) {
                responseListener.onSuccess(response);
            } else {
                responseListener.onError(new SocialController.Error(
                        response.getCode(),
                        response.getMessage()
                ));
            }
        }
    }

    @Data
    @AllArgsConstructor
    public static class Response {
        private int code;
        private String message;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequestProperties {
        private String fileFieldName = "file";

        public RequestProperties setFieldFileName(String fieldFileName) {
            this.fileFieldName = fieldFileName;
            return this;
        }
    }
}