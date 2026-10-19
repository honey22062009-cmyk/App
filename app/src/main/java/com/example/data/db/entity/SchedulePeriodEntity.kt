package com.example.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.core.model.SchedulePeriod
import java.time.DayOfWeek

import androidx.room.ForeignKey

@Entity(
    tableName = "schedule_periods",
    foreignKeys = [
        ForeignKey(
            entity = ManagedAppEntity::class,
            parentColumns = ["packageName"],
            childColumns = ["appPackage"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["appPackage"]),
        Index(value = ["appPackage", "dayOfWeek"])
    ]
)
data class SchedulePeriodEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val appPackage: String,
    val dayOfWeek: Int, // 1 = Monday .. 7 = Sunday (DayOfWeek.value)
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val isEnabled: Boolean = true
) {
    fun toDomain(): SchedulePeriod {
        return SchedulePeriod(
            id = id,
            appPackage = appPackage,
            dayOfWeek = DayOfWeek.of(dayOfWeek),
            startHour = startHour,
            startMinute = startMinute,
            endHour = endHour,
            endMinute = endMinute,
            isEnabled = isEnabled
        )
    }

    companion object {
        fun fromDomain(domain: SchedulePeriod): SchedulePeriodEntity {
            return SchedulePeriodEntity(
                id = domain.id,
                appPackage = domain.appPackage,
                dayOfWeek = domain.dayOfWeek.value,
                startHour = domain.startHour,
                startMinute = domain.startMinute,
                endHour = domain.endHour,
                endMinute = domain.endMinute,
                isEnabled = domain.isEnabled
            )
        }
    }
}
