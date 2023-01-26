package com.example.mediatagging.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.mediatagging.R

class StorageManagerUtil(val context: Context?) {
    private val sharedPreferences: SharedPreferences

    init {
        val prefsFile = context!!.packageName
        sharedPreferences = context.getSharedPreferences(prefsFile, Context.MODE_PRIVATE)
        sharedPreferences.edit().apply()
    }

    companion object {

        private var instance: StorageManagerUtil? = null

        @Synchronized
        fun getInstance(context: Context?): StorageManagerUtil {
            if (instance == null) {
                instance = StorageManagerUtil(context)
            }
            return instance!!
        }
/*        var instance by lazy {
            SharedPreferenceUtil(context)
        }*/
    }

    private fun deletePreference(key: String?) {
        if (sharedPreferences.contains(key)) {
            sharedPreferences.edit().remove(key).apply()
        }
    }

    fun deleteAllPreference() {
        try {
            sharedPreferences.edit().clear().apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun savePreference(key: String?, value: Any?) {
        deletePreference(key)
        if (value is Boolean) {
            sharedPreferences.edit().putBoolean(key, value).apply()
        } else if (value is Int) {
            sharedPreferences.edit().putInt(key, value).apply()
        } else if (value is Float) {
            sharedPreferences.edit().putFloat(key, value).apply()
        } else if (value is Long) {
            sharedPreferences.edit().putLong(key, value).apply()
        } else if (value is String) {
            sharedPreferences.edit().putString(key, value).apply()
        } else if (value is Enum<*>) {
            sharedPreferences.edit().putString(key, value.toString()).apply()
        } else if (value != null) {
            throw RuntimeException(context!!.getString(R.string.preference_expection))
        }
        sharedPreferences.edit().apply()
    }

    fun <T> getPreference(key: String?): T {
        return sharedPreferences.all.get(key) as T
    }

    fun <T> getPreference(key: String?, defValue: T): T {
        return sharedPreferences.all.get(key) as T ?: defValue
    }

    fun isPreferenceExists(key: String?): Boolean {
        return sharedPreferences.contains(key)
    }

//    fun isLoggedIn():Boolean{
//      return  sharedPreferences.getString(StorageManagerKeys.ACCESS_TOKEN.name,null)!=null && sharedPreferences.getString(StorageManagerKeys.TOKEN_TYPE.name,null)!=null
//    }
}