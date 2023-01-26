package com.example.mediatagging.ui.home

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.activityViewModels
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import com.example.mediatagging.utils.FragmentController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.mediatagging.R
import com.example.mediatagging.databinding.AddProjectAlertDialogBinding
import com.example.mediatagging.databinding.FragmentHomeBinding
import com.example.mediatagging.databinding.ProfileAccountBinding
import com.example.mediatagging.model.ProjectModel
import com.example.mediatagging.model.TagCountModel
import com.example.mediatagging.ui.project.PhotosFragment
import com.example.mediatagging.ui.project.ProjectAdapter
import com.example.mediatagging.ui.tagScreen.TagsFragment
import com.example.mediatagging.ui.video.VideoFragment
import com.example.mediatagging.utils.constants.Constants
import com.example.mediatagging.utils.dialog.DialogUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*

class HomeFragment : BaseFragment() {
    lateinit var binding: FragmentHomeBinding
    val dashboardViewModel: DashboardViewModel by activityViewModels()
    var mAdapter: ProjectAdapter? = null
    val projectList: ArrayList<ProjectModel> = arrayListOf()
    private var gridLayoutManager: GridLayoutManager? = null
    private var projectRef: DatabaseReference? = null
    private var tagRef: DatabaseReference? = null
    var projectName: String = ""
    var user: FirebaseUser? = null
    var userUid: String? = ""
    val TAG = "HomeFragment"
    var project: ProjectModel? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(layoutInflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    fun noProject(){
        binding.llNoProject.visibility = View.VISIBLE
        binding.rvProjects.visibility = View.GONE
    }

    fun addProject(){

        binding.llNoProject.visibility = View.GONE
        binding.rvProjects.visibility = View.VISIBLE
    }

    fun initView() {
        user = FirebaseAuth.getInstance().currentUser
        userUid = user?.uid
        projectRef =
            FirebaseDatabase.getInstance().getReference("mediaTagging").child(userUid.toString())
                .child("projects")
        tagRef =
            FirebaseDatabase.getInstance().getReference("mediaTagging").child(userUid.toString())
                .child("tags")

        Log.i(TAG, "userUid: ${userUid}, projectRef: $projectRef")
        Log.i(TAG, "uid: $userUid")
        setListener()
        setupAdapter()
    }

    override fun onResume() {
        super.onResume()
        if(projectList.size>0){
            addProject()
        }else{
            noProject()
        }
        Log.i(TAG, "onResume")
        (activity as DashboardActivity).binding.toolbar.tvHeaderName.text = Constants.Home.value
        (activity as DashboardActivity).binding.toolbar.ivBack.visibility = View.INVISIBLE
        (activity as DashboardActivity).binding.toolbar.cvProfile.visibility = View.VISIBLE
        fetchProjects()
    }

    fun setupAdapter() {
        mAdapter = ProjectAdapter(requireContext(), { item, pos ->
            val bundle = Bundle()
            bundle.putSerializable(Constants.ProjectItem.value, item)
            bundle.putInt(Constants.Position.value, pos)
            FragmentController.setFragment(
                requireContext(),
                R.id.fragmentMain,
                bundle,
                true,
                PhotosFragment::class.java
            )
        }, { item, pos ->
            val bundle = Bundle()
            bundle.putSerializable(Constants.ProjectItem.value, item)
            bundle.putInt(Constants.Position.value, pos)
            FragmentController.setFragment(
                requireContext(),
                R.id.fragmentMain,
                bundle,
                true,
                VideoFragment::class.java
            )
        }, { view, project, pos ->
            val popupMenu = PopupMenu(requireContext(), view)
            popupMenu.inflate(R.menu.project_menu)
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.reNameProj -> {
                        project.projectName?.let {
                            showRenameProjectAlertDialog(
                                requireContext(), it, pos
                            )
                        }
                        true
                    }
                    R.id.removeProj -> {
                        project.projectName?.let { removeProject(it,pos) }
                        true
                    }
                    else -> super.onOptionsItemSelected(item)
                }
            }
            popupMenu.show()
        })

        val column = calNoOfColumns(requireContext())
        gridLayoutManager =
            GridLayoutManager(requireContext(), column, LinearLayoutManager.VERTICAL, false)
        binding.rvProjects.layoutManager = gridLayoutManager
        binding.rvProjects.setHasFixedSize(true)
        binding.rvProjects.adapter = mAdapter
    }

    private fun calNoOfColumns(context: Context): Int {
        val displayMetrics = context.resources.displayMetrics
        val dpWidth = displayMetrics.widthPixels / displayMetrics.density
        return (dpWidth / 180).toInt()
    }

    private fun setListener() {
        binding.fbAddProject.setOnClickListener {
            showAddProjectAlertDialog(requireContext())
        }

        (activity as DashboardActivity).binding.toolbar.cvProfile.setOnClickListener {
            val popupMenu = PopupMenu(requireContext(), it)
            popupMenu.inflate(R.menu.account_menu)
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.profile -> {
                        showProfileAlertDialog(requireContext())
                        true
                    }
                    R.id.tags -> {
                        FragmentController.setFragment(
                            requireContext(), R.id.fragmentMain, true, TagsFragment::class.java
                        )
                        true
                    }
                    R.id.logout -> {
                        FirebaseAuth.getInstance().signOut()
                        (activity as DashboardActivity).finish()
                        true
                    }
                    else -> super.onOptionsItemSelected(item)
                }
            }
            popupMenu.show()
        }
    }

    private fun showProfileAlertDialog(context: Context) {
        val dialog = AlertDialog.Builder(context)
        val dialogBinding = ProfileAccountBinding.inflate(LayoutInflater.from(context))
        dialogBinding.profile = dashboardViewModel.profile.value
        dialogBinding.viewmodel = dashboardViewModel
        Glide.with(this).load(dashboardViewModel.profile.value?.profileImg.toString())
            .apply(RequestOptions().centerCrop()).placeholder(R.drawable.profile)
            .into(dialogBinding.ivProfile)
        dialog.setView(dialogBinding.root)
        dialog.setCancelable(false)
        val alertDialog = dialog.create()
        alertDialog.show()
        dialogBinding.ivClose.setOnClickListener {
            alertDialog.dismiss()
        }
    }

    private fun showAddProjectAlertDialog(context: Context) {
        val dialog = AlertDialog.Builder(context)
        val dialogBinding = AddProjectAlertDialogBinding.inflate(LayoutInflater.from(context))
        dialog.setView(dialogBinding.root)
        dialogBinding.tvHeading.text = "Add Project"
        dialog.setCancelable(false)
        val alertDialog = dialog.create()
        alertDialog.show()
        dialogBinding.cvYes.setOnClickListener {
            projectName = dialogBinding.etProjectName.text.toString().trim()
            Log.i(TAG, "projectName: $projectName")
            if(projectName.isBlank()){
                projectName = "Untitled"
            }
            saveInfo(projectName)
            alertDialog.dismiss()
        }
        dialogBinding.cvNo.setOnClickListener {
            alertDialog.dismiss()
        }
    }


    private fun showRenameProjectAlertDialog(context: Context, oldProjectName: String, pos: Int) {
        val dialog = AlertDialog.Builder(context)
        val dialogBinding = AddProjectAlertDialogBinding.inflate(LayoutInflater.from(context))
        dialog.setView(dialogBinding.root)
        dialogBinding.tvHeading.text = "Rename Project"
        dialogBinding.etProjectName.setText(oldProjectName)
        dialog.setCancelable(false)
        val alertDialog = dialog.create()
        alertDialog.show()
        dialogBinding.cvYes.setOnClickListener {
            renameProject(oldProjectName, dialogBinding.etProjectName.text.toString().trim())
            alertDialog.dismiss()
        }
        dialogBinding.cvNo.setOnClickListener {
            alertDialog.dismiss()
        }
    }


    private fun fetchProjects() {
        //showProgress()
        projectRef?.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                //hideProgress()
                Toast.makeText(context, "$p0", Toast.LENGTH_SHORT).show()
            }

            override fun onDataChange(p0: DataSnapshot) {
                Log.i(TAG, "onDataChange")
                try {
                    if (p0.exists()) {
                        projectList.clear()
                        for (i in p0.children) {
                            project = i.getValue(ProjectModel::class.java)
                            project?.let { projectList.add(it) }
                        }
                        if(projectList.size>0){
                            addProject()
                        }
                        dashboardViewModel.totalProject.postValue(projectList.size)
                        mAdapter?.insertProjectList(projectList)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "exception: $e")
                }
            }

        })
    }

    /** save project **/
    private fun saveInfo(
        projectName: String
    ) {
        val projectCount = dashboardViewModel.totalProject.value
        projectRef?.child(projectName)?.setValue(
            ProjectModel(
                projectName, arrayListOf(), arrayListOf(), 0, 0
            )
        )?.addOnCompleteListener {
            Toast.makeText(
                requireContext(), "Your project is saved successfully ", Toast.LENGTH_SHORT
            ).show()
            dashboardViewModel.totalProject.postValue(projectCount?.plus(1))

        }
        createTag()
        addProject()
        projectList.add(ProjectModel(projectName, arrayListOf(), arrayListOf(), 0, 0))
        mAdapter?.insertProject(
            ProjectModel(projectName, arrayListOf(), arrayListOf(), 0, 0)
        )
    }

    private fun createTag() {
        Log.i(TAG, "createTag")
        tagRef?.child(projectName)?.setValue(
            TagCountModel(projectName, 0, arrayListOf())
        )?.addOnCompleteListener {
           Log.i(TAG,"Tag created")
        }

    }

    private fun renameProject(oldProjectName: String, newProjectName: String) {
        val oldRef = projectRef?.child(oldProjectName)
        val newRef = projectRef?.child(newProjectName)
        // First get all data at the old child location
        oldRef?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Store the data from the old child location
                val oldProject = dataSnapshot.getValue(ProjectModel::class.java)
                val newProject = ProjectModel(
                    newProjectName,
                    oldProject?.imageList,
                    oldProject?.videoList,
                    oldProject?.imageCount,
                    oldProject?.videoCount
                )
                // Now set the data at the new child location
                newRef?.setValue(newProject)
               // renameTags(oldProjectName,newProjectName)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.d(TAG, "Error: ${databaseError.message}")
            }
        })
        oldRef?.removeValue()
        projectName = newProjectName

    }

    fun renameTags(oldName:String, newName:String){
        Log.i(TAG,"updateTags($oldName,$newName)")
        val oldRef = tagRef?.child(oldName)
        val newRef = tagRef?.child(newName)
        // First get all data at the old child location
        Log.i(TAG,"oldRef: $oldRef")
        oldRef?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Store the data from the old child location
                val oldTag = dataSnapshot.getValue(TagCountModel::class.java)
                Log.i(TAG,"oldTag: $oldTag")
                val newTag = oldTag?.tagList?.let {
                    TagCountModel(
                        newName,
                        oldTag.count,
                        it
                    )
                }
                newRef?.setValue(newTag)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.d(TAG, "Error: ${databaseError.message}")
            }
        })

        oldRef?.removeValue()
    }

    fun removeProject(projectName: String,pos:Int) {
        projectList.removeAt(pos)
        DialogUtils.showCustomDialogColoredBtn(
            requireContext(),
            "Delete Project",
            "Are you sure you want to delete $projectName folder?", "Yes", "No", { dialog, _ ->
                val itemRef = projectRef?.child(projectName)
                itemRef?.removeValue()?.addOnCompleteListener {
                    if (it.isSuccessful) {
                        Log.d(TAG, "deleted successfully")
                        mAdapter?.removeProject(projectName)
                        Log.i(TAG,"projectList: $projectList")
                        if(projectList.size==0){
                            noProject()
                        }
                    } else {
                        Log.d(TAG, "deletion failed")
                    }
                }
                dialog.dismiss()
            }, { dialog, _ ->
                dialog.dismiss()
            }
        )

    }

}