package nethical.digipaws.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import nethical.digipaws.databinding.OverlayWarningScreenBinding

class OverlayManager(private val context: Context) {

    private var overlayView: View? = null
    private var windowManager: WindowManager? = null
    private var binding: OverlayWarningScreenBinding? = null
    private var isOverlayVisible = false

    @SuppressLint("InlinedApi")
    fun showOverlay(message: String,onClose:()->Unit, onProceed:()->Unit) {
        if (overlayView != null || isOverlayVisible) return

        binding = OverlayWarningScreenBinding.inflate(LayoutInflater.from(context))
        overlayView = binding?.root

        // Set up WindowManager.LayoutParams for the overlay
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, // For Android O and above
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        layoutParams.gravity = Gravity.CENTER

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager?.addView(overlayView, layoutParams)

        binding?.overlayText?.text = message

        binding?.overlayCloseBtn?.setOnClickListener {
            onClose()
            isOverlayVisible = false
            removeOverlay()
        }
        binding?.overlayProceedBtn?.setOnClickListener{
            onProceed()
            isOverlayVisible = false
            removeOverlay()
        }
        isOverlayVisible = false
    }

    fun removeOverlay() {
        if (overlayView != null) {
            windowManager?.removeView(overlayView)
            overlayView = null
            binding = null // Clean up binding
        }
    }

}
