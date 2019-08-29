package gr.blackswamp.bmaps.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.TextView
import com.mapbox.android.search.autocomplete.AutocompleteAdapter
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import gr.blackswamp.bmaps.R

class AutoCompleteAdapter(private val context: Context) : AutocompleteAdapter(context) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = inflateView(convertView, parent)
        val feature = getItem(position)
        return updateViewData(view, feature)
    }

    private fun inflateView(convertView: View?, parent: ViewGroup?): View {
        return if (convertView == null) {
            val inflater = LayoutInflater.from(context)
            inflater.inflate(R.layout.list_item_auto_complete, parent, false)
        } else {
            convertView
        }
    }

    private fun updateViewData(view: View, feature: CarmenFeature): View {
        val text = view.findViewById<TextView>(R.id.text)
        text.text = feature.text()
        val address = view.findViewById<TextView>(R.id.address)
        if (feature.address().isNullOrBlank()) {
            address.visibility = GONE
        } else {
            address.text = feature.address()
        }
        return view
    }
}