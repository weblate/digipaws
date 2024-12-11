package nethical.digipaws.fragments.anti_uninstall

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import nethical.digipaws.Constants
import nethical.digipaws.databinding.FragmentSetupPasswordModeBinding
import nethical.digipaws.services.DigipawsMainService

class SetupPasswordModeFragment : Fragment() {

    private var _binding: FragmentSetupPasswordModeBinding? = null
    private val binding get() = _binding!!  // Safe getter for binding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetupPasswordModeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnNextPass.setOnClickListener {
            val editor =
                activity?.getSharedPreferences("anti_uninstall", Context.MODE_PRIVATE)?.edit()
            editor?.apply() {
                putBoolean("is_anti_uninstall_on", true)
                putString("password", binding.password.text.toString())
                putInt("mode", Constants.ANTI_UNINSTALL_PASSWORD_MODE)
                commit()
            }

            val intent = Intent(DigipawsMainService.INTENT_ACTION_REFRESH_ANTI_UNINSTALL)
            activity?.sendBroadcast(intent)

            activity?.finish()

        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}