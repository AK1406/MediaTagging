package com.example.mediatagging.ui.home

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.mediatagging.utils.ApiListener
import com.example.mediatagging.utils.dialog.ProgressDialog

open class BaseFragment : Fragment(), ApiListener {

    private val TAG: String = "BaseFragment"
    private var mProgressDialog: Dialog? = null
    protected lateinit var contextFragment: Context;


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        contextFragment = requireContext();
        return super.onCreateView(inflater, container, savedInstanceState)
    }


    protected fun showProgress(context: Context?,message:String ="Please wait...") {
        Log.d(TAG, "showProgress()")
        mProgressDialog?.dismiss()
        mProgressDialog = ProgressDialog.getProgressDialog(requireContext(), message)
        mProgressDialog!!.setCancelable(false)
        mProgressDialog!!.setCanceledOnTouchOutside(false)
        mProgressDialog!!.show()
    }

    override fun hideProgress() {
        Log.d(TAG, "hideProgress()")
        mProgressDialog?.dismiss()
    }


    override fun showProgress() {
        showProgress(context)
    }


    override fun networkError(message: String) {
        Toast.makeText(context,"Please check your network",Toast.LENGTH_LONG).show()
    }

}
