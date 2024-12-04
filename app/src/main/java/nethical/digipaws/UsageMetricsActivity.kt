package nethical.digipaws

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.color.MaterialColors
import nethical.digipaws.databinding.ActivityUsageMetricsBinding
import nethical.digipaws.utils.CustomMarkerView
import nethical.digipaws.utils.SavedPreferencesLoader
import nethical.digipaws.utils.TimeTools
import kotlin.properties.Delegates


class UsageMetricsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsageMetricsBinding
    private var savedPreferencesLoader = SavedPreferencesLoader(this)


    var primaryColor by Delegates.notNull<Int>()

    lateinit var totalReels: Map<String,Int>
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
        primaryColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary, ContextCompat.getColor(this, R.color.text_color))

        totalReels = savedPreferencesLoader.getReelsScrolled()
        makeReelCountStatsChart()
        makeAverageReelAttentionSpanChart()
    }

    private fun setupChartUI(
        chart: LineChart,
        labels: List<String>,
        lineDataSet: LineDataSet,
        chartUnits: String = ""
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
            setScaleEnabled(false)
            setPinchZoom(false)

            data = LineData(lineDataSet)

        }
        val markerView = CustomMarkerView(this, R.layout.custom_marker_view)
        markerView.chartView = chart
        markerView.units = chartUnits
        chart.marker = markerView
        chart.invalidate()
    }

    private fun makeReelCountStatsChart(){
        val entries = mutableListOf<Entry>()
        val labels = mutableListOf<String>() // Store labels for X-axis

        var index = 0f // Keep track of index for the x-axis
        for ((date, value) in totalReels) {
            entries.add(Entry(index, value.toFloat()))
            labels.add(TimeTools.shortenDate(date))
            index += 1f
        }
        val lineDataSet = LineDataSet(entries, "Reel count")
        setupChartUI(binding.reelsStats, labels, lineDataSet, "short videos scrolled")
    }

    private fun makeAverageReelAttentionSpanChart(){

        val entries = mutableListOf<Entry>()
        val labels = mutableListOf<String>() // Store labels for X-axis
        val reelsAttentionSpanData = savedPreferencesLoader.loadUsageHoursAttentionSpanData()
        var index = 0f // Keep track of index for the x-axis
        Log.d("datef", reelsAttentionSpanData.toString())
        for ((date, value) in reelsAttentionSpanData) {
            val totalElapsedTime = value.sumOf { it.elapsedTime.toDouble() }
            val reelsCount = totalReels[date]?.toDouble() ?: 0.0
            val average = if (reelsCount > 0) {
                totalElapsedTime / reelsCount
            } else {
                0.0
            }
            entries.add(Entry(index, average.toFloat()))
            labels.add(TimeTools.shortenDate(date))
            Log.d("datef",date)
            index += 1f
        }

        val lineDataSet = LineDataSet(entries, "average attention Span")
        setupChartUI(binding.avgAttentionStats, labels, lineDataSet, "seconds/video")
    }

}