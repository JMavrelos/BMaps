package gr.blackswamp.bmaps.ui.callbacks

import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior

class AutoCompleteBottomSheetCallback(private val listener:(Int)->Unit): BottomSheetBehavior.BottomSheetCallback() {
    override fun onSlide(bottomSheet: View, slideOffset: Float) {
        // No impl
    }

    override fun onStateChanged(bottomSheet: View, newState: Int) {
        listener.invoke(newState)
    }
}