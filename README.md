#### 最新版本

模块|Location
---|---
最新版本|[![Download](https://jitpack.io/v/like5188/Location.svg)](https://jitpack.io/#like5188/Location)

## 功能介绍

1、百度地图相关工具，只是使用了百度地图的基础定位功能。引用版本：BaiduLoc_AndroidSDK_v7.1

## 使用方法：

1、引用

在Project的gradle中加入：
```groovy
    allprojects {
        repositories {
            ...
            maven { url 'https://jitpack.io' }
        }
    }
```
在Module的gradle中加入：
```groovy
    dependencies {
        compile 'com.github.like5188:Location:版本号'
    }
```

2、在AndroidManifest.xml文件的application标签内添加
```java
    <meta-data
        android:name="com.baidu.lbsapi.API_KEY"
        android:value="你应用的apiKey" />  
```

3、使用
```java
    在Application中
    // 在使用 SDK 各组间之前初始化 context 信息，传入 ApplicationContext
    SDKInitializer.initialize(this)

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
```

4、Proguard
```java
    -dontwarn com.tencent.smtt.**
```
