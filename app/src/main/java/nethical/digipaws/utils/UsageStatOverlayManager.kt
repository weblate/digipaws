package nethical.digipaws.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.CountDownTimer
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import nethical.digipaws.databinding.OverlayUsageStatBinding

class UsageStatOverlayManager(private val context: Context) {

    private var overlayView: View? = null
    private var binding: OverlayUsageStatBinding? = null
    private var isOverlayVisible = false
    private var windowManager: WindowManager? = null
    private lateinit var layoutParams: LayoutParams
    private var proceedTimer: CountDownTimer? = null

    private var reelsScrolledThisSession = 0

    @SuppressLint("InlinedApi")
    fun startDisplaying() {
        if (overlayView != null || isOverlayVisible) return

        binding = OverlayUsageStatBinding.inflate(LayoutInflater.from(context))

        overlayView = binding?.root

        // Set up WindowManager.LayoutParams for the overlay
        layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            LayoutParams.TYPE_APPLICATION_OVERLAY, // For Android O and above
            LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        layoutParams.gravity = Gravity.CENTER

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        windowManager?.addView(overlayView, layoutParams)
    }

    fun removeOverlay() {
        if (overlayView != null) {
            windowManager?.removeView(overlayView)
            binding = null
            overlayView = null
        }
    }

    @SuppressLint("SetTextI18n")
    fun incrementReelScrollCount() {
        reelsScrolledThisSession++
        binding?.reelCounter?.text = reelsScrolledThisSession.toString()
    }


}