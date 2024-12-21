package nethical.digipaws.views

import android.annotation.SuppressLint
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
    var showDecimal = true

    @SuppressLint("SetTextI18n")
    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        super.refreshContent(e, highlight)
        // Customize the marker content
        if (showDecimal) {
            val rounded = "%.2f".format(e?.y)
            tvContent.text = "$rounded $units"
        } else {
            tvContent.text = "${"%.0f".format(e?.y)}" // TODO: shit slow asf, needs to fix
        }
    }

    override fun getOffset(): MPPointF {
        // Adjust marker position
        return MPPointF(-(width / 2).toFloat(), -height.toFloat())
    }
}
