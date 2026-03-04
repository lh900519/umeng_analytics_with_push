package com.lh.umeng_analytics_with_push

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.umeng.analytics.MobclickAgent
import com.umeng.commonsdk.UMConfigure
import com.umeng.commonsdk.statistics.common.DeviceConfig
import com.umeng.message.PushAgent
import com.umeng.message.UmengMessageHandler
import com.umeng.message.UmengNotificationClickHandler
import com.umeng.message.api.UPushRegisterCallback
import com.umeng.message.entity.UMessage
import com.ut.device.UTDevice
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.embedding.engine.plugins.lifecycle.HiddenLifecycleReference
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import org.json.JSONObject


/** UmengAnalyticsWithPushPlugin */
class UmengAnalyticsWithPushPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {

    companion object {
        private const val LOG = true
        private const val TAG = "UmengAWithPPlugin"
    }

    private lateinit var context: Context

    private lateinit var channel: MethodChannel
    private lateinit var handler: Handler

    /**
     * 用于 Flutter 页面统计
     */
    private var currentPage: String? = null

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        if (LOG) Log.d(TAG, "onAttachedToEngine: ")
        context = flutterPluginBinding.applicationContext
        handler = Handler(Looper.getMainLooper())

        channel = MethodChannel(
            flutterPluginBinding.binaryMessenger,
            "com.lh.umeng_analytics_with_push"
        )
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: Result) {

        if (LOG) Log.i(
            TAG,
            "onMethodCall: ${call.method} ${call.arguments} on ${Thread.currentThread().name}"
        )
        when (call.method) {
            "initialize" -> {

                val deviceType = call.argument<String>("device-type")
                val cue = call.argument<Boolean>("catch-uncaught-exceptions")
                val nof = call.argument<Boolean>("notification-on-foreground")

                UmengAnalyticsWithPush.initialize(context, deviceType = deviceType?.run {
                    return@run if (this == "box") {
                        UMConfigure.DEVICE_TYPE_BOX
                    } else {
                        UMConfigure.DEVICE_TYPE_PHONE
                    }
                }, catchUncaughtExceptions = cue, notificationOnForeground = nof)

                PushAgent.getInstance(context).messageHandler = object : UmengMessageHandler() {
                    override fun dealWithNotificationMessage(c: Context?, msg: UMessage?) {
                        Log.d(TAG, "收到消息 ${msg?.extra}")


                        Handler(Looper.getMainLooper()).post {
                            channel.invokeMethod("onMessage", msg?.extra)
                        }

                        super.dealWithNotificationMessage(c, msg)
                    }
                }

                PushAgent.getInstance(context).notificationClickHandler =
                    object : UmengNotificationClickHandler() {
                        override fun handleMessage(c: Context?, msg: UMessage?) {
                            Log.d(TAG, "点击消息 ${msg?.extra}")

                            Handler(Looper.getMainLooper()).post {
                                channel.invokeMethod("onResume", msg?.extra)
                            }
                            super.handleMessage(c, msg)
                        }
                    }

                result.success(null)
            }
            "getTestDeviceInfo" -> {
                result.success(
                    mapOf(
                        "device_id" to DeviceConfig.getDeviceIdForGeneral(context),
                        "mac" to DeviceConfig.getMac(context),
                    )
                )
            }
            "putPushAlias" -> {

                val aliasType = call.argument<String>("alias-type")!!
                val aliasVal = call.argument<String>("alias-value")!!

                PushAgent.getInstance(context).setAlias(aliasVal, aliasType) { isSuccess, message ->
                    if (LOG) Log.d(TAG, "setAlias: <$aliasType : $aliasVal> $isSuccess, $message")
                    handler.post {
                        if (isSuccess) {
                            result.success(null);
                        } else {
                            result.error("PutAliasError", message ?: "setAlias fail", null)
                        }
                    }
                }
            }
            "addPushAlias" -> {

                val agent = PushAgent.getInstance(context)
                val aliasType = call.argument<String>("alias-type")
                val aliasVal = call.argument<String>("alias-value")

                agent.addAlias(aliasVal, aliasType) { successful, message ->
                    if (LOG) Log.d(TAG, "addAlias: <$aliasType : $aliasVal> $successful, $message")

                    handler.post {
                        if (successful) {
                            result.success(null);
                        } else {
                            result.error("AddAliasError", message ?: "setAlias fail", null)
                        }
                    }
                }
            }
            "removePushAlias" -> {

                val agent = PushAgent.getInstance(context)
                val aliasType = call.argument<String>("alias-type")
                val aliasVal = call.argument<String>("alias-value")
                agent.deleteAlias(aliasVal, aliasType) { successful, message ->
                    handler.post {
                        if (LOG) Log.d(
                            TAG,
                            "deleteAlias: <$aliasType : $aliasVal> $successful, $message"
                        )
                        if (successful) {
                            result.success(null);
                        } else {
                            result.error("RemoveAliasError", message ?: "setAlias fail", null)
                        }
                    }
                }
            }
            "addPushTags" -> {

                val tags = (call.arguments as List<*>)
                val array = Array(tags.size) { i -> tags[i] as String }

                val agent = PushAgent.getInstance(context)
                agent.tagManager.addTags({ isSuccess, ret ->
                    if (LOG) Log.d(TAG, "addTags: $isSuccess, $ret")

                    handler.post {
                        if (isSuccess) {
                            result.success(null)
                        } else {
                            result.error("AddTagsError", ret.errors, "")
                        }
                    }
                }, *array)
            }
            "getPushTags" -> {
                val agent = PushAgent.getInstance(context)
                agent.tagManager.getTags { isSuccess, tags ->

                    if (LOG) Log.d(TAG, "getTags: $isSuccess, $tags")
                    handler.post {
                        if (isSuccess) {
                            result.success(tags)
                        } else {
                            result.error("GetTagsError", "Get tags fail", null)
                        }
                    }
                }
            }
            "removePushTags" -> {
                val tags = (call.arguments as List<*>)
                val array = Array(tags.size) { i -> tags[i] as String }

                val agent = PushAgent.getInstance(context)
                agent.tagManager.deleteTags({ isSuccess, ret ->

                    handler.post {
                        if (LOG) Log.d(TAG, "deleteTags: $isSuccess, $ret")
                        if (isSuccess) {
                            result.success(null)
                        } else {
                            result.error("RemoveTagsError", ret.errors, "")
                        }
                    }
                }, *array)
            }
            "deviceToken" -> {
                UmengAnalyticsWithPush.getDeviceToken(context, object : UPushRegisterCallback {
                    override fun onSuccess(deviceToken: String) {

                        handler.post { result.success(deviceToken) }
                    }

                    override fun onFailure(s: String?, s1: String?) {

                        handler.post { result.error("DeviceTokenError", s ?: "Push register fail", s1) }
                    }
                })
            }
            "oaid" -> {
                // Log.i(TAG, "oaid: ")
                UMConfigure.getOaid(context) { oaid ->
                    if (LOG) Log.d(TAG, "getOaid: $oaid")
                    handler.post { result.success(oaid) }
                }
            }
            "utdid" -> {
                // Log.i(TAG, "utdid: ")
                result.success(UTDevice.getUtdid(context))
            }
            "onPageStart" -> {
                val name = call.arguments as String
                MobclickAgent.onPageStart(name)
                result.success(null)
            }
            "onPageEnd" -> {
                val name = call.arguments as String
                MobclickAgent.onPageEnd(name)
                result.success(null)
            }
            "onPageChanged" -> {
                // 这个方法为了统计 Flutter 页面
                val newPage = call.arguments as String?

                if (newPage != this.currentPage) {
                    var log = ""
                    this.currentPage?.apply {
                        MobclickAgent.onPageEnd(this)
                        log += "Pop $this. "
                    }
                    newPage?.apply {
                        MobclickAgent.onPageStart(this)
                        log += "Push $this. "
                    }

                    if (LOG) Log.d(TAG, "onPageChanged: $log")
                    this.currentPage = newPage
                }
                result.success(null)
            }
            "onEvent" -> {
                val event = call.argument<String>("event")!!
                val params = call.argument<Any>("params")
                if (params is Map<*, *>) {
                    val counter = call.argument<Int>("counter")
                    val attrs = params.entries.fold(mutableMapOf<String, String?>()) { p, e ->
                        p.apply { this[e.key as String] = e.value?.toString() }
                    }
                    if (counter == null) {
                        MobclickAgent.onEvent(context, event, attrs)
                    } else {
                        MobclickAgent.onEventValue(context, event, attrs, counter)
                    }
                } else if (params is String) {
                    MobclickAgent.onEvent(context, event, params)
                } else {
                    MobclickAgent.onEvent(context, event)
                }
                result.success(null)
            }
            "onProfileSignIn" -> {
                val id = call.argument<String>("id")
                val provider = call.argument<String>("provider")
                MobclickAgent.onProfileSignIn(provider, id)
                result.success(null)
            }
            "onProfileSignOut" -> {
                MobclickAgent.onProfileSignOff()
                result.success(null)
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        // handler.removeCallbacksAndMessages(null)
        channel.setMethodCallHandler(null)
        if (LOG) Log.d(TAG, "onDetachedFromEngine: ")
    }

    private var activity: Activity? = null
    private var lifecycle: Lifecycle? = null
    private val onLifecycleChanged = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_CREATE -> {
                PushAgent.getInstance(context).onAppStart()
            }

            Lifecycle.Event.ON_RESUME -> {
                MobclickAgent.onResume(activity)
            }

            Lifecycle.Event.ON_PAUSE -> {
                MobclickAgent.onPause(activity)
            }

            else -> {}
        }
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity

        // 冷启动
        handleIntent(activity?.intent)
        // // 已启动再次唤起
        // binding.addOnNewIntentListener { intent ->
        //     handleIntent(intent, false)
        //     true
        // }

        // lifecycle = binding.lifecycle as Lifecycle?
        val reference = binding.lifecycle as HiddenLifecycleReference
        lifecycle = reference.lifecycle
        lifecycle!!.addObserver(onLifecycleChanged)

        if (LOG) Log.d(TAG, "onAttachedToActivity: ${lifecycle?.currentState}")
    }

    private fun handleIntent(intent: Intent?) {
        // 获取启动 Intent
        val intent = intent ?: return
        val extras = intent.extras      // 其他参数

        val map = mutableMapOf<String, Any?>()
        extras?.keySet()?.forEach { key ->
            map[key] = extras.getString(key)
        }

        Handler(Looper.getMainLooper()).post {
            try {
                // val method = if (isLaunch) "onLaunch" else "onResume"
                val method =  "onLaunch"
                channel.invokeMethod(method, map)
                if (LOG) Log.d(TAG, "invokeMethod: $method, $map")
            } catch (e: Exception) {
                Log.e(TAG, "invokeMethod: $e")
            }
        }

    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity()
    }

    override fun onDetachedFromActivity() {
        var log = ""
        currentPage?.apply {
            MobclickAgent.onPageEnd(this)
            log += "Pop $this"
        }
        lifecycle?.removeObserver(onLifecycleChanged)
        lifecycle = null
        activity = null
        if (LOG) Log.d(TAG, "onDetachedFromActivity: $log")
    }
}
