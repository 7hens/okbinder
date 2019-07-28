# OkBinder

[![Download](https://api.bintray.com/packages/7hens/maven/okbinder/images/download.svg)](https://bintray.com/7hens/maven/okbinder/_latestVersion)
![Travis (.org)](https://img.shields.io/travis/7hens/okbinder)
[![license](https://img.shields.io/github/license/7hens/okbinder.svg)](https://github.com/7hens/okbinder/blob/master/LICENSE)
[![stars](https://img.shields.io/github/stars/7hens/okbinder.svg?style=social)](https://github.com/7hens/okbinder)

[OkBinder](https://github.com/7hens/okbinder/blob/master/okbinder/src/main/java/cn/thens/okbinder/OkBinder.kt) is an alternative to AIDL. 
OkBinder is very lightweight, with only one class, 100+ lines of code.
With OkBinder you can find errors in IPC earlier.

*OkBinder 是一个 AIDL 的替代方案。
OkBinder 非常轻量级，只有一个类，100+行代码。
使用 OkBinder 你可以更早的发现 IPC 中的错误。*

## Setting up the dependency

last_version: [![Download](https://api.bintray.com/packages/7hens/maven/okbinder/images/download.svg)](https://bintray.com/7hens/maven/okbinder/_latestVersion)

```groovy
implementation 'cn.thens:okbinder:<last_version>'
```

## Sample usage

Define a remote interface with @OkBinder.Interface annotation.

*使用注解 @OkBinder.Interface 修饰远程服务接口。*

```kotlin
@OkBinder.Interface
interface IRemoteService {
    fun doSomething(aInt: Int, aLong: Long, aString: String)
}
```

On the server side, instantiate OkBinder using the remote interface above.

*在服务端，使用上面的远程接口创建 OkBinder 的实例。*

```kotlin
class MyService: Service() {
    private val okBinder = OkBinder(object: IRemoteService {
        override fun doSomething(aInt: Int, aLong: Long, aString: String) {
            // pass
        }
    })
    
    override fun onBind(intent: Intent?): IBinder? {
        return okBinder
    }
}
```

On the client side, create a proxy for the remote interface.

*在客户端，创建一个远程接口的代理。*

```kotlin
class MyActivity: Activity(), ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val remoteService = OkBinder.proxy(service!!, IRemoteService::class.java)
        remoteService.doSomething(0, 0L, "")      
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindService(Intent(this, MyService::class.java), this, Context.BIND_AUTO_CREATE)
    }
}
```

If you want to learn more about the usage of OkBinder, please refer to [OkBinderSample](https://github.com/7hens/okbinder/blob/master/sample/src/main/java/cn/thens/okbinder/sample/OkBinderSample.kt).

*如果你想跟深入了解 OkBinder 的用法，请参考 OkBinderSample.*
