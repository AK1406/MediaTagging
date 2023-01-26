package com.example.mediatagging.ui.video

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.mediatagging.ExtensionFunctionUtils.afterTextChanged
import com.example.mediatagging.databinding.FragmentVideoDetailsBinding
import com.example.mediatagging.databinding.SearchTagsBinding
import com.example.mediatagging.model.Choices
import com.example.mediatagging.model.TagCountModel
import com.example.mediatagging.model.VideoModel
import com.example.mediatagging.model.request.GptRequest
import com.example.mediatagging.model.response.GptResponse
import com.example.mediatagging.retrofit.RetrofitClient
import com.example.mediatagging.ui.home.DashboardActivity
import com.example.mediatagging.ui.project.PhotosFragment
import com.example.mediatagging.utils.SnackBarUtils
import com.example.mediatagging.utils.StorageManagerKeys
import com.example.mediatagging.utils.StorageManagerUtil
import com.example.mediatagging.utils.constants.Constants
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import kotlin.collections.ArrayList

class VideoDetailsFragment : Fragment() {
    val TAG = "VideoDetailsFragment"
    lateinit var binding: FragmentVideoDetailsBinding
    var videoItem: VideoModel? = null
    var hashtagSuggestions: ArrayList<String> = arrayListOf()
    var addedNewHastag: ArrayList<String> = arrayListOf()
    var resList: ArrayList<Choices> = arrayListOf()
    var tagRef: DatabaseReference? = null
    var request: GptRequest? = null
    var user: FirebaseUser? = null
    var userUid: String? = ""
    private var projectRef: DatabaseReference? = null
    var projectName: String = ""
    var pos: Int? = -1
    var selectedVideo: VideoModel? = null
    var play: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentVideoDetailsBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }


    fun initView() {
        Log.i(TAG, "initView")
        user = FirebaseAuth.getInstance().currentUser
        userUid = user?.uid
        projectName = StorageManagerUtil.getInstance(context)
            .getPreference<String>(StorageManagerKeys.PROJECT_NAME.name, "").toString()

        handleData()
        setListener()
    }

    fun setListener() {
        Log.i(TAG, "setListener")
        binding.tvAddTags.setOnClickListener {
            showAddTagAlertDialog(requireContext())
        }
        binding.cvVideo.setOnClickListener {
            Log.i(TAG, "click")
            play = !play
            playVideo()
        }

        binding.vvVideo.setOnCompletionListener {
            binding.ivPlay.visibility = View.VISIBLE
            binding.ivPause.visibility = View.INVISIBLE
        }

    }

    override fun onPause() {
        super.onPause()
        (activity as DashboardActivity).binding.toolbar.tvHeaderName.text = projectName + " videos"
        (activity as DashboardActivity).binding.toolbar.ivOptions.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        (activity as DashboardActivity).binding.toolbar.ivOptions.visibility = View.GONE
        (activity as DashboardActivity).binding.toolbar.tvHeaderName.text =
            videoItem?.videoTitle
    }

    fun handleData() {
        if (arguments != null) {
            addedNewHastag = arrayListOf<String>()
            videoItem = arguments?.getSerializable(Constants.Video.value) as VideoModel
            //projectName = arguments?.getString(Constants.ProjectName.toString())
            pos = arguments?.getInt(Constants.Position.value)
        }
        Log.i(TAG, "videoItem: $videoItem")
        videoItem?.addedTags?.let { addedNewHastag.addAll(it) }
        setHashtags(addedNewHastag)
        val videoUri = Uri.parse(videoItem?.videoUri)
        binding.vvVideo.setVideoURI(videoUri)

        tagRef =
            FirebaseDatabase.getInstance()
                .getReference("mediaTagging").child(userUid.toString()).child("tags")
                .child(projectName)
        projectRef =
            Firebase.database.getReference("mediaTagging").child(userUid.toString())
                .child("projects")
                .child(projectName).child("videoList")

    }


    private fun showAddTagAlertDialog(context: Context) {
        val dialog = AlertDialog.Builder(context)
        val dialogBinding = SearchTagsBinding.inflate(LayoutInflater.from(context))
        dialog.setView(dialogBinding.root)
        dialog.setCancelable(false)
        val alertDialog = dialog.create()
        alertDialog.show()
        dialogBinding.ivClose.setOnClickListener {
            alertDialog.dismiss()
        }
        dialogBinding.etSearch.afterTextChanged {
            Log.i(TAG, "afterTextChanged")
            val word = dialogBinding.etSearch.text.toString().trim()
            if (word.isNotBlank()) {
                Log.i(TAG, word)
                request =
                    GptRequest("text-davinci-003", "generate 10 hashtag related to $word")
                sendMsg(request!!, word, dialogBinding)
            }
        }
    }

    fun suggestHashtag(chipList: ArrayList<String>, dialogBinding: SearchTagsBinding) {
        val rnd = Random()
        chipList.forEach { chipText ->
            val chip = Chip(requireContext())
            val color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))
            chip.text = chipText
            dialogBinding.chipGroupSuggested.addView(chip)
            chip.setOnClickListener {
                chip.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#74CBF1"))
                chip.setTextColor(Color.parseColor("#067BAF"))
                chip.isClickable = false
                chip.isEnabled = false
                val newChip = Chip(context)
                newChip.text = chip.text
                newChip.chipBackgroundColor = ColorStateList.valueOf(color)
                binding.chipGroup.addView(newChip)
                newChip.setTextColor(Color.WHITE)
                addedNewHastag.add(newChip.text.toString())
                updateSavedVideo(pos, addedNewHastag)
                saveTag(newChip.text.toString())
                //getVideo(addedNewHastag)
            }
        }
        dialogBinding.chipGroupSuggested.invalidate()
    }

    fun setHashtags(chipList: ArrayList<String>) {
        val rnd = Random()
        chipList.forEach { chipText ->
            val chip = Chip(requireContext())
            val color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))
            chip.chipBackgroundColor = ColorStateList.valueOf(color)
            chip.text = chipText
            chip.setTextColor(Color.WHITE)
            binding.chipGroup.addView(chip)
            chip.setOnLongClickListener {
                chip.isCloseIconVisible = true
                true
            }
            chip.setOnCloseIconClickListener {
                // Perform delete action
                (chip.parent as ViewGroup).removeView(chip)
                chipList.remove(chipText)
                addedNewHastag.remove(chipText)
                VideoFragment.mAdapter?.updateTags(pos!!, addedNewHastag)
                removeTag(chipText)
            }
        }
        binding.chipGroup.invalidate()
    }


    fun playVideo() {
        if (play) {
            Log.i(TAG, "play: $play")
            binding.ivPlay.visibility = View.INVISIBLE
            binding.ivPause.visibility = View.VISIBLE
            binding.vvVideo.start()
        } else {
            Log.i(TAG, "play: $play")
            binding.ivPlay.visibility = View.VISIBLE
            binding.ivPause.visibility = View.INVISIBLE
            binding.vvVideo.pause()
        }

    }

    fun removeTag(text: String) {
        Log.i(TAG, "removeTag: $text")
        var tag: TagCountModel? = null
        val list = arrayListOf<String>()
        tagRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                tag = dataSnapshot.getValue(TagCountModel::class.java)
                // Find the position of the item
                list.clear()
                tag?.tagList?.let { list.addAll(it) }
                val position = list.indexOf(text)
                Log.i(TAG, "TAGS list: $list ->pos: $position")
                if (position >= 0) {
                    list.removeAt(position)
                    Log.i(TAG, "TAGS list: $list ->pos: $position")
                    val newTag = TagCountModel(projectName, list.size, list)
                    tagRef?.setValue(newTag)
                    SnackBarUtils.showCustomSnackBar(binding.root, "tag deleted", false)
                    removeTagsFromProject(text)
                } else {
                    Log.i(TAG, "position not found")
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle error
            }
        })
    }

    fun removeTagsFromProject(text: String) {
        val database = Firebase.database
        var video: VideoModel? = null
        val list = arrayListOf<String>()
        val projectRef = projectName?.let {
            database.getReference("mediaTagging").child(userUid.toString())
                .child("projects")
                .child(it).child("videoList")
        }
        Log.i(TAG, "project ref: ${projectRef?.child(pos.toString())}")
        projectRef?.child(pos.toString())
            ?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    video = dataSnapshot.getValue(VideoModel::class.java)
                    // Find the position of the item
                    list.clear()
                    video?.addedTags?.let { list.addAll(it) }
                    val index = list.indexOf(text)
                    Log.i(TAG, "IMAGE list: $list ->pos: $index")
                    if (index >= 0) {
                        Log.i(TAG, "IMAGE list: $list ->pos: $index")
                        list.removeAt(index)
                        projectRef.child(pos.toString()).child("addedTags")
                            .setValue(list)
                    } else {
                        Log.i(TAG, "position not found")
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle error
                }
            })


    }

    private fun sendMsg(
        request: GptRequest,
        searchedWord: String,
        dialogBinding: SearchTagsBinding
    ) {
        Log.i(TAG, "sendMsg($request)")
        var ans: String? = ""

        dialogBinding.laHashtagLoadingAnimation.visibility = View.VISIBLE
        dialogBinding.laHashtagLoadingAnimation.repeatCount = ValueAnimator.INFINITE
        dialogBinding.laHashtagLoadingAnimation.playAnimation()
        RetrofitClient.api.getGptAnswer(request)
            .enqueue(object : Callback<GptResponse> {
                override fun onResponse(
                    call: Call<GptResponse>,
                    response: Response<GptResponse>
                ) {
                    // Handle the response
                    Log.i(TAG, "onResponse: $response")
                    if (response.isSuccessful) {
                        Log.i(TAG, "success")
                        dialogBinding.laHashtagLoadingAnimation.visibility = View.GONE
                        dialogBinding.laHashtagLoadingAnimation.cancelAnimation()
                        val gpt = response.body()
                        resList.clear()
                        hashtagSuggestions.clear()
                        gpt?.choices?.let { resList.addAll(it) }
                        ans = resList[0].text
                        ans = ans.toString().trim()
                        Log.i(TAG, "ans: $ans")
                        val delimiter1 = "\n"
                        val delimiter2 = " "
                        val delimiter3 = ","
                        val words =
                            ans?.split(delimiter1, delimiter2, delimiter3, ignoreCase = true)
                        Log.i(TAG, "words: $words")
                        for (word in words!!) {
                            if (word.startsWith("#")) {
                                hashtagSuggestions.add(word)
                            }
                        }
                        Log.i(TAG, "gptTagList: $hashtagSuggestions")
                        suggestHashtag(hashtagSuggestions, dialogBinding)

                    } else {
                        SnackBarUtils.showCustomSnackBar(binding.root, "Something went wrong", true)
                        dialogBinding.laHashtagLoadingAnimation.visibility = View.GONE
                        dialogBinding.laHashtagLoadingAnimation.cancelAnimation()
                        Log.i(
                            TAG,
                            "errorBody: ${response.errorBody()}, code: ${response.code()}, message: ${response.message()}, headers: ${response.headers()}, raw: ${response.raw()}, body: ${response.body()}"
                        )
                    }
                }

                override fun onFailure(call: Call<GptResponse>, t: Throwable) {
                    // Handle the error
                    Log.i(TAG, "failure")
                    Log.i(TAG, "$t")
                    ans = t.toString()
                }
            })
    }

    private fun getVideo(hashtags: ArrayList<String>) {
        Log.i("TAG", "getImage")

    }

    private fun updateSavedVideo(
        pos: Int? = -1,
        tagList: ArrayList<String>
    ) {
        //update saved project
        Log.i(TAG, "updateSavedVideo")
        if (tagList.size > 0) {
            Log.i(
                TAG, "projectRef: ${
                    projectRef?.child(pos.toString())?.child("addedTags")
                }"
            )
            projectRef?.child(pos.toString())?.child("addedTags")
                ?.setValue(tagList)?.addOnCompleteListener {
                    Log.i(TAG, "tag is added")
                    val video = VideoModel(
                        videoItem?.videoTitle,
                        videoItem?.videoUri,
                        tagList
                    )
                    pos?.let { it1 ->
                        VideoFragment.mAdapter?.updateImage(
                            video,
                            it1.toInt()
                        )
                    }
                    VideoFragment.videoList[pos!!] = video
                }

        } else {
            Log.i(TAG, "no tag")
        }
    }

    private fun saveTag(addedTag: String) {
        Log.i(TAG, "saveTag")
        val items: ArrayList<String> = arrayListOf()
        var tag: TagCountModel? = null
        tagRef?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                items.clear()
                tag = dataSnapshot.getValue(TagCountModel::class.java)
                tag?.let { items.addAll(it.tagList) }
                items.add(addedTag)
                val newTag = TagCountModel(projectName, items.size, items)
                tagRef?.setValue(
                    newTag
                )
                    ?.addOnCompleteListener {
                        Log.i(TAG, "tag is updated successfully")
                    }

            }

            override fun onCancelled(databaseError: DatabaseError) {
                // ...
            }
        })
    }

}