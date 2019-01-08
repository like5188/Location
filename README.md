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

4、定位
```java
    LocationUtils.getInstance(this).addListener()
    LocationUtils.getInstance(this).start()
    LocationUtils.getInstance(this).stop()
    LocationUtils.getInstance(this).restart()
```

5、导航。自动选择高德、百度进行导航。
```java
    NavigationUtils.navigation(this, 29.0, 106.0)
```

6、好友位置实时共享
```java
    在Application中
    // 在使用 SDK 各组间之前初始化 context 信息，传入 ApplicationContext
    SDKInitializer.initialize(this)

    SharedLocationUtils
```

7、Proguard
```java
```
