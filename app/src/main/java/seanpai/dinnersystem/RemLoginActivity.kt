package seanpai.dinnersystem

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.android.synthetic.main.activity_rem_login.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.toast
import org.json.JSONObject
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy

class RemLoginActivity : AppCompatActivity() {
    private lateinit var preferences: SharedPreferences
    private lateinit var progBarHandler: ProgressBarHandler
    private lateinit var preferenceHelper: SharedPreferencesHelper
    private lateinit var keyStoreHelper: KeyStoreHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rem_login)
        CookieHandler.setDefault(CookieManager(null, CookiePolicy.ACCEPT_ALL))

        //initialize late init variables
        preferences = this.getSharedPreferences("dinnersys_data",Context.MODE_PRIVATE)
        progBarHandler = ProgressBarHandler(this)

        preferenceHelper = SharedPreferencesHelper(applicationContext)
        keyStoreHelper = KeyStoreHelper(applicationContext,preferenceHelper)

        //lemme hide u
        remButton.visibility = View.INVISIBLE
        fallbackButton.visibility = View.INVISIBLE

        if(preferences.getString("clear",null) == null || preferences.getString("clear", null) == "cleared" || preferences.getString("clear", null) == "cleared_2"){
            preferences.edit().remove("username").remove("password").remove("name").putString("clear","cleared_3").apply()
            toast("請重新登入")
            fallbackAction()
            return
        }


        progBarHandler.show()
        val url = "$dinnersysURL/frontend/u_move_u_dead/version.txt"
        val versionRequest = StringRequest(url, Response.Listener {
            //indicator

            window.setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            )
            //indicator
            var update = true
            val version = JSONObject(it).getJSONArray("android")
            println(it)
            for (i in 0 until version.length()) {
                val ver = version.getInt(i)
                if (ver == currentVersion) {
                    update = false
                    break
                }
            }
            if (update) {
                //indicator
                progBarHandler.hide()
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                //indicator
                alert("請至Google Play更新最新版本的點餐系統!", "偵測到更新版本") {
                    positiveButton("OK(跳轉至GooglePlay)") {
                        val packageName = packageName
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName)))
                        } catch (e: ActivityNotFoundException) {
                            startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)
                                )
                            )
                        }
                    }
                }.build().apply {
                    setCancelable(false)
                    setCanceledOnTouchOutside(false)
                }.show()

            }
            //indicator
            progBarHandler.hide()
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            //indicator
            initLoginButton()
        }, Response.ErrorListener {
            alert("請注意網路連線", "不知名的錯誤") {
                positiveButton("OK") {}
            }
        })
        VolleySingleton.getInstance(this).addToRequestQueue(versionRequest)

    }

    private fun initLoginButton(){
        if(preferences.getString("username",null) != null){
            val name = preferences.getString("name",null)!!
            remButton.visibility = View.VISIBLE
            fallbackButton.visibility = View.VISIBLE
            remButton.isEnabled = true
            remButton.text = "以${name}登入"
        }else{
            fallbackAction()
        }
    }

    var back = true
    override fun onBackPressed() {
        //super.onBackPressed()
        if(back){
            back = false
            toast("再按一次以退出")
        }else{
            this.finishAffinity()
        }
    }

    private fun fallbackAction(){
        startActivity(Intent(this,LoginActivity::class.java))
    }

    fun fallbackClick(view: View){
        fallbackAction()
    }

    fun remLogin(view: View){
        //indicator
        progBarHandler.show()
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        //indicator
        val usr = preferences.getString("username", "")!!
        val psw = keyStoreHelper.decrypt(preferenceHelper.input!!)
        var chosenID = preferences.getString("org_id", null)
        val noID = chosenID == null
        if(noID){
            preferences.edit().putString("org_id", "1").apply()
            chosenID = "1"
        }
        val timeStamp = (System.currentTimeMillis() / 1000).toString()
        val loginRequest = object : StringRequest(Method.POST, dsRequestURL,Response.Listener { string ->
            println(isValidJson(string))
            if (isValidJson(string)){
                constPassword = psw
                constUsername = usr
                userInfo = JSONObject(string)
                //indicator
                progBarHandler.hide()
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                //indicator

                FirebaseCrashlytics.getInstance().setUserId(usr)

                if(userInfo.getJSONArray("valid_oper").toString().contains("select_class") && !userInfo.getJSONArray("valid_oper").toString().contains("select_others")){
                    startActivity(Intent(view.context,DinnermanMainActivity::class.java))
                }else{
                    startActivity(Intent(view.context,StudentMainActivity::class.java))
                }
            }else {
                //indicator
                progBarHandler.hide()
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                //indicator

                alert("請注意帳號密碼是否錯誤，若持續失敗請通知開發人員!\n錯誤訊息$string","登入失敗"){
                    positiveButton("OK"){}
                }.show()
            }
        },Response.ErrorListener { error ->
            //indicator
            progBarHandler.hide()
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            //indicator
            println(error)
            alert ("請注意網路狀態，或通知開發人員!","不知名的錯誤"){
                positiveButton("OK"){}
            }.show()
        }){
            override fun getParams(): MutableMap<String, String> {
                val postParam: MutableMap<String, String> = HashMap()
                postParam["cmd"] = "login"
                postParam["id"] = usr
                postParam["password"] = psw
                postParam["time"] = timeStamp
                postParam["org_id"] = chosenID!!
                postParam["device_id"] = "HELLO_FROM_ANDROID"
                return postParam
            }
        }
        VolleySingleton.getInstance(this).addToRequestQueue(loginRequest)
    }


}
