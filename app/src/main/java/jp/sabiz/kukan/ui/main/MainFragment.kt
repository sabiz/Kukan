package jp.sabiz.kukan.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import jp.sabiz.kukan.R

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        // TODO: Use the ViewModel

        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            val deniedPermissions = emptyList<String>().toMutableList()
            it.keys.forEach { key ->
                if (it[key] == false) {
                    deniedPermissions.add(key)
                }
            }
            if (deniedPermissions.size > 0) {
                requestPermissionLauncher.launch(deniedPermissions.toTypedArray())
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onResume() {
        super.onResume()
        val permissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

        val showRequestPermission = emptyList<String>().toMutableList()


        permissions.forEach {
            when {
                context?.let { ctx ->
                    ContextCompat.checkSelfPermission(ctx, it)
                } == PackageManager.PERMISSION_DENIED -> {
                    // TODO message
                }
                activity?.let { act -> ActivityCompat.shouldShowRequestPermissionRationale(act, it) } == true -> {
                    showRequestPermission.add(it)
                }
            }
        }
        if (showRequestPermission.size > 0) {
            // TODO
        }

    }
}