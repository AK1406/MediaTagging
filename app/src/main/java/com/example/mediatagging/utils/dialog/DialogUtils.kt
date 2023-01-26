package com.example.mediatagging.utils.dialog

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.widget.LinearLayout

class DialogUtils {

    companion object {
        val TAG = this.javaClass.name
        fun showAlertStyleMessage(
            context: Context,
            title: String = "",
            message: String,
            confirmClickText: String
        ) {
            val alertBuilder = AlertDialog.Builder(context)
            alertBuilder.apply {
                if (title.isNotBlank())
                    setTitle(title)
                setMessage(message)
                setPositiveButton(
                    confirmClickText
                ) { dialog, _ ->
                    dialog?.dismiss()
                }
            }
            alertBuilder.show()
        }

        fun showCustomDialogColoredBtn(
            context: Context,
            title: String? = null,
            message: String? = null,
            positiveButtonText: String? = "OK",
            negativeButtonText: String? = null,
            positiveClickListener: DialogInterface.OnClickListener,
            negativeClickListener: DialogInterface.OnClickListener? = null
        ) {
            val builder = AlertDialog.Builder(context)
            //set title for alert dialog
            builder.setTitle(title)
            //set message for alert dialog
            builder.setMessage(message)
            //performing positive action
            builder.setPositiveButton(positiveButtonText, positiveClickListener)
            if (negativeButtonText != null) {
                val listener = negativeClickListener
                    ?: DialogInterface.OnClickListener { p0, p1 -> p0?.dismiss() }
                builder.setNegativeButton(negativeButtonText, listener)
            }
            // Create the AlertDialog
            val alertDialog: AlertDialog = builder.create()
            // Set other dialog properties
            alertDialog.setCancelable(false)
            alertDialog.show()

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            params.setMargins(20, 0, 0, 0)
            //set left margin of positive button
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).layoutParams = params
            //set color to alert buttons
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setBackgroundColor(Color.parseColor("#06AA6F"))
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE)
            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setBackgroundColor(Color.parseColor("#065BAA"))
            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE)
        }


        fun showCustomDialog(
            context: Context,
            title: String? = null,
            message: String? = null,
            positiveButtonText: String? = "OK",
            negativeButtonText: String? = null,
            positiveClickListener: DialogInterface.OnClickListener,
            negativeClickListener: DialogInterface.OnClickListener? = null

        ) {
            val builder = AlertDialog.Builder(context)
            //set title for alert dialog
            builder.setTitle(title)
            //set message for alert dialog
            builder.setMessage(message)
            //performing positive action
            builder.setPositiveButton(positiveButtonText, positiveClickListener)
            if (negativeButtonText != null) {
                val listener = negativeClickListener
                    ?: DialogInterface.OnClickListener { p0, p1 -> p0?.dismiss() }
                builder.setNegativeButton(negativeButtonText, listener)
            }
            // Create the AlertDialog
            val alertDialog: AlertDialog = builder.create()
            // Set other dialog properties
            alertDialog.setCancelable(false)
            alertDialog.show()
        }

        fun showClearTextDialog(
            context: Context,
            message: String,
            title: String,
            positiveClickListener: DialogInterface.OnClickListener
        ) {
            val builder = AlertDialog.Builder(context)
            //set title for alert dialog
            builder.setTitle(title)
            //set message for alert dialog
            builder.setMessage(message)
            //performing positive action
            builder.setPositiveButton("Confirm", positiveClickListener)

            //performing negative action
            builder.setNegativeButton("Cancel") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            // Create the AlertDialog
            val alertDialog: AlertDialog = builder.create()
            // Set other dialog properties
            alertDialog.setCancelable(false)
            alertDialog.show()
        }
    }
}

