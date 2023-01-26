package com.example.mediatagging.ui.video

import android.R.attr.path
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mediatagging.R
import com.example.mediatagging.databinding.FragmentVideoBinding
import com.example.mediatagging.model.ProjectModel
import com.example.mediatagging.model.VideoModel
import com.example.mediatagging.ui.home.DashboardActivity
import com.example.mediatagging.utils.FragmentController
import com.example.mediatagging.utils.SnackBarUtils
import com.example.mediatagging.utils.StorageManagerKeys
import com.example.mediatagging.utils.StorageManagerUtil
import com.example.mediatagging.utils.constants.Constants
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class VideoFragment : Fragment() {

    val TAG = "VideoFragment"
    lateinit var binding: FragmentVideoBinding
    var projectItem: ProjectModel? = null
    var projectItemPos: Int = -1
    var storageRef: StorageReference? = null
    var user: FirebaseUser? = null
    var userUid: String? = ""
    var projectName: String = ""
    val PICK_VIDEO_REQUEST = 7
    var videoRef: StorageReference? = null
    private var gridLayoutManager: GridLayoutManager? = null
    var projectRef: DatabaseReference? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentVideoBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    companion object {
        var mAdapter: VideoAdapter? = null
        val videoList: ArrayList<VideoModel> = arrayListOf()
    }

    override fun onResume() {
        super.onResume()
        (activity as DashboardActivity).binding.toolbar.tvHeaderName.text = projectName + " videos"
        (activity as DashboardActivity).binding.toolbar.ivBack.visibility = View.VISIBLE
        (activity as DashboardActivity).binding.toolbar.cvProfile.visibility = View.INVISIBLE
        (activity as DashboardActivity).binding.toolbar.ivOptions.visibility = View.VISIBLE
    }

    private fun initView() {
        noVideo()
        storageRef = FirebaseStorage.getInstance().reference
        setAdapter()
        handleData()
        setListener()
        user = FirebaseAuth.getInstance().currentUser
        userUid = user?.uid
        projectRef =
            FirebaseDatabase.getInstance().getReference("mediaTagging").child(userUid.toString())
                .child("projects")
        videoRef =
            storageRef?.child("$userUid")?.child("projects")?.child(projectName)?.child("videos")
    }

    private fun handleData() {
        videoList.clear()
        if (arguments != null) {
            projectItem = arguments?.getSerializable(Constants.ProjectItem.value) as ProjectModel
            projectItemPos = arguments?.getInt(Constants.Position.value)!!
        }
        projectName = projectItem?.projectName.toString()
        StorageManagerUtil.getInstance(context)
            .savePreference(
                StorageManagerKeys.PROJECT_NAME.name,
                projectName
            )
        projectItem?.videoList?.let { videoList.addAll(it) }
        if (videoList.size > 0) {
            videoAdded()
            mAdapter?.insertVideoList(videoList)
        } else {
            noVideo()
        }
    }


    fun noVideo() {
        binding.llNoVideo.visibility = View.VISIBLE
        binding.rvVideos.visibility = View.GONE
    }

    fun videoAdded() {
        binding.llNoVideo.visibility = View.GONE
        binding.rvVideos.visibility = View.VISIBLE
    }

    fun setListener() {
        (activity as DashboardActivity).binding.toolbar.ivBack.setOnClickListener {
            (activity as DashboardActivity).onBackPressed()
        }

        (activity as DashboardActivity).binding.toolbar.ivOptions.setOnClickListener {
            val popupMenu = PopupMenu(requireContext(), it)
            popupMenu.inflate(R.menu.image_menu)
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.addItems -> {
                        uploadVideo()
                        true
                    }
                    R.id.sort -> {
                        showSortDialog()
                        true
                    }

                    else -> super.onOptionsItemSelected(item)
                }
            }
            popupMenu.show()
        }
    }

    fun showSortDialog() {
        val options = arrayOf("None", "reverse", "max tags")
        val checkedItem = 0

        MaterialAlertDialogBuilder(requireContext()).setTitle(resources.getString(R.string.sort_heading))
            .setNeutralButton("cancel") { dialog, which ->
                dialog.dismiss()
            }.setPositiveButton("ok") { dialog, which ->
                dialog.dismiss()
            }
            // Single-choice items (initialized with checked item)
            .setSingleChoiceItems(options, checkedItem) { dialog, which ->
                // Respond to item chosen
                when (which) {
                    0 -> {
                        dialog.dismiss()
                    }
                    1 -> {
                        mAdapter?.reverseList()
                    }
                    2 -> {
                        mAdapter?.sortByMaxTag()
                    }

                }
            }.show()
    }


    fun uploadVideo() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "video/*"
        startActivityForResult(intent, PICK_VIDEO_REQUEST)
    }

    // Handle the result of the file picker intent
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == PICK_VIDEO_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.let {
                val videoUri: Uri = it.data!!
                val fileRef = videoRef?.child(videoUri.lastPathSegment!!)
                val uploadTask = fileRef?.putFile(videoUri)
                val video = VideoModel(
                    "${Constants.Video.value}${videoList.size + 1}",
                    videoUri.toString(),
                    arrayListOf()
                )
                videoAdded()
                mAdapter?.insertVideo(video)
                video.playAnimation = true
                uploadTask?.addOnSuccessListener {
                    val progress = (100.0 * it.bytesTransferred) / it.totalByteCount
                    Log.i(TAG, "progress: $progress")
                    if (progress == 100.0) {
                        SnackBarUtils.showCustomSnackBar(binding.root, "video uploaded", false)
                        video.playAnimation = false
                        Log.i(TAG, "Uri PAth: ${videoUri}")
                        fetchStorageVideo(videoUri, "${Constants.Video.value}${videoList.size + 1}")
                    }

                }?.addOnFailureListener { exception ->
                    // Upload failed
                    Log.i(TAG, "Upload failed: $exception")
                }
            }
        }
    }


    fun fetchStorageVideo(videoUri: Uri, videoTitle: String) {
        Log.i(TAG, "fetchStorageVideo")
        val videoRef =
            storageRef?.child("$userUid")?.child("projects")?.child(projectName)?.child("videos")
        val path = videoUri.path
        val videoName = path?.substringAfterLast("/")
        Log.i(TAG, "videoName: $videoName")
        videoName?.let {
            videoRef?.child(it)?.downloadUrl?.addOnSuccessListener { videoUri ->
                Log.i(TAG, "videoUri: $videoUri")
                val video = VideoModel(
                    videoTitle,
                    videoUri.toString(),
                    arrayListOf()
                )
                videoList.add(video)
                mAdapter?.updateVideo(video, videoList.size - 1)
                Log.i(TAG, "video: $video")
                updateSavedProject(projectName, videoList)
            }?.addOnFailureListener {
                // Handle download failure
            }
        }

    }

    fun setAdapter() {
        Log.i(TAG, "setAdapter")
        mAdapter = VideoAdapter(requireContext(), { item, pos ->
            val bundle = Bundle()
            bundle.putSerializable(Constants.Video.value, item)
            bundle.putInt(Constants.Position.value, pos)
            bundle.putString(Constants.ProjectName.value, projectName)
            FragmentController.setFragmentOn(
                requireContext(),
                R.id.fragmentMain,
                bundle,
                true,
                VideoDetailsFragment::class.java
            )
        }, { pos ->
            removeVideo(pos)
        })
        val column = calNoOfColumns(requireContext())
        gridLayoutManager =
            GridLayoutManager(requireContext(), column, LinearLayoutManager.VERTICAL, false)
        binding.rvVideos.layoutManager = gridLayoutManager
        binding.rvVideos.setHasFixedSize(true)
        binding.rvVideos.adapter = mAdapter

    }

    private fun removeVideo(pos: Int) {
        //showProgress()
        var project: ProjectModel? = null
        CoroutineScope(Dispatchers.IO).launch {
            projectRef?.child(projectName)?.addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    //hideProgress()
                    Toast.makeText(context, "$p0", Toast.LENGTH_SHORT).show()
                }

                override fun onDataChange(p0: DataSnapshot) {
                    Log.i(TAG, "onDataChange")
                    try {
                        if (p0.exists()) {
                            videoList.clear()
                            project = p0.getValue(ProjectModel::class.java)
                            Log.i(TAG, " ptoject: $project")
                            project?.videoList?.let { videoList.addAll(it) }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "exception: $e")
                    }
                }
            })
            withContext(Dispatchers.Main) {
                if (pos >= 0) {
                    Log.i(TAG, "pso: $pos -> ${videoList.size}")
                    videoList.removeAt(pos)
                    Log.i(TAG, "videoList: $videoList")
                    if (videoList.size == 0) {
                        noVideo()
                    } else {
                        mAdapter?.insertVideoList(videoList)
                    }
                }
                projectRef?.child(projectName)
                    ?.setValue(
                        ProjectModel(
                            projectName,
                            project?.imageList,
                            videoList,
                            project?.imageList?.size,
                            videoList.size
                        )
                    )
                    ?.addOnCompleteListener {
                        SnackBarUtils.showCustomSnackBar(
                            binding.root,
                            "video removed",
                            false
                        )
                    }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy")
    }

    private fun calNoOfColumns(context: Context): Int {
        val displayMetrics = context.resources.displayMetrics
        val dpWidth = displayMetrics.widthPixels / displayMetrics.density
        return (dpWidth / 180).toInt()
    }


    private fun updateSavedProject(
        projectName: String, newVideoList: ArrayList<VideoModel>
    ) {
        Log.i(TAG, "updateSavedProject")
        /*val project : ProjectModel
        projectRef?.child(projectName)?.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                //hideProgress()
                Toast.makeText(context, "$p0", Toast.LENGTH_SHORT).show()
            }

            override fun onDataChange(p0: DataSnapshot) {
                Log.i(TAG, "onDataChange")
                try {
                    if (p0.exists()) {
                        videoList.clear()
                        project = p0.getValue(ProjectModel::class.java)
                        Log.i(TAG, " ptoject: $project")
                        project?.videoList?.let { videoList.addAll(it) }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "exception: $e")
                }
            }
        })


*/
        projectRef?.child(projectName)?.child("videoList")
            ?.setValue(
                newVideoList
            )?.addOnCompleteListener {
                Log.i(TAG, "videoList is updated successfully ")
                //imageViewModel.setImageList(imageList)
            }
        projectRef?.child(projectName)?.child("videoCount")
            ?.setValue(
                newVideoList.size
            )?.addOnCompleteListener {
                Log.i(TAG, "videoCount is updated successfully ")
            }
    }

}