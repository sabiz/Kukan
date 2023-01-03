package jp.sabiz.kukan.common

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import androidx.core.os.postDelayed

class LongTouchEventDetector(private val longTouchTimeMillis:Long, private val listener: OnLongTouchEventListener): OnTouchListener {

    private val handler = Handler(Looper.getMainLooper())
    private var isLongTapStarted = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        event?:return false
        when(event.action.and(MotionEvent.ACTION_MASK)){
            MotionEvent.ACTION_DOWN -> {
                handler.removeCallbacksAndMessages(null)
                if (isLongTapStarted) {
                    handler.post {
                        listener.onLongTouchCanceled()
                    }
                }
                handler.post {
                    listener.onLongTouchDown()
                }
                isLongTapStarted = true
                handler.postDelayed(longTouchTimeMillis) {
                    isLongTapStarted = false
                    listener.onLongTouchUp()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacksAndMessages(null)
                if (isLongTapStarted) {
                    handler.post {
                        isLongTapStarted = false
                        listener.onLongTouchCanceled()
                    }
                }
                return true
            }
        }
        return false
    }

    interface OnLongTouchEventListener {
        fun onLongTouchDown()
        fun onLongTouchCanceled()
        fun onLongTouchUp()
    }
}