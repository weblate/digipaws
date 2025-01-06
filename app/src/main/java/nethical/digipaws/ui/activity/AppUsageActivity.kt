package nethical.digipaws.ui.activity

import androidx.activity.enableEdgeToEdge
import nethical.digipaws.R
import nethical.digipaws.databinding.ActivityAppUsageBinding

import android.annotation.SuppressLint
import android.app.usage.UsageStatsManager
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.Process
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.renderer.PieChartRenderer
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nethical.digipaws.databinding.AppUsageItemBinding
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

class AppUsageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAppUsageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        binding = ActivityAppUsageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val appUsageStats = getUsageStats(getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager)

        val launcherApps = getSystemService(LAUNCHER_APPS_SERVICE) as LauncherApps

        val filteredAppUsageStats =
            appUsageStats.asSequence()
                .takeWhile { it.totalTimeInForeground > 5 * 1000 }.map { stats ->
                    Stats(
                        launcherApps.getApplicationInfo(
                            stats.packageName, 0, Process.myUserHandle()
                        ), stats
                    )
                }.toList()

        val adapter = AppUsageAdapter(filteredAppUsageStats)
        binding.appUsageRecyclerView.apply {
            addItemDecoration(
                DividerItemDecoration(
                    context, OrientationHelper.VERTICAL
                )
            )
            this.adapter = adapter
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()

        lifecycleScope.launch(Dispatchers.IO) {
            val appUsageStats =
                getUsageStats(getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager)

            val launcherApps = getSystemService(LAUNCHER_APPS_SERVICE) as LauncherApps

            val filteredAppUsageStats =
                appUsageStats.asSequence()
                    .takeWhile { it.totalTimeInForeground > 5 * 1000 }.map { stats ->
                        val applicationInfo = launcherApps.getApplicationInfo(
                            stats.packageName, 0, Process.myUserHandle()
                        )
                        Stats(applicationInfo, stats)
                    }.toList()

            launch(Dispatchers.Main) {
                val adapter = binding.appUsageRecyclerView.adapter as AppUsageAdapter
                adapter.updateData(filteredAppUsageStats)
            }
            launch(Dispatchers.Main) {
                updatePieChart(filteredAppUsageStats)
            }
        }

    }
    private fun updatePieChart(statsList: List<Stats>) {
        val sortedStats = statsList.sortedByDescending { it.usageStats.totalTimeInForeground }
        val topApps = sortedStats.take(4)

        val othersTime = sortedStats.drop(4)
            .sumOf { it.usageStats.totalTimeInForeground }

        val entries = mutableListOf<PieEntry>()
        topApps.forEach { stats ->
            val appName = stats.applicationInfo.loadLabel(packageManager).toString()
            val usageTime = stats.usageStats.totalTimeInForeground
            entries.add(PieEntry(usageTime.toFloat(), appName))
        }

        if (othersTime > 0) {
            entries.add(PieEntry(othersTime.toFloat(), "Others"))
        }

        val pieDataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                MaterialColors.getColor(this@AppUsageActivity, com.google.android.material.R.attr.colorPrimary, Color.BLUE),
                MaterialColors.getColor(this@AppUsageActivity, com.google.android.material.R.attr.colorSecondary, Color.WHITE),
                MaterialColors.getColor(this@AppUsageActivity, com.google.android.material.R.attr.colorTertiary, Color.WHITE),
                MaterialColors.getColor(this@AppUsageActivity, com.google.android.material.R.attr.colorPrimaryVariant, Color.CYAN),
                MaterialColors.getColor(this@AppUsageActivity, com.google.android.material.R.attr.colorSurfaceVariant, Color.GRAY)
            )

            // Add spacing between slices
            sliceSpace = 3f

            setDrawValues(false)

            // Increase selection shift
            selectionShift = 10f

            setGradientColor(MaterialColors.getColor(this@AppUsageActivity, com.google.android.material.R.attr.colorPrimaryContainer, Color.LTGRAY),
                MaterialColors.getColor(this@AppUsageActivity, com.google.android.material.R.attr.colorSecondaryContainer, Color.DKGRAY))
        }

        val pieData = PieData(pieDataSet)

        binding.pieChart.apply {
            data = pieData
            description.isEnabled = false
            isRotationEnabled = true

            // Center hole styling
            isDrawHoleEnabled = true
            holeRadius = 65f
            transparentCircleRadius = 0f  // Remove transparent circle
            setHoleColor(MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurface, Color.WHITE))

            legend.isEnabled = false

            // External labels styling
            setDrawEntryLabels(false)  // Disable internal labels


            animateY(1200, Easing.EaseInOutQuart)

            setExtraOffsets(50f, 50f, 50f, 50f)

            //Todo: Add external labels
            invalidate()
        }
    }

    fun getUsageStats(usageStatsManager: UsageStatsManager): List<AppUsageStats> {
        val endTime = System.currentTimeMillis()
        val startTime = ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())
            .toLocalDate()
            .atStartOfDay(ZoneOffset.systemDefault())
            .toInstant()
            .toEpochMilli()

        return usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ).map { AppUsageStats(it.packageName, it.totalTimeInForeground) }
            .groupBy { it.packageName }
            .map { (_, statsList) ->
                statsList.reduce { acc, stats ->
                    acc.apply { totalTimeInForeground += stats.totalTimeInForeground }
                }
            }.sortedByDescending { it.totalTimeInForeground }
    }

    data class AppUsageStats(val packageName: String, var totalTimeInForeground: Long)

    inner class AppUsageViewHolder(private val binding: AppUsageItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(stats: Stats, packageManager: PackageManager) {
            binding.appIcon.setImageDrawable(stats.applicationInfo.loadIcon(packageManager))
            binding.appName.text = stats.applicationInfo.loadLabel(packageManager)
            binding.appUsage.text = formatTime(stats.usageStats.totalTimeInForeground)

            binding.root.setOnClickListener {
                if (stats.applicationInfo.packageName == packageName) {
                    Snackbar.make(
                        binding.root, "Cannot set limit for Reef", Snackbar.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
//                val intent = Intent(
//                    this@AppUsageActivity, ApplicationDailyLimitActivity::class.java
//                ).apply {
//                    putExtra("package_name", stats.applicationInfo.packageName)
//                }
//                startActivity(intent)
            }
        }
    }

    private fun formatTime(timeInMillis: Long): String {
        val hours = timeInMillis / (1000 * 60 * 60)
        val minutes = (timeInMillis % (1000 * 60 * 60)) / (1000 * 60)
        val seconds = (timeInMillis % (1000 * 60)) / 1000

        return when {
            hours > 0 -> "$hours hr" + (if (minutes > 0) " $minutes mins" else "") + (if (seconds > 0) " $seconds secs" else "")
            minutes > 0 -> "$minutes mins" + (if (seconds > 0) " $seconds secs" else "")
            else -> "$seconds secs"
        }
    }

    data class Stats(val applicationInfo: ApplicationInfo, val usageStats: AppUsageStats)

    inner class AppUsageAdapter(private var appUsageStats: List<Stats>) :
        RecyclerView.Adapter<AppUsageViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppUsageViewHolder {
            val binding = AppUsageItemBinding.inflate(layoutInflater, parent, false)
            return AppUsageViewHolder(binding)
        }

        override fun onBindViewHolder(holder: AppUsageViewHolder, position: Int) {
            holder.bind(appUsageStats[position], packageManager)
        }

        @SuppressLint("NotifyDataSetChanged")
        fun updateData(newAppUsageStats: List<Stats>) {
            appUsageStats = newAppUsageStats
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int = appUsageStats.size
    }
}