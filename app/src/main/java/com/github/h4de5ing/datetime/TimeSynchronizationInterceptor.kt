package com.github.h4de5ing.datetime
import android.os.SystemClock
import android.text.format.DateFormat
import okhttp3.Interceptor
import okhttp3.Response
import java.util.Calendar
import kotlin.math.abs
/**
 * 时间同步拦截器。主要功能为获取响应头中的时间，然后与本地时间对比，相差超过1分钟的则进行同步。
 */
class TimeSynchronizationInterceptor : Interceptor {
    private val oneMinute = 1000 * 60
    override fun intercept(chain: Interceptor.Chain): Response {
        println("拦截器入口")
        val response = chain.proceed(chain.request()) // 这个函数调用是有可能抛出IOException的，所以需要try
        println("拦截器得到响应")
        val webServerDate = response.headers.getDate("Date")
        val is1970 = Calendar.getInstance().get(Calendar.YEAR) == 1970
        // 如果当前时间为1970年或者当前时间和服务器时间相差大于1分钟则同步服务器时间
        if (webServerDate != null && (is1970 || (abs(webServerDate.time - System.currentTimeMillis()) > oneMinute))) {
            println("当前系统时间为：${DateFormat.format("yyyy-MM-dd HH:mm:ss", Calendar.getInstance())}")
            SystemClock.setCurrentTimeMillis(webServerDate.time)
            println("更新系统时间为：${DateFormat.format("yyyy-MM-dd HH:mm:ss", webServerDate)}")
            println("当前系统时间为：${DateFormat.format("yyyy-MM-dd HH:mm:ss", Calendar.getInstance())}")
        } else {
            println("不需要调用时间")
        }
        return response
    }
}