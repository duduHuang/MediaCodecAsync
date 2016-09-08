package com.ned.mediacodecasync;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by NedHuang on 2016/9/6.
 */
public class FragmentMainView extends Fragment {

    private final static String TAG = "FragmentMainView";
    private View mVf;
    private ViewGroup mVg;
    private FragmentMainView instance = this;

    private final static int sSurViewNumber = 4;
    private Button mBtnExit, mBtnPlay, mBtnStop;
    private SurfaceView[] mSurView = new SurfaceView[sSurViewNumber];
    private Surface[] mSurface = new Surface[sSurViewNumber];
    private SeekBar mSeekBar;
    private TextView mTxtPlayTime, mTxtDuration;

    private MediaCodecPlayer mMediaCodecPlayer = null;
    private String mPath = "/mnt/usbdisk/usbdisk4/CH01/01-20160420_083130.mp4";
    private Timer mTimer = null;
    private MyTimerTask mTimerTask = null;
    private static final int UPDATE_TIMER = 0;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setUpViewComponent();
    }

    private void setUpViewComponent() {
        for (int i = 0; i < sSurViewNumber; i++) {
            mSurface[i] = mSurView[i].getHolder().getSurface();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mVg = container;
        mVf = inflater.inflate(R.layout.mainviewlayout, container, false);
        initButtonAndSurfaceView();
        mBtnExit.setOnClickListener(mOnClickListener);
        mBtnPlay.setOnClickListener(mOnClickListener);
        mBtnStop.setOnClickListener(mOnClickListener);
        mSeekBar.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
        return mVf;
    }

    private void initButtonAndSurfaceView() {
        mBtnExit = (Button) mVf.findViewById(R.id.btn_exit);
        mBtnPlay = (Button) mVf.findViewById(R.id.btn_play);
        mBtnStop = (Button) mVf.findViewById(R.id.btn_stop);
        mSeekBar = (SeekBar) mVf.findViewById(R.id.seekbar);
        mSurView[0] = (SurfaceView) mVf.findViewById(R.id.surview1);
        mSurView[1] = (SurfaceView) mVf.findViewById(R.id.surview2);
        mSurView[2] = (SurfaceView) mVf.findViewById(R.id.surview3);
        mSurView[3] = (SurfaceView) mVf.findViewById(R.id.surview4);
        mTxtPlayTime = (TextView) mVf.findViewById(R.id.txt_playtime);
        mTxtDuration = (TextView) mVf.findViewById(R.id.txt_duration);
    }

    private SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (mMediaCodecPlayer != null) {
                mTxtPlayTime.setText(changeTimeFormat((int) mMediaCodecPlayer.getPresentation()));
            } else {
                mTxtPlayTime.setText(getResources().getText(R.string.time_zero));
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (mMediaCodecPlayer != null) {
                mMediaCodecPlayer.seekTo(mSeekBar.getProgress());
            }
        }
    };

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.btn_exit) {
                if (mMediaCodecPlayer != null) {
                    mMediaCodecPlayer.stop();
                    mMediaCodecPlayer.release();
                    mMediaCodecPlayer = null;
                }
            } else if (v.getId() == R.id.btn_play) {
                if (mMediaCodecPlayer == null) {
                    mMediaCodecPlayer = new MediaCodecPlayer();
                    mMediaCodecPlayer.initial();
                    mMediaCodecPlayer.setSurface(mSurface[0]);
                    mMediaCodecPlayer.setDataSource(mPath);
                    mMediaCodecPlayer.prepare();
                    updateTimeBarInfo();
                }
                mMediaCodecPlayer.start();
                if (mBtnPlay.getText().toString().equals(getResources().getText(R.string.play))) {
                    mBtnPlay.setText(getResources().getText(R.string.pause));
                } else {
                    mBtnPlay.setText(getResources().getText(R.string.play));
                }
            } else if (v.getId() == R.id.btn_stop) {
                if (mMediaCodecPlayer != null) {
                    mMediaCodecPlayer.stop();
                    mMediaCodecPlayer.release();
                    mMediaCodecPlayer = null;
                }
                mBtnPlay.setText(getResources().getText(R.string.play));
            }
        }

        private void updateTimeBarInfo() {
            mTimer = new Timer();
            mTimerTask = new MyTimerTask();
            mTimer.schedule(mTimerTask, 50, 50);
            mTxtPlayTime.setText(changeTimeFormat((int) mMediaCodecPlayer.getPresentation()));
            mSeekBar.setMax((int) mMediaCodecPlayer.getDuration());
            mTxtDuration.setText(changeTimeFormat((int) mMediaCodecPlayer.getDuration()));
        }
    };

    private class MyTimerTask extends TimerTask {
        @Override
        public void run() {
            mHandler.sendEmptyMessage(UPDATE_TIMER);
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case UPDATE_TIMER:
                    if (mMediaCodecPlayer != null) {
                        mSeekBar.setProgress((int) mMediaCodecPlayer.getPresentation());
                    } else {
                        mSeekBar.setProgress(0);
                    }
                    break;
            }
        }
    };

    private String changeTimeFormat(int time) {
        String sMinute = "" + (time / 60000000);
        if ((time / 60000000) < 10) {
            sMinute = "0" + sMinute;
        }
        String sSecond = "" + (time % 60000000) / 1000000;
        if ((time % 60000000) / 1000000 < 10) {
            sSecond = "0" + sSecond;
        }
        return sMinute + ":" + sSecond;
    }
}