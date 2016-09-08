package com.ned.mediacodecasync;

import android.annotation.TargetApi;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by NedHuang on 2016/9/6.
 */
public class MediaCodecWork {

    private final static String TAG = "MediaCodecWork";
    private final static int TIMEOUT_USED = 1000, TIME_SLEEP = 10;

    private MediaExtractor mExtractor = null;
    private StreamThread mStreamThread = null;
    private boolean mIsLoop = false;
    // Video codec
    private MediaCodec.BufferInfo mInfoVideo = null;
    private MediaCodec mDecoderVideo = null;
    private int mTrackIndexVideo;
    private MediaFormat mFormatVideo = null;
    private String mMimeVideo = null;

    // Audio codec
    private MediaCodec.BufferInfo mInfoAudio = null;
    private AudioTrack mPlayAudioTrack = null;
    private MediaCodec mDecoderAudio = null;
    private int mTrackIndexAudio;
    private MediaFormat mFormatAudio = null;
    private String mMimeAudio = null;

    protected void initial() {
        release();
        mExtractor = new MediaExtractor();
        mInfoVideo = new MediaCodec.BufferInfo();
        mInfoAudio = new MediaCodec.BufferInfo();
    }

    protected void setDataSource(String path) {
        try {
            mExtractor.setDataSource(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void createExtractor(Surface surface) {
        // Video decoder
        createVideoDecoder(surface);
        // Audio decoder
        createAudioDecoder();
    }

    protected void seekTo(long t) {
        mStreamThread.mIsSeek = true;
        mStreamThread.mSeekTimeUs = t;
        mStreamThread.mIsInitTime = true;
    }

    protected void start() {
        if (mStreamThread == null) {
            mStreamThread = new StreamThread();
            mStreamThread.start();
        }
        if (mStreamThread.mIsPause) {
            mStreamThread.mIsPause = false;
        } else {
            mStreamThread.mIsPause = true;
        }
    }

    protected void stop() {
        mStreamThread.interrupt();
        try {
            mStreamThread.join();
        } catch (InterruptedException e) {
            Log.d(TAG, "stop: Kill thread fail.");
        }
        mStreamThread = null;
    }

    protected void setLooping(boolean b) {
        mIsLoop = b;
    }

    protected long getDuration() {
        return mFormatVideo.getLong(MediaFormat.KEY_DURATION);
    }

    protected long getPresentation() {
        return mInfoVideo.presentationTimeUs;
    }

    protected void release() {
        if (mDecoderVideo != null) {
            mDecoderVideo.stop();
            mDecoderVideo.release();
            mDecoderVideo = null;
        }
        if (mDecoderAudio != null) {
            mDecoderAudio.stop();
            mDecoderAudio.release();
            mDecoderAudio = null;
        }
        if (mExtractor != null) {
            mExtractor.release();
            mExtractor = null;
        }
        if (mPlayAudioTrack != null) {
            mPlayAudioTrack.stop();
            mPlayAudioTrack.release();
            mPlayAudioTrack = null;
        }
        mFormatAudio = null;
        mFormatAudio = null;
        mMimeVideo = null;
        mMimeAudio = null;
        mInfoVideo = null;
        mInfoAudio = null;
    }

    private void createVideoDecoder(Surface surface) {
        mTrackIndexVideo = selectTrackVideo();
        mExtractor.selectTrack(mTrackIndexVideo);
        mFormatVideo = mExtractor.getTrackFormat(mTrackIndexVideo);
        mMimeVideo = mFormatVideo.getString(MediaFormat.KEY_MIME);
        try {
            mDecoderVideo = MediaCodec.createDecoderByType(mMimeVideo);
        } catch (IOException e) {
            Log.d(TAG, "createVideoDecoder: Create decoder fail.");
        }
        mDecoderVideo.configure(mFormatVideo, surface, null, 0);
        mDecoderVideo.start();
    }

    private int selectTrackVideo() {
        // Select the first video track we find, ignore the rest.
        int numTracks = mExtractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = mExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                return i;
            }
        }
        return -1;
    }

    private void createAudioDecoder() {
        mTrackIndexAudio = selectTrackAudio();
        int minBufferSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_CONFIGURATION_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        int bufferSize = 4 * minBufferSize;
        mExtractor.selectTrack(mTrackIndexAudio);
        mFormatAudio = mExtractor.getTrackFormat(mTrackIndexAudio);
        mMimeAudio = mFormatAudio.getString(MediaFormat.KEY_MIME);
        try {
            mDecoderAudio = MediaCodec.createDecoderByType(mMimeAudio);
        } catch (IOException e) {
            Log.d(TAG, "createAudioDecoder: Create decoder fail.");
        }
        mDecoderAudio.configure(mFormatAudio, null, null, 0);
        mPlayAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, mFormatAudio.getInteger(MediaFormat.KEY_SAMPLE_RATE), AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
        mPlayAudioTrack.play();
        mDecoderAudio.start();
    }

    private int selectTrackAudio() {
        // Select the first video track we find, ignore the rest.
        int numTracks = mExtractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = mExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")) {
                return i;
            }
        }
        return -1;
    }

    private class StreamThread extends Thread {

        private boolean mIsPause = true, mIsSeek = false, mIsInitTime = true;
        private long mSystemStartMs, mVideoStartUs, mSeekTimeUs;
        @Override
        public void run() {
            do {
                while (!Thread.interrupted()) {
                    try {
                        if (mIsSeek) {
                            mDecoderVideo.flush();
                            mDecoderAudio.flush();
                            mExtractor.seekTo(mSeekTimeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                            mIsSeek = false;
                        }
                        while (mIsPause) {
                            if (mIsSeek) {
                                mDecoderVideo.flush();
                                mDecoderAudio.flush();
                                mExtractor.seekTo(mSeekTimeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                                mIsSeek = false;
                            }
                            Thread.sleep(TIME_SLEEP);
                        }
                        int trackIndex = mExtractor.getSampleTrackIndex();
                        if (trackIndex != -1) {
                            fillBuffer(trackIndex);
                            emptyBuffer(trackIndex);
                        } else {
                            break;
                        }
                    } catch (InterruptedException e) {
                        seekTo(0);
                        return;
                    }
                }
                seekTo(0);
            } while (mIsLoop);
        }

        private void fillBuffer(int trackIndex) {
            if (trackIndex == mTrackIndexVideo) {
                fillVideoBuffer();
            } else if (trackIndex == mTrackIndexAudio) {
                fillAudioBuffer();
            } else {
                Log.d(TAG, "fillBuffer: The track index is neither video nor audio.");
            }
        }

        private void emptyBuffer(int trackIndex) throws InterruptedException {
            if (trackIndex == mTrackIndexVideo) {
                emptyVideoBuffer();
            } else if (trackIndex == mTrackIndexAudio) {
                emptyAudioBuffer();
            } else {
                Log.d(TAG, "emptyBuffer: The track index is neither video nor audio.");
            }
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        private void fillVideoBuffer() {
//            ByteBuffer[] decoderBuffer = mDecoderVideo.getInputBuffers();
            int videoBufIndex = mDecoderVideo.dequeueInputBuffer(TIMEOUT_USED);
            if (videoBufIndex >= 0) {
//                ByteBuffer videoBuf = decoderBuffer[videoBufIndex];
                ByteBuffer videoBuf = mDecoderVideo.getInputBuffer(videoBufIndex);
                videoBuf.clear();
                int videoChunkSize = mExtractor.readSampleData(videoBuf, 0);
                if (videoChunkSize < 0) {
                    mDecoderVideo.queueInputBuffer(videoBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                } else {
                    mDecoderVideo.queueInputBuffer(videoBufIndex, 0, videoChunkSize, mExtractor.getSampleTime(), 0);
                    mExtractor.advance();
                    videoBuf.clear();
                }
            }
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        private void fillAudioBuffer() {
//            ByteBuffer[] decoderBuffer = mDecoderAudio.getInputBuffers();
            int audioBufIndex = mDecoderAudio.dequeueInputBuffer(TIMEOUT_USED);
            if (audioBufIndex >= 0) {
//                ByteBuffer audioBuf = decoderBuffer[audioBufIndex];
                ByteBuffer audioBuf = mDecoderAudio.getInputBuffer(audioBufIndex);
                audioBuf.clear();
                int audioChunkSize = mExtractor.readSampleData(audioBuf, 0);
                if (audioChunkSize < 0) {
                    mDecoderAudio.queueInputBuffer(audioBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                } else {
                    mDecoderAudio.queueInputBuffer(audioBufIndex, 0, audioChunkSize, mExtractor.getSampleTime(), 0);
                    mExtractor.advance();
                    audioBuf.clear();
                }
            }
        }

        private void emptyVideoBuffer() throws InterruptedException {
            int decoderStatus = mDecoderVideo.dequeueOutputBuffer(mInfoVideo, TIMEOUT_USED);
            if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
            } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            } else if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
            } else if (decoderStatus < 0) {
            } else {
                if ((mInfoVideo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {

                }
                if (decoderStatus >= 0) {
                    mDecoderVideo.releaseOutputBuffer(decoderStatus, true);
                    if (mIsInitTime) {
                        mSystemStartMs = System.currentTimeMillis();
                        mVideoStartUs = mInfoVideo.presentationTimeUs;
                        mIsInitTime = false;
                    }
                    while ((mInfoVideo.presentationTimeUs - mVideoStartUs) / 1000 > System.currentTimeMillis() - mSystemStartMs) {
                        Thread.sleep(TIME_SLEEP);
                    }
                }
            }
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        private void emptyAudioBuffer() {
//            ByteBuffer[] decoderBuffer = mDecoderAudio.getOutputBuffers();
            int decoderStatus = mDecoderAudio.dequeueOutputBuffer(mInfoAudio, TIMEOUT_USED);
            if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
            } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
//                decoderBuffer = mDecoderAudio.getInputBuffers();
            } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                mPlayAudioTrack.setPlaybackRate(mFormatAudio.getInteger(MediaFormat.KEY_SAMPLE_RATE));
            } else if (decoderStatus < 0) {
            } else {
                if ((mInfoAudio.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {

                }
                if (decoderStatus >= 0) {
//                    ByteBuffer buffer = decoderBuffer[decoderStatus];
                    ByteBuffer buffer = mDecoderAudio.getOutputBuffer(decoderStatus);
                    byte[] chunk = new byte[mInfoAudio.size];
                    buffer.get(chunk);
                    mPlayAudioTrack.write(chunk, 0, chunk.length);
                    mDecoderAudio.releaseOutputBuffer(decoderStatus, false);
                }
            }
        }
    }
}
