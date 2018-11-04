package com.mairon.socialposter.data;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ImageStorage {

    private static Map<String, Bitmap> saved = new HashMap<>();

    public interface DownloadListener {
        void onSuccess(Bitmap image);
        void onError();
    }

    public static void get(String url, DownloadListener downloadListener) {
        Bitmap result = findFromSaved(url);
        if (result != null) {
            downloadListener.onSuccess(result);
        } else {
            download(url, downloadListener);
        }
    }

    public static void download(final String url, final DownloadListener downloadListener) {
        new DownloadTask(url, new DownloadListener() {
            @Override
            public void onSuccess(Bitmap image) {
                saved.put(url, image);
                downloadListener.onSuccess(image);
            }

            @Override
            public void onError() {
                downloadListener.onError();
            }
        }).execute();
    }

    public static Bitmap findFromSaved(String url) {
        for (String key : saved.keySet()) {
            if (key.equals(url)) {
                return saved.get(key);
            }
        }
        return null;
    }

    private static class DownloadTask extends AsyncTask<String, Integer, String> {

        private String url;
        private DownloadListener downloadListener;
        private Bitmap result;

        DownloadTask(String url, DownloadListener downloadListener) {
            this.url = url;
            this.downloadListener = downloadListener;
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected String doInBackground(String... strings) {
            result = downloadBitmap(url);
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            if (result == null) {
                downloadListener.onError();
            } else {
                downloadListener.onSuccess(result);
            }
        }
    }

    private static Bitmap downloadBitmap(String url) {
        Bitmap image = null;

        // initilize the default HTTP client object
        final DefaultHttpClient client = new DefaultHttpClient();

        //forming a HttpGet request
        final HttpGet getRequest = new HttpGet(url);
        try {

            HttpResponse response = client.execute(getRequest);

            //check 200 OK for success
            final int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode != HttpStatus.SC_OK) {
                Log.w("ImageDownloader", "Error " + statusCode +
                                         " while retrieving bitmap from " + url);
                return null;

            }

            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream inputStream = null;
                try {
                    // getting contents from the stream
                    inputStream = entity.getContent();

                    // decoding stream data back into image Bitmap that android understands
                    image = BitmapFactory.decodeStream(inputStream);


                } finally {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    entity.consumeContent();
                }
            }
        } catch (Exception e) {
            // You Could provide a more explicit error message for IOException
            getRequest.abort();
            Log.e("ImageDownloader", "Something went wrong while" +
                                     " retrieving bitmap from " + url + e.toString());
        }

        return image;
    }
}
