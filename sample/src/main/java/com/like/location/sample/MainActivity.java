package com.like.location.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.baidu.location.BDLocation;
import com.like.location.LocationUtils;
import com.like.location.MyLocationListener;
import com.like.logger.Logger;

public class MainActivity extends AppCompatActivity {
    private LocationUtils mLocationUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initLocation();
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
        mLocationUtils.start();
    }
}
