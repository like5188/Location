#### 最新版本

模块|Location
---|---
最新版本|[![Download](https://jitpack.io/v/like5188/Location.svg)](https://jitpack.io/#like5188/Location)

## 功能介绍

1、百度地图相关工具，封装了基础地图、基础定位。

2、包含的工具类有：BaiduMapUtils、LocationUtils、MarkerUtils、NavigationUtils

3、导航。使用NavigationUtils工具类。自动选择高德、百度进行导航。

4、已经做了权限、混淆处理。

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
    defaultConfig {
        ...
        manifestPlaceholders = [baiduApiKey: "你自己的apiKey"]
    }

    dependencies {
        implementation 'androidx.activity:activity-ktx:1.2.0-alpha08'
        implementation 'androidx.fragment:fragment-ktx:1.3.0-alpha08'

        implementation 'com.github.like5188:Location:版本号'
    }
```
