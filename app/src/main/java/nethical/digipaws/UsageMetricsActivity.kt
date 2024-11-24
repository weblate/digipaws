package nethical.digipaws

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
        val data = savedPreferencesLoader.loadUsageHoursAttentionSpanData()
        val totalReels = savedPreferencesLoader.getReelsScrolled()

        var totalTimeElapsed = 0f
        data.forEach { item ->
            totalTimeElapsed += item.elapsedTime
        }

        val avgAttentionSpan = totalTimeElapsed / totalReels

        binding.attentionSpan.text =
            "Your average attention span is $avgAttentionSpan seconds per video."
    }
}