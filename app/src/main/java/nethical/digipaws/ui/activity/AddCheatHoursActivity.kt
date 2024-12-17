package nethical.digipaws.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
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
import nl.joery.timerangepicker.TimeRangePicker

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

    @SuppressLint("ClickableViewAccessibility")
    private fun makeCheatHoursDialog() {


        dialogAddToCheatHoursBinding = DialogAddToCheatHoursBinding.inflate(layoutInflater)

        dialogAddToCheatHoursBinding.picker.startTime = TimeRangePicker.Time(6, 30)
        dialogAddToCheatHoursBinding.picker.endTime = TimeRangePicker.Time(22, 0)

        dialogAddToCheatHoursBinding.picker.hourFormat = TimeRangePicker.HourFormat.FORMAT_24
        var endTimeInMins: Int? = dialogAddToCheatHoursBinding.picker.endTimeMinutes
        var startTimeInMins: Int? = dialogAddToCheatHoursBinding.picker.startTimeMinutes

        dialogAddToCheatHoursBinding.picker.setOnTouchListener { v, event ->
            // Disable ScrollView's touch interception when interacting with the picker
            when (event.action) {
                MotionEvent.ACTION_DOWN -> dialogAddToCheatHoursBinding.scrollview.requestDisallowInterceptTouchEvent(
                    true
                )

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> dialogAddToCheatHoursBinding.scrollview.requestDisallowInterceptTouchEvent(
                    false
                )
            }
            v.onTouchEvent(event) // Pass the event to the picker
        }
        dialogAddToCheatHoursBinding.picker.setOnTimeChangeListener(object :
            TimeRangePicker.OnTimeChangeListener {
            override fun onStartTimeChange(startTime: TimeRangePicker.Time) {
                dialogAddToCheatHoursBinding.fromTime.text =
                    dialogAddToCheatHoursBinding.picker.startTime.toString()
                startTimeInMins = dialogAddToCheatHoursBinding.picker.startTimeMinutes
            }

            override fun onEndTimeChange(endTime: TimeRangePicker.Time) {
                dialogAddToCheatHoursBinding.endTime.text =
                    dialogAddToCheatHoursBinding.picker.endTime.toString()
                endTimeInMins = dialogAddToCheatHoursBinding.picker.endTimeMinutes
            }

            override fun onDurationChange(duration: TimeRangePicker.TimeDuration) {
            }
        })

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
            .setView(dialogAddToCheatHoursBinding.root)
            .setPositiveButton(getString(R.string.add)) { dialog, _ ->
                if (dialogAddToCheatHoursBinding.cheatHourTitle.text?.isEmpty() == true) {
                    Toast.makeText(
                        this,
                        getString(R.string.please_type_a_title),
                        Toast.LENGTH_SHORT
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
                            selectedUnblockedApps!!
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