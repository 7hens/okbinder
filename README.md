# OkBinder

[![jitpack](https://jitpack.io/v/7hens/okbinder.svg)](https://jitpack.io/#7hens/okbinder)
![travis](https://img.shields.io/travis/7hens/okbinder)
[![license](https://img.shields.io/github/license/7hens/okbinder.svg)](https://github.com/7hens/okbinder/blob/master/LICENSE)
[![stars](https://img.shields.io/github/stars/7hens/okbinder.svg?style=social)](https://github.com/7hens/okbinder)

OkBinder 是一个轻量级的跨进程通信方案，可以用来替代 AIDL。

_OkBinder is a lightweight IPC library that can be used to replace AIDL._

## Features

| 特点               | AIDL                       | OkBinder                     |
| ------------------ | -------------------------- | ---------------------------- |
| 实现方式           | ✓ AIDL 接口                | ✓ 纯 Java/Kotlin 接口        |
| 获取方法的返回值   | ✓ 支持                     | ✓ 支持                       |
| 非阻塞式调用       | ✓ 使用 oneway              | ✓ 返回值类型使用 void        |
| 通过参数传值       | ✓ 使用 in                  | ✓ 默认支持                   |
| 通过参数取值       | ✓ 使用 out                 | ✕ _不支持_                   |
| 通过参数传值并取值 | ✓ 使用 inout               | ✕ _不支持_                   |
| 通过参数回调       | ✓ 支持                     | ✓ 支持                       |
| 打乱原有方法顺序   | ✕ _不支持_                 | ✓ 支持                       |
| 异常日志           | ✕ _不完整_                 | ✓ 完整                       |
| IDE 智能提示       | ✕ _较少（需手动 import）_  | ✓ 完整（因为是纯 Java 代码） |
| 重构代价           | ✕ _麻烦（需要重新 build）_ | ✓ 简单（无需 build）         |

> OkBinder 从 2.0 开始支持编译时注解，优化了远程方法的调用。

## Setting up the dependency

```groovy
// maven { url "https://jitpack.io" }
implementation("com.github.7hens.okbinder:okbinder2:2.0")

// The following is optional
annotationProcessor("com.github.7hens.okbinder:okbinder2-compiler:2.0")
```

## Sample usage

首先，你需要定义一个用于 IPC 的服务接口。这个接口的功能类似于 AIDL，但不同的是，这是一个纯 Java 的接口，并且需要使用 `@AIDL` 注解。

_First, you need to define a service interface for IPC. Similar to AIDL, but the difference is that this is a pure Java interface and needs to be annotated with `@AIDL`._

```java
@AIDL
public interface IRemoteService {
    void doSomething(int aInt, IRemoteService callback);
}
```

其次，在服务端，你需要实现上面的服务接口，并用它来创建一个 Binder 供客户端调用。

_Second, on the server side, you need to implement the above service interface and use it to create a Binder to provide to the client._

```java
public class MyService extends Service {
    private Binder okBinder = OkBinder.create(new IRemoteService() {
        @Override
        public void doSomething(int aInt, IRemoteService callback) {
            // pass
        }
    })

    @Override
    public IBinder onBind(Intent intent) {
        return okBinder;
    }
}
```

最后，你可以使用客户端的 IBinder 创建出一个服务接口的代理对象。使用这个代理，就可以进行跨进程通信了。

_Finally, you can use the client's IBinder to create a proxy object for the service interface. Use this proxy to communicate across processes._

```java
public class MyActivity extends Activity implements ServiceConnection {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        IRemoteService remoteService = OkBinder.proxy(IRemoteService.class, service);
        remoteService.doSomething(0, remoteService);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindService(new Intent(this, MyService.java), this, Context.BIND_AUTO_CREATE);
    }
}
```
