package com.example.mediatagging.ui.image

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import com.example.mediatagging.R
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.mediatagging.ExtensionFunctionUtils.afterTextChanged
import com.example.mediatagging.databinding.FragmentImageBinding
import com.example.mediatagging.databinding.SearchTagsBinding
import com.example.mediatagging.model.Choices
import com.example.mediatagging.model.ImageModel
import com.example.mediatagging.model.TagCountModel
import com.example.mediatagging.model.request.GptRequest
import com.example.mediatagging.model.response.GptResponse
import com.example.mediatagging.retrofit.RetrofitClient
import com.example.mediatagging.ui.home.DashboardActivity
import com.example.mediatagging.ui.project.ImageViewModel
import com.example.mediatagging.ui.project.PhotosFragment
import com.example.mediatagging.ui.video.VideoFragment
import com.example.mediatagging.utils.SnackBarUtils
import com.example.mediatagging.utils.StorageManagerKeys
import com.example.mediatagging.utils.StorageManagerUtil
import com.example.mediatagging.utils.constants.Constants
import com.google.android.material.chip.Chip
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import kotlin.collections.ArrayList


class ImageFragment : Fragment() {
    lateinit var binding: FragmentImageBinding
    val TAG = "ImageFragment"
    var imageName: String? = ""
    var imageUrl: String? = ""
    var userUid: String? = ""
    var projectName: String = ""
    var selectedImage: ImageModel? = null
    var tagRef: DatabaseReference? = null
    var pos: Int? = -1
    var addedNewHastag: ArrayList<String> = arrayListOf()
    var tagList: ArrayList<TagCountModel> = arrayListOf()
    var tag: TagCountModel? = null
    private var projectRef: DatabaseReference? = null
    var hashtagSuggestions: ArrayList<String> = arrayListOf()
    var resList: ArrayList<Choices> = arrayListOf()
    var request: GptRequest? = null
    var chosenImage: ImageModel? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentImageBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    fun initView() {
        Log.i(TAG, "initView")
        userUid = StorageManagerUtil.getInstance(context)
            .getPreference<String>(StorageManagerKeys.USER_UID.name).toString()
        Log.i(TAG, "uid: $userUid")
        handleData()
        setListener()

    }

    fun setListener() {
        binding.tvAddTags.setOnClickListener {
            showAddTagAlertDialog(requireContext())
        }

    }

    override fun onPause() {
        super.onPause()
        (activity as DashboardActivity).binding.toolbar.tvHeaderName.text = projectName + " images"
        (activity as DashboardActivity).binding.toolbar.ivOptions.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onResume")
        (activity as DashboardActivity).binding.toolbar.ivOptions.visibility = View.GONE
        (activity as DashboardActivity).binding.toolbar.tvHeaderName.text =
            chosenImage?.imageTitle
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
                getImage(chip.text.toString())
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
                PhotosFragment.mAdapter?.updateTags(pos!!, addedNewHastag)
                removeTag(chipText)
            }
            chip.setOnClickListener {
                chip.isCloseIconVisible = false
            }
        }
        binding.chipGroup.invalidate()
    }

    fun handleData() {
        if (arguments != null) {
            addedNewHastag = arrayListOf<String>()
            chosenImage = arguments?.getSerializable(Constants.Image.value) as ImageModel?
            imageName = arguments?.getString("projectName")!!
            imageUrl = chosenImage?.imageUri
            pos = arguments?.getInt(Constants.Position.value)
            projectName = arguments?.getString("projectName")!!
            chosenImage?.addedTags?.let { addedNewHastag.addAll(it) }
            Log.i(TAG, "imageUrl: $imageUrl")
            setHashtags(addedNewHastag)
            Log.i(TAG, "addedTags: $addedNewHastag")
        }
        imageUrl?.let { downloadImage(it) }
        tagRef = FirebaseDatabase.getInstance()
            .getReference("mediaTagging").child(userUid.toString()).child("tags").child(projectName)
    }


    private fun fetchTag() {
        Log.i(TAG, "fetchTag")
        tagRef?.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                //hideProgress()
                Toast.makeText(context, "$p0", Toast.LENGTH_SHORT).show()
            }

            override fun onDataChange(p0: DataSnapshot) {
                Log.i(TAG, "onDataChange")
                try {
                    if (p0.exists()) {
                        tag = p0.getValue(TagCountModel::class.java)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "exception: $e")
                }
            }

        })

    }


    fun downloadImage(uri: String) {
        Log.i(TAG, "photoUrl: $uri")
        Glide.with(requireContext()).load(uri).placeholder(R.drawable.flower_placeholder)
            .into(binding.ivImage)
    }

    private fun getImage(hashtag: String?) {
        Log.i("TAG", "getImage")
        val database = Firebase.database
        projectRef =
            projectName?.let {
                database.getReference("mediaTagging").child(userUid.toString())
                    .child("projects")
                    .child(it).child("imageList")
            }
        Log.i(TAG, "ref: ${projectRef?.child(pos.toString())}")
        projectRef?.child(pos.toString())
            ?.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    selectedImage = dataSnapshot.getValue(ImageModel::class.java)
                    // Use the value
                    Log.i(TAG, "selectedImage: $selectedImage")
                    if (hashtag != null) {
                        addedNewHastag.add(hashtag)
                        updateSavedImage(projectName, pos, addedNewHastag, selectedImage)
                        saveTag(hashtag)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })

    }

    private fun updateSavedImage(
        projectName: String?,
        pos: Int? = -1,
        tagList: ArrayList<String>,
        selectedImage: ImageModel?
    ) {
        //update saved project
        Log.i(TAG, "updateSavedImage")
        if (tagList.size > 0) {
            val updateImage = ImageModel(
                "${Constants.Image.value}${pos?.toInt()?.plus(1)}",
                selectedImage?.imageUri,
                tagList
            )
            projectRef?.child(pos.toString())
                ?.setValue(updateImage)
                ?.addOnCompleteListener {
                    Log.i(TAG, "tag is added")
                    val image = ImageModel(
                        selectedImage?.imageTitle,
                        selectedImage?.imageUri,
                        tagList
                    )
                    pos?.let { it1 ->
                        PhotosFragment.mAdapter?.updateImage(
                            image,
                            it1.toInt()
                        )
                    }
                }

        } else {
            Log.i(TAG, "no tag")
        }
    }

    fun fetchTags(projectName: String?) {
        Log.i(TAG, "fetchTags")
        if (projectName != null) {
            tagRef?.addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    //hideProgress()
                    Toast.makeText(context, "$p0", Toast.LENGTH_SHORT).show()
                }

                override fun onDataChange(p0: DataSnapshot) {
                    Log.i(TAG, "onDataChange")
                    try {
                        if (p0.exists()) {
                            tagList.clear()
                            for (i in p0.children) {
                                tag = i.getValue(TagCountModel::class.java)
                                tag?.let { tagList.add(it) }
                            }
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "exception: $e")
                    }
                }

            })
        }
    }


    private fun saveTag(addedTag: String) {
        Log.i(TAG, "createTag")
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
        var image: ImageModel? = null
        val list = arrayListOf<String>()
        val projectRef = projectName?.let {
            database.getReference("mediaTagging").child(userUid.toString())
                .child("projects")
                .child(it).child("imageList")
        }
        Log.i(TAG, "project ref: ${projectRef?.child(pos.toString())}")
        projectRef?.child(pos.toString())
            ?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    image = dataSnapshot.getValue(ImageModel::class.java)
                    // Find the position of the item
                    list.clear()
                    image?.addedTags?.let { list.addAll(it) }
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

}