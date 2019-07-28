package cn.thens.okbinder

import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import android.util.Base64
import android.util.Log
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

class OkBinder(private val remoteObject: Any) : Binder() {
    private val remoteMethods = ConcurrentHashMap<String, Method>()
    private val descriptor: String

    init {
        val remoteInterface = (remoteObject.javaClass.interfaces ?: emptyArray())
            .filter { it.isOkBinderInterface() }
            .also { require(it.size == 1) { "remote object must implement only one interface with @OkBinder.Interface annotation" } }
            .first()
        for (method in remoteInterface.declaredMethods) {
            if (method.isBridge) continue
            remoteMethods[getMethodId(method)] = method
        }
        descriptor = remoteInterface.name
        attachInterface({ this }, descriptor)
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        requireNotNull(reply)
        if (code == IBinder.FIRST_CALL_TRANSACTION) {
            try {
                data.enforceInterface(descriptor)
                val remoteMethod = remoteMethods.getValue(data.readString()!!)
                val paramTypes = remoteMethod.parameterTypes
                val result = if (paramTypes.isNullOrEmpty()) {
                    remoteMethod.invoke(remoteObject)
                } else {
                    val classLoader = remoteObject.javaClass.classLoader
                    remoteMethod.invoke(remoteObject, *run {
                        Array(paramTypes.size) { adaptToProxy(data.readValue(classLoader), paramTypes[it]) }
                    })
                }
                reply.writeNoException()
                if (result != null) {
                    reply.writeInt(1)
                    reply.writeValue(adaptToBinder(result, remoteMethod.returnType))
                } else {
                    reply.writeInt(0)
                }
                return true
            } catch (e: Exception) {
                val cause = e.cause.takeIf { e is InvocationTargetException } ?: e
                Log.e("@OkBinder", "from Service", cause)
                throw cause
            }
        }
        return super.onTransact(code, data, reply, flags)
    }

    @Target(AnnotationTarget.CLASS)
    annotation class Interface

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun <T> proxy(binder: IBinder, serviceClass: Class<T>): T {
            require(serviceClass.isOkBinderInterface()) { "class must be an interface with @OkBinder.Interface annotation" }
            if (binder is OkBinder) return binder.remoteObject as T
            val classLoader = serviceClass.classLoader
            return serviceClass.cast(Proxy.newProxyInstance(classLoader, arrayOf(serviceClass)) { _, method, args ->
                if (!binder.isBinderAlive) throw RuntimeException("binder has died")
                val data = Parcel.obtain()
                val reply = Parcel.obtain()
                var result: Any? = null
                try {
                    data.writeInterfaceToken(serviceClass.name)
                    data.writeString(getMethodId(method))
                    args?.forEachIndexed { index, arg ->
                        data.writeValue(adaptToBinder(arg, method.parameterTypes[index]))
                    }
                    val flags = if (method.returnType == Void.TYPE) IBinder.FLAG_ONEWAY else 0
                    binder.transact(IBinder.FIRST_CALL_TRANSACTION, data, reply, flags)
                    reply.readException()
                    if (reply.readInt() != 0) {
                        result = adaptToProxy(reply.readValue(classLoader)!!, method.returnType)
                    }
                } finally {
                    reply.recycle()
                    data.recycle()
                }
                result
            })!!
        }

        private fun adaptToBinder(value: Any?, type: Class<*>): Any? {
            return if (value != null && type.isOkBinderInterface()) OkBinder(value) else value
        }

        private fun adaptToProxy(value: Any?, type: Class<*>): Any? {
            return if (value != null && type.isOkBinderInterface()) proxy(value as IBinder, type) else value
        }

        private fun getMethodId(method: Method): String {
            val params = method.parameterTypes.joinToString(",") { it.name }
            val methodSignature = method.declaringClass.name + "." + method.name + "@" + params
            val md5 = MessageDigest.getInstance("MD5").digest(methodSignature.toByteArray())
            return Base64.encodeToString(md5, Base64.NO_WRAP)
        }

        private fun Class<*>.isOkBinderInterface(): Boolean {
            return isInterface && isAnnotationPresent(Interface::class.java)
        }
    }
}