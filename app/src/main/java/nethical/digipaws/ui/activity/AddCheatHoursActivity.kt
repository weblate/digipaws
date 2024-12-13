package nethical.digipaws.ui.activity

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import nethical.digipaws.R
import nethical.digipaws.databinding.ActivityAddCheatHoursActivityBinding
import nethical.digipaws.databinding.CheatHourItemBinding
import nethical.digipaws.databinding.DialogAddToCheatHoursBinding
import nethical.digipaws.utils.SavedPreferencesLoader
import nethical.digipaws.utils.TimeTools
import java.util.Calendar

class AddCheatHoursActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddCheatHoursActivityBinding
    private val savedPreferencesLoader = SavedPreferencesLoader(this)
    private var cheatHoursList: MutableList<CheatHourItem> = mutableListOf()

    private lateinit var selectUnblockedAppsLauncher: ActivityResultLauncher<Intent>
    private var selectedUnblockedApps: ArrayList<String>? = arrayListOf()

    private lateinit var dialogAddToCheatHoursBinding: DialogAddToCheatHoursBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAddCheatHoursActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        dialogAddToCheatHoursBinding = DialogAddToCheatHoursBinding.inflate(layoutInflater)
        cheatHoursList = savedPreferencesLoader.loadCheatHoursList()
        selectUnblockedAppsLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val selectedApps = result.data?.getStringArrayListExtra("SELECTED_APPS")
                    selectedApps?.let {
                        selectedUnblockedApps = selectedApps
                        dialogAddToCheatHoursBinding.btnSelectUnblockedApps.text =
                            getString(R.string.app_s_selected, selectedApps.size)
                    }
                }
            }

        binding.recyclerView2.layoutManager = LinearLayoutManager(this)
        binding.recyclerView2.adapter = CheatHourAdapter(cheatHoursList)
        binding.button.setOnClickListener {
            makeCheatHoursDialog()
        }
    }

    private fun makeCheatHoursDialog() {


        var endTimeInMins: Int? = null
        var startTimeInMins: Int? = null

        dialogAddToCheatHoursBinding.btnSelectEndTime.setOnClickListener {
            if (startTimeInMins == null) {
                Toast.makeText(
                    this,
                    getString(R.string.please_specify_start_time_first),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePickerDialog = TimePickerDialog(
                this,
                { _, selectedHour, selectedMinute ->
                    val selectedEndTime =
                        TimeTools.convertToMinutesFromMidnight(selectedHour, selectedMinute)

                    // Ensure end time is after start time
                    if (startTimeInMins != null && selectedEndTime <= startTimeInMins!!) {
                        Toast.makeText(
                            this,
                            getString(R.string.end_time_must_be_after_start_time_has_passed),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        endTimeInMins = selectedEndTime
                        dialogAddToCheatHoursBinding.btnSelectEndTime.text =
                            getString(R.string.end_time, selectedHour, selectedMinute)
                    }
                },
                hour,
                minute,
                false
            )
            timePickerDialog.show()
        }
        dialogAddToCheatHoursBinding.btnSelectStartTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePickerDialog = TimePickerDialog(
                this,
                { _, selectedHour, selectedMinute ->
                    startTimeInMins =
                        TimeTools.convertToMinutesFromMidnight(selectedHour, selectedMinute)
                    dialogAddToCheatHoursBinding.btnSelectStartTime.text =
                        getString(R.string.start_time_02d_02d, selectedHour, selectedMinute)
                },
                hour,
                minute,
                false // Use 24-hour format
            )
            timePickerDialog.show()
        }
        dialogAddToCheatHoursBinding.btnSelectUnblockedApps.setOnClickListener {
            val intent = Intent(this, SelectAppsActivity::class.java)
            intent.putStringArrayListExtra(
                "PRE_SELECTED_APPS",
                selectedUnblockedApps
            )
            intent.putStringArrayListExtra(
                "APP_LIST",
                java.util.ArrayList(savedPreferencesLoader.loadBlockedApps())
            )
            selectUnblockedAppsLauncher.launch(intent)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.specify_cheat_hours))
            .setView(dialogAddToCheatHoursBinding.root)
            .setPositiveButton(getString(R.string.add)) { dialog, _ ->
                if (dialogAddToCheatHoursBinding.cheatHourTitle.text?.isEmpty() == true) {
                    Toast.makeText(
                        this,
                        getString(R.string.please_type_a_title),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                if (startTimeInMins == null) {
                    Toast.makeText(
                        this,
                        getString(R.string.please_select_a_start_time), Toast.LENGTH_SHORT
                    ).show()
                } else if (endTimeInMins == null) {
                    Toast.makeText(
                        this,
                        getString(R.string.please_select_an_end_time), Toast.LENGTH_SHORT
                    ).show()
                } else if (selectedUnblockedApps?.isEmpty() == true) {
                    Toast.makeText(
                        this,
                        getString(R.string.please_select_a_few_apps_to_block), Toast.LENGTH_SHORT
                    )
                        .show()
                } else {
                    cheatHoursList.add(
                        CheatHourItem(
                            dialogAddToCheatHoursBinding.cheatHourTitle.text.toString(),
                            startTimeInMins!!,
                            endTimeInMins!!,
                            selectedUnblockedApps!!,
                            dialogAddToCheatHoursBinding.cbDisableProceed.isChecked
                        )
                    )
                    binding.recyclerView2.adapter?.notifyItemInserted(cheatHoursList.size)
                    savedPreferencesLoader.saveCheatHoursList(cheatHoursList)
                    dialog.dismiss()
                }

            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }


    inner class CheatHourAdapter(
        private val items: List<CheatHourItem>
    ) : RecyclerView.Adapter<CheatHourAdapter.CheatHourViewHolder>() {

        inner class CheatHourViewHolder(private val binding: CheatHourItemBinding) :
            RecyclerView.ViewHolder(binding.root) {

            @SuppressLint("SetTextI18n")
            fun bind(item: CheatHourItem) {
                binding.cheatHourTitle.text = item.title
                val convertedStartTime = TimeTools.convertMinutesTo24Hour(item.startTime)
                val convertedEndTIme = TimeTools.convertMinutesTo24Hour(item.endTime)

                binding.removeCheatHour.setOnClickListener {
                    cheatHoursList.removeAt(layoutPosition)
                    notifyItemRemoved(layoutPosition)
                    savedPreferencesLoader.saveCheatHoursList(cheatHoursList)
                }

                binding.cheatTimings.text =
                    getString(
                        R.string.cheat_timings,
                        convertedStartTime.first,
                        convertedStartTime.second,
                        convertedEndTIme.first,
                        convertedEndTIme.second
                    )
                item.packages.forEach { packageName ->
                    binding.selectedApps.text =
                        binding.selectedApps.text.toString() + " " + packageName
                }

            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CheatHourViewHolder {
            val binding = CheatHourItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return CheatHourViewHolder(binding)
        }

        override fun onBindViewHolder(holder: CheatHourViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size
    }


    data class CheatHourItem(
        val title: String,
        val startTime: Int,
        val endTime: Int,
        val packages: ArrayList<String>,
        val isProceedHidden: Boolean = false
    )

}