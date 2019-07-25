package cn.thens.okbinder.sample

import android.app.Activity
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.Parcelable
import android.util.Log
import android.widget.Toast
import cn.thens.okbinder.OkBinder
import kotlinx.android.synthetic.main.activity_main.*

@OkBinder.Interface
interface IRemoteService {
    fun test(): String

    fun testError(aBoolean: Boolean, aParcelable: Parcelable)

    fun testCallback(data: String, callback: ICallback): ICallback
}

@OkBinder.Interface
interface ICallback {
    val data: String
    fun onResult(result: String)
}

private const val TAG = "@OkBinder"

class MainActivity : Activity(), ServiceConnection {
    private var isServiceConnected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        vTestLocalService.setOnClickListener {
            rebindService(LocalService::class.java)
        }
        vTestRemoteService.setOnClickListener {
            rebindService(RemoteService::class.java)
        }
    }

    private fun rebindService(serviceClass: Class<*>) {
        if (isServiceConnected) {
            unbindService(this)
        }
        bindService(Intent(this, serviceClass), this, Context.BIND_AUTO_CREATE)
        isServiceConnected = true
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        Toast.makeText(this, "please check the log", Toast.LENGTH_SHORT).show()
        val remoteService = OkBinder.proxy(service!!, IRemoteService::class.java)
        remoteService.test().let { Log.d(TAG, "remoteService.test => $it") }
        try {
            remoteService.testError(false, ComponentName.unflattenFromString("a.b/.c")!!)
        } catch (e: Exception) {
            Log.e(TAG, "remoteService.testError => \n" + Log.getStackTraceString(e))
        }
        val callback = remoteService.testCallback("Hello, OkBinder :)", object : ICallback {
            override val data: String = "I_CALLBACK_DATA"

            override fun onResult(result: String) {
                Log.d(TAG, ">> ** ICallback.onResult: result = $result ** <<")
            }
        })
        Log.d(TAG, "callback.data... = ${callback.data}")
        callback.onResult("CALLBACK_RESULT ...")
        Log.d(TAG, "end of onServiceConnected")
    }

    override fun onServiceDisconnected(name: ComponentName?) {
    }
}

abstract class BaseService : Service() {
    private val okBinder = OkBinder(object : IRemoteService {
        override fun test(): String {
            Log.d(TAG, ">> ** IRemoteService.test ** <<")
            return "TEST"
        }

        override fun testError(aBoolean: Boolean, aParcelable: Parcelable) {
            Log.d(TAG, ">> ** IRemoteService.testError: aBoolean = $aBoolean ** <<")
            Log.d(TAG, ">> ** IRemoteService.testError: aParcelable = $aParcelable ** <<")
            throw NullPointerException()
        }

        override fun testCallback(data: String, callback: ICallback): ICallback {
            Log.d(TAG, ">> ** IRemoteService.testCallback: data = $data ** <<")
            Log.d(TAG, ">> ** IRemoteService.testCallback: callback.data = ${callback.data} ** <<")
            callback.onResult("CALLBACK_RESULT")
            return callback
        }
    })

    override fun onBind(intent: Intent?): IBinder? {
        return okBinder
    }
}

class LocalService : BaseService()

class RemoteService : BaseService()