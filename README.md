#### 最新版本

模块|Location
---|---
最新版本|[![Download](https://jitpack.io/v/like5188/Location.svg)](https://jitpack.io/#like5188/Location)

## 功能介绍

1、百度地图相关工具，封装了基础地图、基础定位、鹰眼轨迹。

2、可以定位、导航、位置共享

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

2、在build.gradle中添加apiKey
```java
    defaultConfig {
        ...
        manifestPlaceholders = [baiduApiKey: "你自己的apiKey"]
    }
```

3、危险权限申请
```java
    Manifest.permission.READ_PHONE_STATE,
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.WRITE_EXTERNAL_STORAGE
```

4、定位。使用LocationUtils工具类。

5、导航。使用NavigationUtils工具类。自动选择高德、百度进行导航。

6、好友位置实时共享。使用SharedLocationUtils工具类。
```java
    LiveDataBus注册
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        liveDataBusRegister(this)
    }

    /*************************接收事件**************************/
    // 点击了marker
    @BusObserver([LocationConstants.TAG_CLICK_MARKER])
    fun onClickMarker(markerInfo: MarkerInfo?) {
        Log.d("ShareLocationActivity", markerInfo.toString())
    }

    // 点击了围栏上的覆盖物
    @BusObserver([LocationConstants.TAG_CLICK_FENCE_OVERLAY])
    fun onClickFenceOverlay(circleFenceInfo: CircleFenceInfo) {
        Log.d("ShareLocationActivity", circleFenceInfo.toString())
    }

    // 走进围栏
    @BusObserver([LocationConstants.TAG_MOVE_IN_FENCE])
    fun onMoveInFence(circleFenceInfo: CircleFenceInfo?) {
        Log.d("ShareLocationActivity", circleFenceInfo.toString())
    }

    // 走出围栏
    @BusObserver([LocationConstants.TAG_MOVE_OUT_FENCE])
    fun onMoveOutFence(circleFenceInfo: CircleFenceInfo?) {
        Log.d("ShareLocationActivity", circleFenceInfo.toString())
    }
```

7、Proguard
```java
    -keep class com.baidu.** { *; }
    -keep class vi.com.gdi.bgl.android.**{*;}
    -keep class mapsdkvi.com.**{*;}
    -dontwarn com.baidu.**
    -dontwarn javax.annotation.**
    -dontwarn org.codehaus.**

    #livedatabus
    -keep class * extends com.like.livedatabus.Bridge
    -keep class com.like.livedatabus_annotations.**{*;}
```
