package nethical.digipaws

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import nethical.digipaws.databinding.ActivityUsageMetricsBinding
import nethical.digipaws.utils.SavedPreferencesLoader
import nethical.digipaws.utils.TimeTools


class UsageMetricsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsageMetricsBinding
    private var savedPreferencesLoader = SavedPreferencesLoader(this)


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
        totalReels = savedPreferencesLoader.getReelsScrolled()
        makeReelStatsChart()
        makeAverageAttentionSpanChart()
    }

    private fun setupChart(chart: LineChart, labels:List<String>, lineDataSet: LineDataSet){

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false) // Disable vertical grid lines
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.textColor = Color.WHITE

        val leftAxis = chart.axisLeft
        leftAxis.setDrawGridLines(false)
        leftAxis.textColor = Color.WHITE

        chart.axisRight.isEnabled = false

        chart.data = LineData(lineDataSet)

        chart.legend.isEnabled = false

        chart.invalidate()
    }

    private fun makeReelStatsChart(){
        val entries = mutableListOf<Entry>()
        val labels = mutableListOf<String>() // Store labels for X-axis

        var index = 0f // Keep track of index for the x-axis
        for ((date, value) in totalReels) {
            entries.add(Entry(index, value.toFloat()))
            labels.add(TimeTools.shortenDate(date))
            index += 1f
        }
        val lineDataSet = LineDataSet(entries, "Reel count").apply {
            color = Color.WHITE
            valueTextColor = Color.WHITE
            lineWidth = 3f
            setDrawCircles(false)
            circleRadius = 4f
            setDrawValues(false)

            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f

        }
        setupChart(binding.reelsStats,labels,lineDataSet)
    }

    private fun makeAverageAttentionSpanChart(){

        val entries = mutableListOf<Entry>()
        val labels = mutableListOf<String>() // Store labels for X-axis
        val reelsAttentionSpanData = savedPreferencesLoader.loadUsageHoursAttentionSpanData()
        var index = 0f // Keep track of index for the x-axis
        Log.d("datef", reelsAttentionSpanData.toString())
        for ((date, value) in reelsAttentionSpanData) {
            val totalElapsedTime = value.sumOf { it.elapsedTime.toDouble() }
            val average = totalElapsedTime / totalReels[date]!!
            entries.add(Entry(index, average.toFloat()))
            labels.add(TimeTools.shortenDate(date))
            Log.d("datef",date)
            index += 1f
        }

        val lineDataSet = LineDataSet(entries, "average attention Span").apply {
            color = Color.WHITE
            valueTextColor = Color.WHITE
            lineWidth = 3f
            setDrawCircles(false)
            circleRadius = 4f
            setDrawValues(false)

            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f

        }
        setupChart(binding.avgAttentionStats,labels,lineDataSet)

    }
}