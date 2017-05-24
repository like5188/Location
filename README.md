# Location

百度地图相关工具，只是使用了百度地图的基础定位功能。引用版本：BaiduLoc_AndroidSDK_v7.1

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
        compile 'com.github.like5188:Location:1.0.0'
    }
```
2、使用
```java
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
3、引用的库
```java
    compile rootProject.ext.deps.Logger
```
# License
```xml
    Copyright 2017 like5188
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
    http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.