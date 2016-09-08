package com.ned.mediacodecasync;

import android.view.Surface;

/**
 * Created by NedHuang on 2016/9/6.
 */
public class MediaCodecPlayer {

    private final static String TAG = "MediaCodecPlayer";
    private MediaCodecWork mMediaCodecWork = null;
    private Surface mSurface = null;

    public MediaCodecPlayer() {
        mMediaCodecWork = new MediaCodecWork();
    }

    public void start() {
        mMediaCodecWork.start();
    }

    public void stop() {
        mMediaCodecWork.stop();
    }

    public void setDataSource(String path) {
        mMediaCodecWork.setDataSource(path);
    }

    public void seekTo(long t) {
        mMediaCodecWork.seekTo(t);
    }

    public void setSurface(Surface surface) {
        mSurface = surface;
    }

    public void pause() {

    }

    public void initial() {
        mMediaCodecWork.initial();
    }

    public void prepare() {
        mMediaCodecWork.createExtractor(mSurface);
    }

    public void release() {
        mMediaCodecWork.release();
        mMediaCodecWork = null;
    }

    public void setLooping(boolean b) {
        mMediaCodecWork.setLooping(b);
    }

    public long getDuration() {
        return mMediaCodecWork.getDuration();
    }

    public long getPresentation() {
        return mMediaCodecWork.getPresentation();
    }
}
