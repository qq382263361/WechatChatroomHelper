package ui

import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.SwitchCompat
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.google.gson.JsonParser
import com.zdy.project.wechat_chatroom_helper.Constants
import com.zdy.project.wechat_chatroom_helper.R
import com.zdy.project.wechat_chatroom_helper.ui.helper.uisetting.UISettingActivity
import manager.PermissionHelper
import network.ApiManager
import okhttp3.*
import utils.AppSaveInfoUtils
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var thisActivity: MainActivity

    private lateinit var clickMe: Button
    private lateinit var qian: Button
    private lateinit var multiWechat:Button
    private lateinit var detail: TextView
    private lateinit var listContent: LinearLayout

    private lateinit var receiver: PermissionBroadCastReceiver
    private var permissionHelper: PermissionHelper? = null


    inner class PermissionBroadCastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            bindView()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        thisActivity = this@MainActivity

        //权限检查广播
        receiver = PermissionBroadCastReceiver()
        registerReceiver(receiver, IntentFilter(Constants.FILE_INIT_SUCCESS))

        //检查权限
        permissionHelper = PermissionHelper.check(thisActivity)


        //加載佈局
        setContentView(R.layout.activity_main)
        clickMe = findViewById<Button>(R.id.button)
        qian = findViewById<Button>(R.id.button2)
        multiWechat= findViewById<Button>(R.id.button3)
        detail = findViewById<TextView>(R.id.detail)
        listContent = findViewById<LinearLayout>(R.id.list_content)


        //加載可配置項的佈局
        initSetting()
    }

    private fun initSetting() {
        val titles = arrayOf("功能开关", "我使用的是play版本", "助手圆形头像", "进入聊天界面自动关闭助手", "群助手UI设置", "Xposed日志开关","隐藏程序入口")

        for (i in 0 until titles.size) {
            title = titles[i]

            val itemView = LayoutInflater.from(thisActivity).inflate(R.layout.layout_setting_item, listContent, false)
            val text = itemView.findViewById<TextView>(android.R.id.text1)
            val switch = itemView.findViewById<SwitchCompat>(android.R.id.button1)

            text.text = title

            itemView.setOnClickListener { switch.performClick() }

            when (i) {
                0 -> {
                    switch.isChecked = AppSaveInfoUtils.openInfo()
                    switch.setOnCheckedChangeListener { _, isChecked -> AppSaveInfoUtils.setOpen(isChecked) }
                }
                1 -> {
                    switch.isChecked = AppSaveInfoUtils.isPlayVersionInfo()
                    switch.setOnCheckedChangeListener { _, isChecked ->
                        AppSaveInfoUtils.setPlayVersionInfo(isChecked)
                        sendRequest(MyApplication.get().getWechatVersionCode().toString(), AppSaveInfoUtils.isPlayVersionInfo())
                    }
                }
                2 -> {
                    switch.isChecked = AppSaveInfoUtils.isCircleAvatarInfo()
                    switch.setOnCheckedChangeListener { _, isChecked -> AppSaveInfoUtils.setCircleAvatarInfo(isChecked) }
                }
                3 -> {
                    switch.isChecked = AppSaveInfoUtils.autoCloseInfo()
                    switch.setOnCheckedChangeListener { _, isChecked -> AppSaveInfoUtils.setAutoCloseInfo(isChecked) }
                }
                4 -> {
                    switch.visibility = View.INVISIBLE
                    switch.setOnClickListener { if (i == 4) startActivity(Intent(thisActivity, UISettingActivity::class.java)) }
                }
                5 -> {
                    switch.isChecked = AppSaveInfoUtils.openLogInfo()
                    switch.setOnCheckedChangeListener { _, isChecked -> AppSaveInfoUtils.setOpenLog(isChecked) }
                }
                6 -> {
                    switch.isChecked = AppSaveInfoUtils.launcherEntryInfo()
                    switch.setOnCheckedChangeListener { _, isChecked ->
                        AppSaveInfoUtils.setLauncherEntry(isChecked)
                        showHideLauncherIcon(!isChecked)
                    }
                }

            }

            listContent.addView(itemView)
        }

        title = "微信群消息助手"
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    private fun bindView() {

        //获取公告及其Dialog
        clickMe.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("http://116.62.247.71:8080/wechat/")
            startActivity(intent)
        }

        qian.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://QR.ALIPAY.COM/FKX09384NJXB5JXT9MLD11")
            startActivity(intent)
        }
        multiWechat.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://github.com/zhudongya123/WechatChatroomHelper/wiki/在双开-分身-中使用群消息助手")
            startActivity(intent)
        }

        //檢查配置
        loadConfig()
    }

    private fun loadConfig() {
        //是否已经适配了合适的数据的标记(已经成功匹配过)
        val hasSuitWechatData = AppSaveInfoUtils.hasSuitWechatDataInfo()

        //play开关是否打开的标记
        val playVersion = AppSaveInfoUtils.isPlayVersionInfo()

        //当前保存的微信版本号
        val saveWechatVersionCode = AppSaveInfoUtils.wechatVersionInfo()

        //当前保存的主程序版本号
        val saveHelperVersionCode = AppSaveInfoUtils.helpVersionCodeInfo()

        //当前的微信版本号
        val wechatVersionCode = MyApplication.get().getWechatVersionCode().toString()

        //当前的主程序版本号
        val helperVersionCode = MyApplication.get().getHelperVersionCode().toString()

        //  如果微信版本号发生了变化且保存过版本号（上次使用别的版本加载过）
        //  或者保存的数据中是 没有适合的数据的标记
        //  或者主程序版本号发生了改变
        if (wechatVersionCode != saveWechatVersionCode
                && saveWechatVersionCode != "0"
                || !hasSuitWechatData
                || saveHelperVersionCode != helperVersionCode) {

            //则发送数据请求
            sendRequest(wechatVersionCode, playVersion)
        } else {

            //否则则取出上次保存的合适的信息
            detail.setTextColor(0xFF888888.toInt())
            detail.text = AppSaveInfoUtils.showInfo()
        }
    }

    private fun sendRequest(versionCode: String, play_version: Boolean) {

        val requestBody = FormBody.Builder()
                .add("versionCode", versionCode)
                .add("isPlayVersion", if (play_version) "1" else "0").build()

        val request = Request.Builder().url(ApiManager.UrlPath.CLASS_MAPPING).post(requestBody).build()

        ApiManager.okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call?, e: IOException?) {
            }

            override fun onResponse(call: Call, response: Response) {

                try {
                    val string = response.body()!!.string()
                    val jsonObject = JsonParser().parse(string).asJsonObject
                    val code = jsonObject.get("code").asInt
                    val msg = jsonObject.get("msg").asString

                    //保存匹配到的数据
                    if (code == 0) {
                        AppSaveInfoUtils.setJson(jsonObject.get("data").toString())
                        setSuccessText(msg)
                    } else {
                        AppSaveInfoUtils.setJson("")
                        setFailText(msg)
                    }

                    //保存主程序版本号
                    AppSaveInfoUtils.setHelpVersionCodeInfo(MyApplication.get().getHelperVersionCode().toString())

                } catch (e: Exception) {
                    e.printStackTrace()
                    setFailText("从服务器获取数据失败，请联系检查网络链接")
                }
            }
        })
    }

    private fun showHideLauncherIcon(show: Boolean) {
        val p = packageManager
        val componentName = ComponentName(this, packageName + ".LauncherDelegate")
        p.setComponentEnabledSetting(componentName,
                if (show) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP)
    }

    private fun setFailText(msg: String) {
        runOnUiThread {
            detail.text = msg
            detail.setTextColor(0xFFFF0000.toInt())

            AppSaveInfoUtils.setSuitWechatDataInfo(false)
            AppSaveInfoUtils.setShowInfo(msg)
        }
    }

    private fun setSuccessText(msg: String) {
        runOnUiThread {
            detail.setTextColor(0xFF888888.toInt())
            detail.text = msg

            AppSaveInfoUtils.setWechatVersionInfo(MyApplication.get().getWechatVersionCode().toString())
            AppSaveInfoUtils.setSuitWechatDataInfo(true)
            AppSaveInfoUtils.setShowInfo(msg)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionHelper?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onRestart() {
        super.onRestart()
        permissionHelper = PermissionHelper.check(thisActivity)
    }

}