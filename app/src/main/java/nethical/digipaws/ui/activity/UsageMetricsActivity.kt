package nethical.digipaws.ui.activity

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nethical.digipaws.R
import nethical.digipaws.databinding.ActivityUsageMetricsBinding
import nethical.digipaws.services.UsageTrackingService
import nethical.digipaws.utils.SavedPreferencesLoader
import nethical.digipaws.utils.TimeTools
import nethical.digipaws.views.CustomMarkerView
import kotlin.properties.Delegates


class UsageMetricsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsageMetricsBinding
    private var savedPreferencesLoader = SavedPreferencesLoader(this)


    var primaryColor by Delegates.notNull<Int>()

    lateinit var totalReels: Map<String,Int>
    lateinit var reelsAttentionSpanData: MutableMap<String, MutableList<UsageTrackingService.AttentionSpanVideoItem>>

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
        primaryColor = MaterialColors.getColor(
            this, com.google.android.material.R.attr.colorPrimary, ContextCompat.getColor(
                this,
                R.color.text_color
            )
        )

        totalReels = savedPreferencesLoader.getReelsScrolled()
        reelsAttentionSpanData = savedPreferencesLoader.loadUsageHoursAttentionSpanData()


        lifecycleScope.launch {
            // Get the current date and update UI elements

            makeReelCountStatsChart()
            makeAverageReelAttentionSpanChart()

            val date = TimeTools.getCurrentDate()
            binding.statsTodayReels.text = getString(R.string.you_scrolled_reels, totalReels[date])

            val average = withContext(Dispatchers.Default) {
                reelsAttentionSpanData[date]?.let { calculateAverageAttentionSpan(it, date) }
            }

            binding.statsAttentionSpanToday.text =
                getString(R.string.you_had_an_attention_span_of_seconds_video, average)
        }
        binding.btnDigiWelbeing.setOnClickListener {
            val packageName = "com.google.android.apps.wellbeing"
            val intent = packageManager.getLaunchIntentForPackage(packageName)

            if (intent != null) {
                startActivity(intent)
            } else {
                Snackbar.make(
                    binding.root,
                    getString(R.string.failed_to_launch_system_digital_wellbeing),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }


    private fun setupChartUI(
        chart: LineChart,
        labels: List<String>,
        lineDataSet: LineDataSet,
        chartUnits: String = "",
        showDecimal: Boolean = true
    ) {

        lineDataSet.apply {
            color = primaryColor
            valueTextColor = primaryColor
            lineWidth = 3f
            setDrawCircles(true)
            setDrawCircleHole(true)
            circleHoleRadius = 4f
            circleRadius = 8f
            setCircleColor(primaryColor)

            setDrawValues(false)

            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f
        }

        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            labelCount = 5
            setDrawGridLines(false) // Disable vertical grid lines
            valueFormatter = IndexAxisValueFormatter(labels)
            textColor = primaryColor
        }

        chart.axisLeft.apply {
            setDrawGridLines(false)
            textColor = primaryColor
        }

        chart.apply {
            axisRight.isEnabled = false
            legend.isEnabled = false
            description.isEnabled = false
            animateY(800, Easing.EaseInCubic)


            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)

            setPinchZoom(false)

            data = LineData(lineDataSet)

        }
        val markerView = CustomMarkerView(this, R.layout.custom_marker_view)
        markerView.chartView = chart
        markerView.units = chartUnits
        markerView.showDecimal = showDecimal
        chart.marker = markerView
        chart.invalidate()
    }


    private suspend fun makeReelCountStatsChart() {
        withContext(Dispatchers.Default) {
            val entries = mutableListOf<Entry>()
            val labels = mutableListOf<String>() // Store labels for X-axis

            var index = 0f // Keep track of index for the x-axis
            for ((date, value) in totalReels) {
                entries.add(Entry(index, value.toFloat()))
                labels.add(TimeTools.shortenDate(date))
                index += 1f
            }

            val lineDataSet = LineDataSet(entries, getString(R.string.reel_count))
            // Switch back to the main thread to update the UI
            withContext(Dispatchers.Main) {
                setupChartUI(
                    binding.reelsStats, labels, lineDataSet,
                    getString(R.string.short_videos_scrolled), false
                )
            }
        }
    }

    private suspend fun makeAverageReelAttentionSpanChart() {
        withContext(Dispatchers.Default) {
            val entries = mutableListOf<Entry>()
            val labels = mutableListOf<String>() // Store labels for X-axis
            var index = 0f // Keep track of index for the x-axis

            Log.d("datef", reelsAttentionSpanData.toString())
            for ((date, value) in reelsAttentionSpanData) {
                val average = calculateAverageAttentionSpan(value, date)
                entries.add(Entry(index, average.toFloat()))
                labels.add(TimeTools.shortenDate(date))
                Log.d("datef", date)
                index += 1f
            }

            val lineDataSet = LineDataSet(entries, getString(R.string.average_attention_span))
            // Switch back to the main thread to update the UI
            withContext(Dispatchers.Main) {
                setupChartUI(
                    binding.avgAttentionStats, labels, lineDataSet,
                    getString(R.string.seconds_video)
                )
            }
        }
    }

    private fun calculateAverageAttentionSpan(
        value: MutableList<UsageTrackingService.AttentionSpanVideoItem>,
        date: String
    ): Double {
        val totalElapsedTime = value.sumOf { it.elapsedTime.toDouble() }
        val reelsCount = totalReels[date]?.toDouble() ?: 0.0
        return if (reelsCount > 0) {
            totalElapsedTime / reelsCount
        } else {
            0.0
        }
    }

}