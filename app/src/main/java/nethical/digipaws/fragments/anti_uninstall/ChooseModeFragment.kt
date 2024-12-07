package nethical.digipaws.fragments.anti_uninstall

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import nethical.digipaws.R
import nethical.digipaws.databinding.FragmentChoseAntiUninstallModeBinding

class ChooseModeFragment : Fragment() {

    private var _binding: FragmentChoseAntiUninstallModeBinding? = null
    private val binding get() = _binding!!  // Safe getter for binding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChoseAntiUninstallModeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnNext.setOnClickListener {
            when (binding.radioGroup.checkedRadioButtonId) {
                binding.passMode.id -> {
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.anti_uninstall_fragment_holder,
                            SetupPasswordModeFragment()
                        ) // Replace with FragmentB
                        .addToBackStack(null)
                        .commit()
                }

                binding.timedMode.id -> {
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.anti_uninstall_fragment_holder,
                            SetupTimedModeFragment()
                        ) // Replace with FragmentB
                        .addToBackStack(null)
                        .commit()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
