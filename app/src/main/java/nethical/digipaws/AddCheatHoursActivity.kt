package nethical.digipaws

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import nethical.digipaws.databinding.ActivityAddCheatHoursActivityBinding
import nethical.digipaws.databinding.CheatHourItemBinding
import nethical.digipaws.databinding.DialogAddToCheatHoursBinding
import nethical.digipaws.utils.SavedPreferencesLoader
import nethical.digipaws.utils.Tools
import java.util.Calendar

class AddCheatHoursActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddCheatHoursActivityBinding
    private val savedPreferencesLoader = SavedPreferencesLoader(this)
    private var cheatHoursList: MutableList<CheatHourItem> = mutableListOf()

    private lateinit var selectUnblockedAppsLauncher: ActivityResultLauncher<Intent>
    private var selectedUnblockedApps: ArrayList<String>? = null

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

        val dialogAddToCheatHoursBinding = DialogAddToCheatHoursBinding.inflate(layoutInflater)

        var endTimeInMins: Int? = null
        var startTimeInMins: Int? = null

        dialogAddToCheatHoursBinding.btnSelectEndTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePickerDialog = TimePickerDialog(
                this,
                { _, selectedHour, selectedMinute ->
                    endTimeInMins = Tools.convertToMinutesFromMidnight(selectedHour, selectedMinute)
                    dialogAddToCheatHoursBinding.btnSelectEndTime.text =
                        "End Time: " + String.format("%02d:%02d", selectedHour, selectedMinute)
                },
                hour,
                minute,
                true
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
                        Tools.convertToMinutesFromMidnight(selectedHour, selectedMinute)
                    dialogAddToCheatHoursBinding.btnSelectStartTime.text =
                        "Start Time: " + String.format("%02d:%02d", selectedHour, selectedMinute)
                },
                hour,
                minute,
                true // Use 24-hour format
            )
            timePickerDialog.show()
        }
        dialogAddToCheatHoursBinding.btnSelectUnblockedApps.setOnClickListener {
            val intent = Intent(this, SelectAppsActivity::class.java)
            intent.putStringArrayListExtra(
                "PRE_SELECTED_APPS",
                arrayListOf()
            )
            selectUnblockedAppsLauncher.launch(intent)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Specify Cheat Hours")
            .setView(dialogAddToCheatHoursBinding.root)
            .setPositiveButton("Add") { dialog, _ ->
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
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }


    class CheatHourAdapter(
        private val items: List<CheatHourItem>
    ) : RecyclerView.Adapter<CheatHourAdapter.ExampleViewHolder>() {

        class ExampleViewHolder(private val binding: CheatHourItemBinding) :
            RecyclerView.ViewHolder(binding.root) {

            fun bind(item: CheatHourItem) {
                binding.cheatHourTitle.text = item.title
                val convertedStartTime = Tools.convertMinutesTo24Hour(item.startTime)
                val convertedEndTIme = Tools.convertMinutesTo24Hour(item.endTime)

                binding.cheatTimings.text =
                    "${convertedStartTime.first}:${convertedStartTime.second} to ${convertedEndTIme.first}:${convertedEndTIme.second}"
                item.packages.forEach { packageName ->
                    binding.selectedApps.text =
                        binding.selectedApps.text.toString() + " " + packageName
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExampleViewHolder {
            val binding = CheatHourItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ExampleViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ExampleViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size
    }


    data class CheatHourItem(
        val title: String,
        val startTime: Int,
        val endTime: Int,
        val packages: ArrayList<String>
    )

}