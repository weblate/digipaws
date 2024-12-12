package nethical.digipaws.views

import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import nethical.digipaws.R

class CustomMarkerView(context: Context, layoutResource: Int) :
    MarkerView(context, layoutResource) {

    private val tvContent: TextView = findViewById(R.id.tvContent)

    var units = ""
    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        // Customize the marker content

        val rounded = "%.2f".format(e?.y).toFloat()
        tvContent.text = "$rounded $units"
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        // Adjust marker position
        return MPPointF(-(width / 2).toFloat(), -height.toFloat())
    }
}
