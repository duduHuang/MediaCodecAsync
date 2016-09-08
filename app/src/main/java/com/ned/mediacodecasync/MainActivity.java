package com.ned.mediacodecasync;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;

/**
 * Created by NedHuang on 2016/9/6.
 */
public class MainActivity extends AppCompatActivity {

    private final static String TAG = "MainActivity";
    private MainActivity instance = this;
    private FragmentManager mFm = instance.getFragmentManager();
    private FragmentMainView mFragmentMainView = new FragmentMainView();

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activitylayout);
        mFm.beginTransaction().replace(R.id.fragment_used, mFragmentMainView, TAG).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).commit();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
