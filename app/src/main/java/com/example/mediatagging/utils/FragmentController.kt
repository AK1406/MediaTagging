package com.example.mediatagging.utils

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager

object FragmentController {
    val TAG = "FragmentController"
    fun FragmentController() {}

    /**
     * @param replaceViewId
     * @param fragmentClass
     */
    fun setFragment(
        context: Context,
        replaceViewId: Int,
        backPressed: Boolean,
        fragmentClass: Class<out Fragment?>
    ) {
        try {
            val fragment = fragmentClass.newInstance()

            val transaction =
                (context as AppCompatActivity).supportFragmentManager.beginTransaction()
            transaction.replace(replaceViewId, fragment!!)
            if (backPressed) transaction.addToBackStack(null)
            transaction.commit()
        } catch (ex: Exception) {
            Log.e(TAG, "setFragment($ex)", ex)
        }
    }

    /**
     * @param context
     * @param replaceViewId
     * @param bundle
     * @param fragmentClass
     */
    fun setFragment(
        context: Context,
        replaceViewId: Int,
        bundle: Bundle?,
        backPressed: Boolean,
        fragmentClass: Class<out Fragment>
    ) {
        try {
            val fragment = fragmentClass.newInstance()
            if (bundle != null) fragment.arguments = bundle
            val transaction =
                (context as AppCompatActivity).supportFragmentManager.beginTransaction()
            transaction.replace(replaceViewId, fragment, fragmentClass.simpleName)
            if (backPressed) transaction.addToBackStack(null)
            transaction.commit()
        } catch (ex: Exception) {
            Log.e(TAG, "setFragment($ex)", ex)
        }
    }


    /**
     * @param context
     * @param replaceViewId
     * @param bundle
     * @param fragmentClass
     */
    fun setFragmentOn(
        context: Context,
        replaceViewId: Int,
        bundle: Bundle?,
        backPressed: Boolean,
        fragmentClass: Class<out Fragment>,
        frag: Fragment? = null
    ) {
        try {

            val fragment = frag ?: fragmentClass.newInstance()
            if (bundle != null) fragment.arguments = bundle
            val manager = (context as AppCompatActivity).supportFragmentManager
            val transaction =
                manager.beginTransaction()
            transaction.add(replaceViewId, fragment)
            if (backPressed) transaction.addToBackStack(null)
            transaction.commit()
        } catch (ex: Exception) {
            Log.e(TAG, "setFragmentOn($ex)", ex)
        }
    }

    fun clearBackStack(context: Context) {
        Log.d(TAG, "clearBackStack")
        val manager: FragmentManager = (context as AppCompatActivity).supportFragmentManager
        Log.d(TAG, "backStackCount: ${manager.backStackEntryCount}")
        if (manager.backStackEntryCount > 0) {
            val first: FragmentManager.BackStackEntry = manager.getBackStackEntryAt(1)
            manager.popBackStack(first.id, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
    }

}