package com.cylan.jiafeigou.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.cylan.utils.MD5Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by yzd on 16-11-30.
 */

public class GlideNetVideoUtils {

    public static void loadNetVideo(final Context context, final String url, final ImageView imageView) {
        //网络视频加载

        Observable.create(new Observable.OnSubscribe<File>() {
            @Override
            public void call(Subscriber<? super File> subscriber) {
                try {
                    final File file = new File(context.getCacheDir(), MD5Util.lowerCaseMD5(url));
                    if (!file.exists()) {
                        createVideoThumbnail(url).compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(file));
                    }
                    subscriber.onNext(file);
                    subscriber.onCompleted();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<File>() {
                    @Override
                    public void call(File file) {
                        Glide.with(context)
                                .load(file)
                                .into(imageView);
                    }
                });
    }

    public static Bitmap createVideoThumbnail(String filePath) {
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(filePath, new HashMap<String, String>());
            bitmap = retriever.getFrameAtTime(0);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException ex) {
                ex.printStackTrace();
            }
        }

        if (bitmap == null) return null;

        // Scale down the bitmap if it's too large.
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int max = Math.max(width, height);
        if (max > 512) {
            float scale = 512f / max;
            int w = Math.round(scale * width);
            int h = Math.round(scale * height);
            bitmap = Bitmap.createScaledBitmap(bitmap, w, h, true);
        }
        return bitmap;
    }
}