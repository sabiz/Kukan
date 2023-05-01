package jp.sabiz.kukan.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.Resources.Theme
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ShareCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.material.snackbar.Snackbar
import com.techiness.progressdialoglibrary.ProgressDialog
import jp.sabiz.kukan.R
import jp.sabiz.kukan.common.Kiosk
import jp.sabiz.kukan.common.KukanState
import jp.sabiz.kukan.common.Logger
import jp.sabiz.kukan.common.PermissionChecker
import jp.sabiz.kukan.databinding.FragmentKukanBinding
import jp.sabiz.kukan.location.LocationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class KukanFragment : Fragment() {

    companion object {
        fun newInstance() = KukanFragment()
    }
    private lateinit var _binding: FragmentKukanBinding
    private lateinit var kukanViewModel: KukanViewModel
    private var lodingDialog: ProgressDialog? = null
    private val sendShare = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        lodingDialog?.dismiss()
        if (it.resultCode != Activity.RESULT_CANCELED) {
            val snackbar = Snackbar.make(_binding.main, "Upload done", Snackbar.LENGTH_INDEFINITE)
                .setAction("Delete local data") {
                    kukanViewModel.clearData()
                }
                .setActionTextColor(requireActivity().getColor(R.color.lcd_color))
                .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
            snackbar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_action).isAllCaps = false
            snackbar.show()
            kukanViewModel.viewModelScope.launch(Dispatchers.IO) {
                Thread.sleep(10 * 1000)
                kukanViewModel.viewModelScope.launch(Dispatchers.Main) {
                    snackbar.dismiss()
                }
            }
        } else {
            Snackbar.make(_binding.main, "Canceled", Snackbar.LENGTH_SHORT).show()
        }
    }
    private val kukanStateObserver: Observer<KukanState> = Observer {
        if (it == KukanState.ON) {
            LocationProvider.get().requestRepeat(requireContext(), kukanViewModel)
            Kiosk(requireContext()).start(requireActivity())
        } else if (it == KukanState.OFF) {
            LocationProvider.get().stopRepeat(requireContext())
            Kiosk(requireContext()).stop(requireActivity())
        }
    }
    private val isClickUploadObserver: Observer<Boolean> = Observer {
        if (!it) {
            return@Observer
        }
        val dialog = lodingDialog?: ProgressDialog(ProgressDialog.MODE_INDETERMINATE ,requireActivity(), ProgressDialog.THEME_DARK)
        with(dialog) {
            hideProgressText()
            isCancelable = false
            show()
        }
        lodingDialog = dialog
    }
    private val uploadDataObserver: Observer<String> = Observer {
        if(it == "") {
            return@Observer
        }
        val formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd-HHmmss")
        val dateTime = LocalDateTime.now()
        val dateTimeString: String = dateTime.format(formatter)
        val builder = ShareCompat.IntentBuilder(requireActivity())
        builder.setText(it)
            .setSubject("Kukan_$dateTimeString.csv")
            .setType("text/csv")
        sendShare.launch(builder.createChooserIntent())
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PermissionChecker.get().setupActivityResultLauncher(
            registerForActivityResult((ActivityResultContracts.RequestMultiplePermissions()), PermissionChecker.get()::activityResultCallback)
        )
        kukanViewModel = ViewModelProvider(this)[KukanViewModel::class.java]
        kukanViewModel.loadDb()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_kukan, container, false)
        _binding.viewModel = kukanViewModel
        _binding.lifecycleOwner = this
        val progress:ProgressBar = _binding.main.requireViewById(R.id.progress_on_off)
        progress.setOnTouchListener(kukanViewModel.longTouchEventDetector)
        return _binding.main
    }

    override fun onResume() {
        super.onResume()
        kukanViewModel.kukanState.observe(viewLifecycleOwner, kukanStateObserver)
        kukanViewModel.isClickUpload.observe(viewLifecycleOwner, isClickUploadObserver)
        kukanViewModel.uploadData.observe(viewLifecycleOwner, uploadDataObserver)
        PermissionChecker.get().check(requireActivity())
        LocationProvider.get().setup(requireActivity())
    }
}