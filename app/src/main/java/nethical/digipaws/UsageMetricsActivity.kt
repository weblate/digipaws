package nethical.digipaws

import android.graphics.Color
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import nethical.digipaws.databinding.ActivityUsageMetricsBinding
import nethical.digipaws.utils.SavedPreferencesLoader


class UsageMetricsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsageMetricsBinding
    private var savedPreferencesLoader = SavedPreferencesLoader(this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityUsageMetricsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val reelsAttentionSpanData = savedPreferencesLoader.loadUsageHoursAttentionSpanData()
        val totalReels = savedPreferencesLoader.getReelsScrolled()

        var totalTimeElapsed = 0f
        reelsAttentionSpanData.forEach { item ->
            totalTimeElapsed += item.elapsedTime
        }

        val avgAttentionSpan = (totalTimeElapsed / totalReels.size)

        binding.attentionSpan.text = "Attention Span: $avgAttentionSpan/video"


        // Convert the Map into a List of Entries
        val entries = mutableListOf<Entry>()
        val labels = mutableListOf<String>() // Store labels for X-axis

        var index = 0f // Keep track of index for the x-axis
        for ((date, value) in totalReels) {
            entries.add(Entry(index, value.toFloat()))
            labels.add(date) // Add date as a label
            index += 1f
        }

        val lineDataSet = LineDataSet(entries, "Reel count").apply {
            color = Color.WHITE
            valueTextColor = Color.WHITE
            lineWidth = 3f
            setDrawCircles(false)
            circleRadius = 4f
            setDrawValues(false)

            mode = LineDataSet.Mode.CUBIC_BEZIER // Enable smooth, curvy lines
            cubicIntensity = 0.5f // Adjust curve intensity (lower is curvier)

        }

        // Customize X-Axis
        val xAxis = binding.reelsStats.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false) // Disable vertical grid lines
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.textColor = Color.WHITE

        val leftAxis = binding.reelsStats.axisLeft
        leftAxis.setDrawGridLines(false)
        leftAxis.textColor = Color.WHITE

        // Disable right Y-axis
        binding.reelsStats.axisRight.isEnabled = false

        // Set LineData to the chart
        binding.reelsStats.data = LineData(lineDataSet)

        binding.reelsStats.legend.isEnabled = false

        // Refresh chart
        binding.reelsStats.invalidate()


    }
}