package nethical.digipaws.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.CountDownTimer
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import nethical.digipaws.R
import nethical.digipaws.databinding.OverlayWarningScreenBinding

class WarningOverlayManager(private val context: Context) {

    private var overlayView: View? = null
    private var binding: OverlayWarningScreenBinding? = null
    private var isOverlayVisible = false
    private var windowManager: WindowManager? = null
    private lateinit var layoutParams: LayoutParams
    private var proceedTimer: CountDownTimer? = null

    @SuppressLint("InlinedApi")
    fun showTextOverlay(message: String, onClose: () -> Unit, onProceed: () -> Unit) {
        if (overlayView != null || isOverlayVisible) return

        binding = OverlayWarningScreenBinding.inflate(LayoutInflater.from(context))

        overlayView = binding?.root

        // Set up WindowManager.LayoutParams for the overlay
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT,
            LayoutParams.TYPE_APPLICATION_OVERLAY, // For Android O and above
            LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        layoutParams.gravity = Gravity.CENTER

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        binding?.overlayText?.text = message

        binding?.overlayCloseBtn?.setOnClickListener {
            onClose()
            isOverlayVisible = false
            removeOverlay()
        }
        binding?.overlayProceedBtn?.setOnClickListener {
            onProceed()
            isOverlayVisible = false
            removeOverlay()
        }
        isOverlayVisible = false

        setUpDelayToProceedOn()

        windowManager?.addView(overlayView, layoutParams)
    }

    fun removeOverlay() {
        if (overlayView != null) {
            windowManager?.removeView(overlayView)
            binding = null
            overlayView = null
        }
    }


    private fun setUpDelayToProceedOn() {
        if (binding == null) {
            return
        }
        if (!binding?.overlayProceedBtn?.isEnabled!!) {
            return
        }
        proceedTimer?.cancel()
        binding?.overlayProceedBtn?.isEnabled = false
        binding?.overlayProceedBtn?.setBackgroundColor(Color.TRANSPARENT)
        proceedTimer = object : CountDownTimer(15000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding?.overlayProceedBtn?.let { button ->
                    button.text = "Proceed in ${millisUntilFinished / 1000}"
                }
            }

            override fun onFinish() {
                binding?.overlayProceedBtn?.let { button ->
                    button.isEnabled = true
                    button.setText(R.string.proceed)
                }
            }
        }.start()
    }


}