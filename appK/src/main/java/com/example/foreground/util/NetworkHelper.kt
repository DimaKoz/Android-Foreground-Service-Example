package com.example.foreground.util

import android.content.Context
import android.net.ConnectivityManager

object NetworkHelper {
    @JvmStatic
    fun isInternetAvailable(pContext: Context?): Boolean {
        if (pContext == null) {
            return false
        }
        val cm = pContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting
    }
}