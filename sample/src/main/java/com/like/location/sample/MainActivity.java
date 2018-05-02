package com.like.location.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.baidu.location.BDLocation;
import com.baidu.trace.api.entity.EntityListResponse;
import com.baidu.trace.api.entity.OnEntityListener;
import com.baidu.trace.api.track.HistoryTrackResponse;
import com.baidu.trace.api.track.OnTrackListener;
import com.like.location.LocationUtils;
import com.like.location.MyLocationListener;
import com.like.location.TraceUtils;
import com.like.logger.Logger;

public class MainActivity extends AppCompatActivity {
    private LocationUtils mLocationUtils;
    private TraceUtils mTraceUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initLocation();
        mTraceUtils = new TraceUtils(this, 164746, 5, 10);
        mTraceUtils.startTrace();
    }

    /**
     * 初始化百度定位
     */
    public void initLocation() {
        mLocationUtils = new LocationUtils(this, new MyLocationListener() {
            @Override
            public void onReceiveLocation(BDLocation location) {
                super.onReceiveLocation(location);
                if (location != null) {
                    Logger.i("Location", location.getCity());
                }
                mLocationUtils.stop();
            }
        });
    }

    public void startLocation(View view) {
        mLocationUtils.start();
    }

    public void stopLocation(View view) {
        mLocationUtils.stop();
    }

    public void startTrack(View view) {
        mTraceUtils.startTrace();
    }

    public void stopTrack(View view) {
        mTraceUtils.stopTrace();
    }

    public void startGather(View view) {
        mTraceUtils.startGather();
    }

    public void stopGather(View view) {
        mTraceUtils.stopGather();
    }

    public void queryEntityList(View view) {
        mTraceUtils.queryEntityList(1, null, new OnEntityListener() {
            @Override
            public void onEntityListCallback(EntityListResponse entityListResponse) {
                Logger.e(entityListResponse);
            }
        });
    }

    public void queryHistoryTrack(View view) {
        mTraceUtils.queryHistoryTrack(1, System.currentTimeMillis() / 1000 - 12 * 60 * 60, System.currentTimeMillis() / 1000, new OnTrackListener() {
            @Override
            public void onHistoryTrackCallback(HistoryTrackResponse historyTrackResponse) {
                Logger.e(historyTrackResponse);
            }
        });
    }
}
