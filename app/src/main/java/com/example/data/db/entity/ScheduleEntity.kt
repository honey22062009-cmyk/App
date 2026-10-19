package com.example.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.core.model.SchedulePeriod
import java.time.DayOfWeek

/**
 * Entity representing an allowed time window schedule for a specific application.
 *
 * Enforces data integrity using a [ForeignKey] relationship to [AppEntity] referencing
 * the app's [packageName], with cascading delete to prevent orphaned schedules.
 */
@Entity(
    tableName = "schedules",
    foreignKeys = [
        ForeignKey(
            entity = AppEntity::class,
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
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "appPackage")
    val appPackage: String,
    val dayOfWeek: Int = 1, // 1 = Monday .. 7 = Sunday (DayOfWeek.value)
    val startHour: Int = 0,
    val startMinute: Int = 0,
    val endHour: Int = 0,
    val endMinute: Int = 0,
    val isEnabled: Boolean = true
) {
    @Ignore
    val packageName: String = appPackage

    fun toDomain(): SchedulePeriod {
        return SchedulePeriod(
            id = id,
            appPackage = appPackage,
            dayOfWeek = DayOfWeek.of(dayOfWeek.coerceIn(1, 7)),
            startHour = startHour,
            startMinute = startMinute,
            endHour = endHour,
            endMinute = endMinute,
            isEnabled = isEnabled
        )
    }

    companion object {
        fun fromDomain(domain: SchedulePeriod): ScheduleEntity {
            return ScheduleEntity(
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
