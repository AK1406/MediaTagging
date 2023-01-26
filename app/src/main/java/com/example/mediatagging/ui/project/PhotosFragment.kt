package com.example.mediatagging.ui.project

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mediatagging.ui.image.ImageFragment
import com.example.mediatagging.R
import com.example.mediatagging.databinding.FragmentPhotosBinding
import com.example.mediatagging.model.ImageModel
import com.example.mediatagging.model.ProjectModel
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
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class PhotosFragment : Fragment() {
    private lateinit var binding: FragmentPhotosBinding
    private lateinit var imageViewModel: ImageViewModel
    val TAG = "PhotosFragment"

    var imageList: ArrayList<ImageModel> = arrayListOf()
    private var gridLayoutManager: GridLayoutManager? = null
    var projectName: String = ""
    var projectPos: Int = -1
    private var imagesRef: StorageReference? = null
    private var projectRef: DatabaseReference? = null
    var user: FirebaseUser? = null
    var userUid: String? = ""
    val storage = Firebase.storage
    val PICK_IMAGE_REQUEST = 100
    val REQUEST_IMAGE_CAPTURE = 101
    var storageRef: StorageReference? = null
    var projectItem: ProjectModel? = null
    var projectItemPos: Int = -1
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentPhotosBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    companion object {
        var mAdapter: PhotosAdapter? = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        initView()
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onResume")
        (activity as DashboardActivity).binding.toolbar.ivBack.visibility = View.VISIBLE
        (activity as DashboardActivity).binding.toolbar.tvHeaderName.text = projectName + " images"
        (activity as DashboardActivity).binding.toolbar.cvProfile.visibility = View.INVISIBLE
        (activity as DashboardActivity).binding.toolbar.ivOptions.visibility = View.VISIBLE
    }

    private fun initView() {
        Log.i(TAG, "initView")
        noImage()
        setupViewModel()
        user = FirebaseAuth.getInstance().currentUser
        userUid = user?.uid
        StorageManagerUtil.getInstance(context).savePreference(
            StorageManagerKeys.USER_UID.name, userUid
        )
        Log.i(TAG, "userUid: $userUid")
        projectRef =
            FirebaseDatabase.getInstance().getReference("mediaTagging").child(userUid.toString())
                .child("projects")
        setObservers()
        setupAdapter()
        handleData()
        setListener()
    }

    fun setupViewModel() {
        imageViewModel = ViewModelProvider(this).get(ImageViewModel::class.java)
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
                        showAlertUploadDialog()
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

    private fun handleData() {
        if (arguments != null) {
            projectItem = arguments?.getSerializable(Constants.ProjectItem.value) as ProjectModel
            projectItemPos = arguments?.getInt(Constants.Position.value)!!
        }
        projectName = projectItem?.projectName.toString()
        projectPos = projectItemPos
        imageList.clear()
        val list = imageViewModel.imageListResult.value
        if ((list?.size ?: 0) > 0) {
            Log.i(TAG, "imageLista: ${imageList.size}")
            list?.let { imageList.addAll(it) }
        } else {
            Log.i(TAG, "imageListb: ${imageList.size}")
            projectItem?.imageList?.let { imageList.addAll(it) }
        }
        if (imageList.size > 0) {
            imageAdded()
            Log.i(TAG, "imageList: $imageList")
            imageViewModel.setImageList(imageList)
            // mAdapter?.insertImageList(imageList)
        } else {
            noImage()
        }
        storageRef = storage.reference
        imagesRef =
            storageRef?.child("$userUid")?.child("projects")?.child(projectName)?.child("photos")
    }

    fun noImage() {
        binding.llNoImage.visibility = View.VISIBLE
        binding.rvImages.visibility = View.GONE
    }

    fun imageAdded() {
        binding.llNoImage.visibility = View.GONE
        binding.rvImages.visibility = View.VISIBLE
    }

    fun setupAdapter() {
        Log.i(TAG, "setupAdapter")
        mAdapter = PhotosAdapter(requireContext(), { item, pos ->
            val bundle = Bundle()
            bundle.putSerializable(Constants.Image.value, item)
            bundle.putString("projectName", projectName)
            bundle.putInt("pos", pos)
            FragmentController.setFragmentOn(
                requireContext(), R.id.fragmentMain, bundle, true, ImageFragment::class.java
            )
        }, { pos, deleteIcon ->
            removeImage(pos, deleteIcon)
        })
        val column = calNoOfColumns(requireContext())
        gridLayoutManager =
            GridLayoutManager(requireContext(), column, LinearLayoutManager.VERTICAL, false)
        binding.rvImages.layoutManager = gridLayoutManager
        binding.rvImages.setHasFixedSize(true)
        binding.rvImages.adapter = mAdapter
    }

    private fun calNoOfColumns(context: Context): Int {
        val displayMetrics = context.resources.displayMetrics
        val dpWidth = displayMetrics.widthPixels / displayMetrics.density
        return (dpWidth / 180).toInt()
    }


    fun showAlertUploadDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("upload Image")
        builder.setMessage("Choose any option to proceed")
        builder.setPositiveButton("Camera") { _, _ ->
            checkCameraPermission()
        }
        builder.setNeutralButton("Cancel") { dialogInterface, _ ->
            dialogInterface.dismiss()
        }
        builder.setNegativeButton("Gallery") { _, _ ->
            uploadImages()
        }

        val alertDialog: AlertDialog = builder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    private fun capturePictureIntent() {
        Log.i(TAG, "capturePictureIntent")
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(requireActivity().packageManager)?.also {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE)
    }

    private fun uploadImages() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted
            if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle("Camera Permission Needed")
                builder.setMessage("This app needs the Camera permission, please accept to use camera functionality")
                builder.setPositiveButton("OK") { _, _ ->
                    requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_IMAGE_CAPTURE)
                }
                builder.setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                builder.show()
            } else {
                // No explanation needed, we can request the permission.
                requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_IMAGE_CAPTURE)
            }
        } else {
            // Permission has already been granted
            openCamera()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_IMAGE_CAPTURE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted
                    openCamera()
                } else {
                    // permission denied
                    val builder = AlertDialog.Builder(requireContext())
                    builder.setTitle("Permission Denied")
                    builder.setMessage("Without camera permission, the app can't take picture")
                    builder.setPositiveButton("OK") { _, _ -> }
                    builder.show()
                }
                return
            }
            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if ((requestCode == REQUEST_IMAGE_CAPTURE || requestCode == PICK_IMAGE_REQUEST) && resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                val imageUri = data?.extras?.get("data") as Bitmap
                if (imageUri != null) {
                    uploadImageAndSaveUri(imageUri)
                } else {
                    Log.i(TAG, "imageUri : $imageUri")
                }
            } else {
                val imageUri = data?.data
                val imageRef = imagesRef?.child(imageUri?.lastPathSegment!!)
                val uploadTask = imageRef?.putFile(imageUri!!)
                var image = ImageModel(
                    "${Constants.Image.value}${imageList.size + 1}",
                    imageUri.toString(),
                    arrayListOf()
                )
                mAdapter?.insertImage(image)
                image.playAnimation = true
                uploadTask?.addOnProgressListener { taskSnapshot ->
                    imageAdded()
                    val progress =
                        (100.0 * taskSnapshot.bytesTransferred) / taskSnapshot.totalByteCount
                    if (progress == 100.0) {
                        val Imagename = taskSnapshot.metadata?.name
                        Log.i(TAG, "name:$Imagename")
                        image.playAnimation = false
                        Log.i(TAG, "imageUri: $imageUri")
                        SnackBarUtils.showCustomSnackBar(binding.root, "Photo uploaded", false)
                        image = ImageModel(
                            "${Constants.Image.value}${imageList.size + 1}",
                            imageUri.toString(),
                            arrayListOf()
                        )
                        Log.i(TAG, "URi path: ${imageUri?.path}")
                        Imagename?.let {
                            fetchStorageImages(
                                it,
                                "${Constants.Image.value}${imageList.size + 1}"
                            )
                        }
                    }
                }

                Log.i(TAG, "imageListSize: ${imageList.size}")
                updateSavedProject(projectName, imageList)

                uploadTask?.addOnFailureListener { exception ->
                    Log.i(TAG, "Upload failed: $exception")
                }
            }
        }
    }


    private fun uploadImageAndSaveUri(imageUri: Bitmap) {
        Log.i(TAG, "uploadImageAndSaveUri")
        val baos = ByteArrayOutputStream()
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageName = "$timeStamp.jpg"
        val uploadedImageRef = imagesRef?.child(imageName)
        imageUri.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val capturedImage = baos.toByteArray()
        val uploadTask = uploadedImageRef?.putBytes(capturedImage)

        var image = ImageModel(
            "${Constants.Image.value}${imageList.size + 1}",
            imageUri.toString(),
            arrayListOf()
        )
        mAdapter?.insertImage(image)
        image.playAnimation = true

        uploadTask?.addOnCompleteListener { taskSnapshot ->
            if (taskSnapshot.isSuccessful) {
                image.playAnimation = false
                Log.i(TAG, "imageUri: $imagesRef")
                SnackBarUtils.showCustomSnackBar(binding.root, "Photo uploaded", false)
                image = ImageModel(
                    "${Constants.Image.value}${imageList.size + 1}",
                    imagesRef.toString(),
                    arrayListOf()
                )
                Log.i(TAG, "URi path: ${imagesRef?.path}")
                Log.i(TAG, "imageName: $imageName")
                fetchStorageImages(imageName, "${Constants.Image.value}${imageList.size + 1}")

            } else {
                uploadTask.exception?.let {
                    Log.i(TAG, "exception-> $it")
                }
            }
        }

        /*uploadTask?.addOnProgressListener { taskSnapshot ->
            imageAdded()
            val progress =
                (100.0 * taskSnapshot.bytesTransferred) / taskSnapshot.totalByteCount
            if (progress == 100.0) {
                val imageName = taskSnapshot.metadata?.name
                image.playAnimation = false
                Log.i(TAG, "imageUri: $imagesRef")
                SnackBarUtils.showCustomSnackBar(binding.root, "Photo uploaded", false)
                image = ImageModel(
                    "${Constants.Image.value}${imageList.size + 1}",
                    imagesRef.toString(),
                    arrayListOf()
                )
                Log.i(TAG, "URi path: ${imagesRef?.path}")
                Log.i(TAG,"imageName: $imageName")
                imageName?.let { fetchStorageImages(it,"${Constants.Image.value}${imageList.size + 1}") }
            }
        }*/

    }

    private fun removeImage(pos: Int, deleteIcon: View) {
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
                            imageList.clear()
                            project = p0.getValue(ProjectModel::class.java)
                            Log.i(TAG, "ptoject: $project")
                            project?.imageList?.let { imageList.addAll(it) }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "exception: $e")
                    }
                }
            })
            withContext(Dispatchers.Main) {
                if (pos >= 0) {
                    Log.i(TAG, "pso: $pos -> ${imageList.size}")
                    imageList.removeAt(pos)
                    Log.i(TAG, "imageList: $imageList")
                    if (imageList.size == 0) {
                        noImage()
                    } else {
                        deleteIcon.visibility = View.GONE
                        mAdapter?.insertImageList(imageList)
                    }
                }
                projectRef?.child(projectName)
                    ?.setValue(
                        ProjectModel(
                            projectName,
                            imageList,
                            project?.videoList,
                            imageList.size,
                            project?.videoList?.size
                        )
                    )
                    ?.addOnCompleteListener {
                        SnackBarUtils.showCustomSnackBar(
                            binding.root,
                            "image removed",
                            false
                        )
                    }
            }
        }
    }

    private fun fetchStorageImages(imageName: String, imageTitle: String) {
        Log.i(TAG, "fetchStorageImages")
        Log.i(TAG, "imageName: $imageName")
        val imageRef = storageRef?.child(userUid.toString())?.child("projects")?.child(projectName)
            ?.child("photos")
        Log.i(TAG, "imageRef: ${imageRef?.child(imageName)}")
        imageRef?.child(imageName)?.downloadUrl?.addOnSuccessListener { imageUri ->
            // Use the bytes to display the image
            val image = ImageModel(
                imageTitle,
                imageUri.toString(),
                arrayListOf()
            )
            imageList.add(
                image
            )
            mAdapter?.updateImage(image, imageList.size - 1)
            updateSavedProject(projectName, imageList)
        }?.addOnFailureListener {
            Log.i(TAG, "exception: $it")
        }
    }

    fun setObservers() {
        imageViewModel.imageListResult.observe(viewLifecycleOwner, Observer { list ->
            mAdapter?.insertImageList(list)
        })
    }


    private fun updateSavedProject(
        projectName: String, imageList2: ArrayList<ImageModel>
    ) {
        //update saved project
        var project: ProjectModel? = null
        Log.i(TAG, "updateSavedProject")
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
                            project = p0.getValue(ProjectModel::class.java)
                            Log.i(TAG, "project: $project")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "exception: $e")
                    }
                }
            })
            withContext(Dispatchers.IO) {
                if (imageList.size > 0) {
                    projectRef?.child(projectName)?.child("imageList")
                        ?.setValue(
                            imageList
                        )
                        ?.addOnCompleteListener {
                            Log.i(TAG, "$projectName is updated successfully ")
                            //imageViewModel.setImageList(imageList)
                        }
                    projectRef?.child(projectName)?.child("imageCount")
                        ?.setValue(
                            imageList.size
                        )
                } else {
                    Log.i(TAG, "no image")
                }
            }
        }
    }
}