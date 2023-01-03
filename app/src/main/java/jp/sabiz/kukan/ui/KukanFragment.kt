package jp.sabiz.kukan.ui

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.progressindicator.CircularProgressIndicator
import jp.sabiz.kukan.R
import jp.sabiz.kukan.common.KukanState
import jp.sabiz.kukan.common.PermissionChecker
import jp.sabiz.kukan.databinding.FragmentKukanBinding
import jp.sabiz.kukan.location.LocationProvider


class KukanFragment : Fragment() {

    companion object {
        fun newInstance() = KukanFragment()
    }
    private lateinit var _binding: FragmentKukanBinding
    private lateinit var kukanViewModel: KukanViewModel

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
        kukanViewModel.kukanState.observe(viewLifecycleOwner) {
            if (it == KukanState.ON) {
                LocationProvider.get().requestRepeat(requireContext(), kukanViewModel)
            } else if (it == KukanState.OFF) {
                LocationProvider.get().stopRepeat(requireContext())
            }
        }
        PermissionChecker.get().check(requireActivity())
        LocationProvider.get().setup(requireActivity())
    }
}