package com.example.mediatagging

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.StaggeredGridLayoutManager

class CustomStaggeredGridLayoutParams(c: Context, attrs: AttributeSet?) : StaggeredGridLayoutManager.LayoutParams(c, attrs) {
    var spanSize: Int = 0
}
