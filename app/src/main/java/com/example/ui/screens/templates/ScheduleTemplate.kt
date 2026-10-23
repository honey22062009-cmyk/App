package com.example.ui.screens.templates
import com.example.core.model.SchedulePeriod
import java.time.DayOfWeek
data class ScheduleTemplate(val id:String,val title:String,val description:String,val days:Set<DayOfWeek>,val startHour:Int,val startMinute:Int,val endHour:Int,val endMinute:Int) {
 fun createPeriods(packageName:String):List<SchedulePeriod> = days.sortedBy { it.value }.map { day -> SchedulePeriod(appPackage=packageName,dayOfWeek=day,startHour=startHour,startMinute=startMinute,endHour=endHour,endMinute=endMinute,isEnabled=true) }
 val timeLabel:String get()="%02d:%02d–%02d:%02d".format(startHour,startMinute,endHour,endMinute)
}
object ScheduleTemplates {
 private val weekdays=setOf(DayOfWeek.MONDAY,DayOfWeek.TUESDAY,DayOfWeek.WEDNESDAY,DayOfWeek.THURSDAY,DayOfWeek.FRIDAY)
 private val everyDay=DayOfWeek.values().toSet()
 val all=listOf(
  ScheduleTemplate("study","Study focus","Allow access for one evening hour on weekdays; block outside it.",weekdays,17,0,18,0),
  ScheduleTemplate("workday","Workday","Allow access from 9 AM to 5 PM, Monday to Friday.",weekdays,9,0,17,0),
  ScheduleTemplate("sleep","Sleep routine","Allow access from 7 AM to 10 PM every day; nights remain blocked.",everyDay,7,0,22,0),
  ScheduleTemplate("weekend","Weekend only","Allow access from 10 AM to 9 PM on Saturday and Sunday only.",setOf(DayOfWeek.SATURDAY,DayOfWeek.SUNDAY),10,0,21,0)
 )
}
