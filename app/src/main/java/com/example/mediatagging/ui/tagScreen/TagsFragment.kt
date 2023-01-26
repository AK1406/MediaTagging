package com.example.mediatagging.ui.tagScreen

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mediatagging.databinding.FragmentTagsBinding
import com.example.mediatagging.model.TagCountModel
import com.example.mediatagging.ui.home.DashboardActivity
import com.example.mediatagging.utils.FragmentController
import com.example.mediatagging.utils.StorageManagerKeys
import com.example.mediatagging.utils.StorageManagerUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class TagsFragment : Fragment() {
    val TAG = "TagsFragment"
    lateinit var binding: FragmentTagsBinding
    var mAdapter: TagCountAdapter? = null
    var user: FirebaseUser? = null
    private var gridLayoutManager: GridLayoutManager? = null
    val tagCountList: ArrayList<TagCountModel> = arrayListOf()
    var tagRef: DatabaseReference? = null
    var tag: TagCountModel? = null
    var userUid: String? = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentTagsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    override fun onResume() {
        super.onResume()
        (activity as DashboardActivity).binding.toolbar.ivBack.visibility = View.VISIBLE
        (activity as DashboardActivity).binding.toolbar.tvHeaderName.text = "Tags"
    }


    fun initView() {
        noTag()
        user = FirebaseAuth.getInstance().currentUser
        userUid = user?.uid
        tagRef = FirebaseDatabase.getInstance()
            .getReference("mediaTagging").child(userUid.toString()).child("tags")
        fetchTags()
        setListener()
    }

    fun noTag(){
        binding.llNoHashtag.visibility = View.VISIBLE
        binding.rvTagList.visibility = View.GONE
    }
    fun addTags(){
        binding.llNoHashtag.visibility = View.GONE
        binding.rvTagList.visibility = View.VISIBLE
    }

    fun setListener() {
        (activity as DashboardActivity).binding.toolbar.ivBack.setOnClickListener {
            FragmentController.clearBackStack(requireContext())
            (activity as DashboardActivity).binding.toolbar.ivBack.visibility = View.GONE
            (activity as DashboardActivity).binding.toolbar.tvHeaderName.text = "Home"
        }
    }

    fun setupAdapter() {
        Log.i(TAG, "setupAdapter")
        if(tagCountList.size>0){
            addTags()
        }else{
            noTag()
        }
        mAdapter = TagCountAdapter(requireContext(), tagCountList)
        val column = calNoOfColumns(requireContext())
        gridLayoutManager =
            GridLayoutManager(requireContext(), column, LinearLayoutManager.VERTICAL, false)
        binding.rvTagList.layoutManager = gridLayoutManager
        binding.rvTagList.setHasFixedSize(true)
        binding.rvTagList.adapter = mAdapter
    }

    private fun calNoOfColumns(context: Context): Int {
        val displayMetrics = context.resources.displayMetrics
        val dpWidth = displayMetrics.widthPixels / displayMetrics.density
        return (dpWidth / 180).toInt()
    }

    private fun fetchTags() {
        Log.i(TAG, "fetchTags")
        tagRef?.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                //hideProgress()
                Toast.makeText(context, "$p0", Toast.LENGTH_SHORT).show()
            }

            override fun onDataChange(p0: DataSnapshot) {
                Log.i(TAG, "onDataChange")
                try {
                    if (p0.exists()) {
                        tagCountList.clear()
                        for (i in p0.children) {
                            tag = i.getValue(TagCountModel::class.java)
                            tag?.let { tagCountList.add(it) }
                        }
                    }
                    setupAdapter()
                } catch (e: Exception) {
                    Log.e(TAG, "exception: $e")
                }
            }

        })

    }

}