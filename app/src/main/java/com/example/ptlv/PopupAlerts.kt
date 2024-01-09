package com.example.ptlv

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.Window
import android.widget.TextView

class PopupAlerts(context: Context, alertList: MutableList<Activity.Alert>) : Dialog(context) {

    private var alerts_list: MutableList<Activity.Alert> = alertList

    lateinit var MainPopupTextView: TextView
    var MainMessage:String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.popup_alerts)

        create_popup_messages()

        MainPopupTextView = findViewById<TextView>(R.id.MainPopupText)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            MainPopupTextView.text = Html.fromHtml(MainMessage, Html.FROM_HTML_MODE_COMPACT)
        } else {
            @Suppress("DEPRECATION")
            MainPopupTextView.text = Html.fromHtml(MainMessage)
        }

    }

    private fun create_popup_messages()
    {
        for(alert in alerts_list)
        {
            MainMessage+= "<br>⚠️&ensp;" + alert.message + "</br>" + "<br><b>&ensp;Impacted Lines: " + alert.impact + "</b></br><br></br>"
        }
    }
}
