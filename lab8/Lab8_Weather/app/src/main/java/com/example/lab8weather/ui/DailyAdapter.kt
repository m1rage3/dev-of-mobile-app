package com.example.lab8weather.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lab8weather.R
import com.example.lab8weather.mapping.DailyUiModel
import kotlin.math.roundToInt

class DailyAdapter : RecyclerView.Adapter<DailyAdapter.DayViewHolder>() {

    private var days: List<DailyUiModel> = emptyList()

    fun submit(newDays: List<DailyUiModel>) {
        days = newDays
        notifyDataSetChanged()
    }

    class DayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDayDate)
        val tvEmoji: TextView = view.findViewById(R.id.tvDayEmoji)
        val tvTemp: TextView = view.findViewById(R.id.tvDayTemp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val day = days[position]
        holder.tvDate.text = day.date
        holder.tvEmoji.text = day.emoji
        holder.tvEmoji.contentDescription = day.condition
        holder.tvTemp.text = "${day.tempMin.roundToInt()}° … ${day.tempMax.roundToInt()}°"
    }

    override fun getItemCount() = days.size
}
