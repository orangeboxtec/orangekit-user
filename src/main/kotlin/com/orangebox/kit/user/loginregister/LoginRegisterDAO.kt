package com.orangebox.kit.user.loginregister

import com.mongodb.BasicDBObject
import com.orangebox.kit.core.dao.AbstractDAO
import jakarta.enterprise.context.ApplicationScoped
import org.bson.Document
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

@ApplicationScoped
class LoginRegisterDAO: AbstractDAO<LoginRegister>(LoginRegister::class.java) {

    private val dayOfWeek = listOf("", "Domingo", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado")

    private val monthStringShort = listOf("", "Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago","Set", "Out", "Nov", "Dez")

    private val sdf = SimpleDateFormat("dd/MM/yyyy")

    override fun getId(bean: LoginRegister): Any? {
        return bean.id
    }

    fun loginRegisterDaily(userId: String, page: Int): Map<String, *>{
        val list = ArrayList<Document>()

        val calendarInitial = Calendar.getInstance()
        calendarInitial.set(Calendar.HOUR_OF_DAY, 0)
        calendarInitial.set(Calendar.MINUTE, 0)
        calendarInitial.set(Calendar.SECOND, 0)
        calendarInitial.set(Calendar.MILLISECOND, 0)

        val calendarFinal = Calendar.getInstance()
        calendarFinal.set(Calendar.HOUR_OF_DAY, 23)
        calendarFinal.set(Calendar.MINUTE, 59)
        calendarFinal.set(Calendar.SECOND, 59)
        calendarFinal.set(Calendar.MILLISECOND, 999)

        if(page > 0) {
            calendarInitial.add(Calendar.DAY_OF_WEEK, page - 1)
            calendarFinal.add(Calendar.DAY_OF_WEEK, page - 1)
        } else if (page < 0) {
            calendarInitial.add(Calendar.DAY_OF_WEEK, page)
            calendarFinal.add(Calendar.DAY_OF_WEEK, page)
        }

        val myCollection = getDb().getCollection("loginRegister")
        val matchDate = BasicDBObject("\$match",
            BasicDBObject("userId", userId)
                .append("loginDate", BasicDBObject("\$gte", calendarInitial.time).append("\$lte", calendarFinal.time))
        )

        val project = BasicDBObject("\$project",
            BasicDBObject("hour", BasicDBObject("\$dateToString", BasicDBObject("format", "%H").append("date", "\$loginDate")))
                .append("count", BasicDBObject("\$toInt", "1"))
                .append("userId", "\$userId")
                .append("loginDate", "\$loginDate")
                .append("weekDay", BasicDBObject("\$dayOfWeek", "\$loginDate"))
                .append("dateF", BasicDBObject("\$dateToString", BasicDBObject("format", "%d/%m/%Y").append("date", "\$loginDate")))

        )

        val group = BasicDBObject("\$group",
            BasicDBObject("_id", "\$hour")
            .append("total", BasicDBObject("\$sum", "\$count"))
            .append("weekDay", BasicDBObject("\$first", "\$weekDay"))
            .append("date", BasicDBObject("\$first", "\$dateF"))
        )

        val sort = BasicDBObject("\$sort", BasicDBObject("_id", 1))

        myCollection.aggregate(
            listOf(
                matchDate,
                project,
                group,
                sort
            )).into(list)

        val map = HashMap<String, Any>()
        map["labels"] = list.map { p -> p["_id"].toString().toInt() - 3 }
        map["series"] = list.map { p -> p["total"] }
        map["weekDay"] = dayOfWeek[calendarInitial.get(Calendar.DAY_OF_WEEK)]
        map["date"] = sdf.format(calendarInitial.time)
        return map
    }

    fun loginRegisterWeekly(userId: String, page: Int): Map<String, *>{
        val list = ArrayList<Document>()

        val calendarInitial = Calendar.getInstance()
        calendarInitial.set(Calendar.HOUR_OF_DAY, 0)
        calendarInitial.set(Calendar.MINUTE, 0)
        calendarInitial.set(Calendar.SECOND, 0)
        calendarInitial.set(Calendar.MILLISECOND, 0)

        val calendarFinal = Calendar.getInstance()
        calendarFinal.set(Calendar.HOUR_OF_DAY, 23)
        calendarFinal.set(Calendar.MINUTE, 59)
        calendarFinal.set(Calendar.SECOND, 59)
        calendarFinal.set(Calendar.MILLISECOND, 999)

        if(page == 0 || page == 1) {
            calendarFinal.add(Calendar.DAY_OF_WEEK, 6)
        } else if (page > 1) {
            calendarInitial.add(Calendar.DAY_OF_WEEK, (page - 1) * 7 )
            calendarFinal.add(Calendar.DAY_OF_WEEK, ((page -1 ) * 7) + 6)
        } else if(page == -1){
            calendarInitial.add(Calendar.DAY_OF_WEEK, -7)
            calendarFinal.add(Calendar.DAY_OF_WEEK, -1)
        } else {
            calendarInitial.add(Calendar.DAY_OF_WEEK, (page + 1) * 7 - 7 )
            calendarFinal.add(Calendar.DAY_OF_WEEK, (page + 1 ) * 7 - 1)
        }

        val myCollection = getDb().getCollection("loginRegister")
        val matchDate = BasicDBObject("\$match", BasicDBObject("userId", userId)
            .append("loginDate", BasicDBObject("\$gte", calendarInitial.time).append("\$lte", calendarFinal.time))
        )

        val project = BasicDBObject("\$project", BasicDBObject("day", BasicDBObject("\$dateToString", BasicDBObject("format", "%d").append("date", "\$loginDate")))
            .append("count", BasicDBObject("\$toInt", "1"))
            .append("userId", "\$userId")
            .append("loginDate", "\$loginDate")
            .append("dayString", BasicDBObject("\$dateToString", BasicDBObject("format", "%d").append("date", "\$loginDate")))
            .append("dateToSort", BasicDBObject("\$dateToString", BasicDBObject("format", "%m%d").append("date", "\$loginDate")))
        )

        val group = BasicDBObject("\$group", BasicDBObject("_id", "\$day")
            .append("total", BasicDBObject("\$sum", "\$count"))
            .append("dayString", BasicDBObject("\$first", "\$dayString"))
            .append("dateToSort", BasicDBObject("\$first", "\$dateToSort"))
        )

        val sort = BasicDBObject("\$sort", BasicDBObject("_id", 1))

        myCollection.aggregate(
            listOf(
                matchDate,
                project,
                group,
                sort
            )).into(list)

        var verification = 0
        while (verification < 7){
            val day = calendarInitial.get(Calendar.DAY_OF_MONTH).toString()

            val find = list.find { a -> a["_id"] == day }
            if(find == null){
                val monthString = if((calendarInitial.get(Calendar.MONTH) + 1) < 10){
                    "0".plus((calendarInitial.get(Calendar.MONTH) + 1).toString())
                } else {
                    "".plus((calendarInitial.get(Calendar.MONTH) + 1).toString())
                }

                val dayString = if(calendarInitial.get(Calendar.DAY_OF_MONTH) < 10){
                    "0".plus(calendarInitial.get(Calendar.DAY_OF_MONTH).toString())
                } else {
                    "".plus(calendarInitial.get(Calendar.DAY_OF_MONTH).toString())
                }

                val document = Document()
                document["_id"] = day
                document["total"] = 0
                document["dateToSort"] = monthString + dayString

                list.add(document)
            }

            calendarInitial.add(Calendar.DAY_OF_MONTH, 1)
            verification ++
        }

        calendarInitial.add(Calendar.DAY_OF_MONTH, -7)


        list.sortBy { it["dateToSort"].toString() }


        val map = HashMap<String, Any>()
        map["labels"] = list.map { p-> p["_id"] }
        map["series"] = list.map { p-> p["total"] }
        map["initialDate"] = sdf.format(calendarInitial.time)
        map["finaleDate"] = sdf.format(calendarFinal.time)

        return map
    }

    fun loginRegisterMonthly(userId: String, page: Int): Map<String, *>{
        val list = ArrayList<Document>()

        val calendarInitial = Calendar.getInstance()
        calendarInitial.set(Calendar.HOUR_OF_DAY, 0)
        calendarInitial.set(Calendar.MINUTE, 0)
        calendarInitial.set(Calendar.SECOND, 0)
        calendarInitial.set(Calendar.MILLISECOND, 0)
        calendarInitial.set(Calendar.DAY_OF_MONTH, 1)
        calendarInitial.set(Calendar.MONTH, 0)

        val calendarFinal = Calendar.getInstance()
        calendarFinal.set(Calendar.HOUR_OF_DAY, 23)
        calendarFinal.set(Calendar.MINUTE, 59)
        calendarFinal.set(Calendar.SECOND, 59)
        calendarFinal.set(Calendar.MILLISECOND, 999)
        calendarFinal.set(Calendar.DAY_OF_MONTH, 31)
        calendarFinal.set(Calendar.MONTH, 11)

        if(page > 1) {
            calendarInitial.add(Calendar.YEAR, page - 1)
            calendarFinal.add(Calendar.YEAR, page - 1)
        } else if( page < 0){
            calendarInitial.add(Calendar.YEAR, page)
            calendarFinal.add(Calendar.YEAR, page)
        }

        val myCollection = getDb().getCollection("loginRegister")
        val matchDate = BasicDBObject("\$match", BasicDBObject("userId", userId)
            .append("loginDate", BasicDBObject("\$gte", calendarInitial.time).append("\$lte", calendarFinal.time))
        )

        val project = BasicDBObject("\$project",
            BasicDBObject("month", BasicDBObject("\$dateToString", BasicDBObject("format", "%m").append("date", "\$loginDate")))
            .append("count", BasicDBObject("\$toInt", "1"))
            .append("userId", "\$userId")
            .append("loginDate", "\$loginDate")
        )

        val group = BasicDBObject("\$group", BasicDBObject("_id", "\$month")
            .append("total", BasicDBObject("\$sum", "\$count"))
        )

        val sort = BasicDBObject("\$sort", BasicDBObject("_id", 1))

        myCollection.aggregate(
            listOf(
                matchDate,
                project,
                group,
                sort
            )).into(list)

        var verification = 1

        while (verification <= 12){
            val find = list.find { a -> a["_id"].toString().toInt() == verification }
            if(find == null){
                val document = Document()
                document["_id"] = if(verification < 10){
                    "0".plus(verification)
                } else {
                    verification.toString()
                }
                document["total"] = 0

                list.add(document)
            }

            verification ++
        }

        list.sortBy { it["_id"].toString().toInt() }

        list.forEach {
            it["_id"] =  monthStringShort[it["_id"].toString().toInt()]
        }

        val map = HashMap<String, Any>()
        map["labels"] = list.map { p-> p["_id"] }
        map["series"] = list.map { p-> p["total"] }
        map["year"] = "${calendarInitial.get(Calendar.YEAR)}"

        return map
    }

    fun loginsRegisterDaily(usersId: List<String>?, page: Int?): Map<String, Any> {
        val (calendarInitial, calendarFinal) = getDailyPeriod(page)
        val docs = aggregateLogins(
            matchObj = buildMatchObj(calendarInitial.time, calendarFinal.time, usersId),
            projectFields = dailyProjectFields(),
            groupFields = dailyGroupFields(),
            sortFields = BasicDBObject("_id", 1)
        )
        return mapOf(
            "labels" to docs.map { (it["_id"].toString().toInt() - 3) },
            "series" to docs.map { it["total"] },
            "weekDay" to dayOfWeek[calendarInitial.get(Calendar.DAY_OF_WEEK)],
            "date" to sdf.format(calendarInitial.time)
        )
    }

    fun loginsRegisterWeekly(usersId: List<String>?, page: Int?): Map<String, Any> {
        val (calendarInitial, calendarFinal) = getWeeklyPeriod(page)
        val docs = aggregateLogins(
            matchObj = buildMatchObj(calendarInitial.time, calendarFinal.time, usersId),
            projectFields = weeklyProjectFields(),
            groupFields = weeklyGroupFields(),
            sortFields = BasicDBObject("_id", 1)
        )
        fillMissingDays(calendarInitial, docs)
        docs.sortBy { it["dateToSort"].toString() }
        return mapOf(
            "labels" to docs.map { it["_id"] },
            "series" to docs.map { it["total"] },
            "initialDate" to sdf.format(calendarInitial.time),
            "finaleDate" to sdf.format(calendarFinal.time)
        )
    }

    fun loginsRegisterMonthly(usersId: List<String>?, page: Int?): Map<String, Any> {
        val (calendarInitial, calendarFinal) = getMonthlyPeriod(page)
        val docs = aggregateLogins(
            matchObj = buildMatchObj(calendarInitial.time, calendarFinal.time, usersId),
            projectFields = monthlyProjectFields(),
            groupFields = monthlyGroupFields(),
            sortFields = BasicDBObject("_id", 1)
        )
        fillMissingMonths(docs)
        docs.sortBy { it["_id"].toString().toInt() }
        docs.forEach { it["_id"] = monthStringShort[it["_id"].toString().toInt()] }
        return mapOf(
            "labels" to docs.map { it["_id"] },
            "series" to docs.map { it["total"] },
            "year" to "${calendarInitial.get(Calendar.YEAR)}"
        )
    }


    private fun getDailyPeriod(page: Int?): Pair<Calendar, Calendar> {
        val initial = Calendar.getInstance().apply { setToStartOfDay() }
        val final = Calendar.getInstance().apply { setToEndOfDay() }
        page?.let {
            initial.add(Calendar.DAY_OF_WEEK, it - if (it > 0) 1 else 0)
            final.add(Calendar.DAY_OF_WEEK, it - if (it > 0) 1 else 0)
        }
        return initial to final
    }

    private fun getWeeklyPeriod(page: Int?): Pair<Calendar, Calendar> {
        val initial = Calendar.getInstance().apply { setToStartOfDay() }
        val final = Calendar.getInstance().apply { setToEndOfDay() }
        when {
            page == null || page == 0 || page == 1 -> final.add(Calendar.DAY_OF_WEEK, 6)
            page > 1 -> {
                initial.add(Calendar.DAY_OF_WEEK, (page - 1) * 7)
                final.add(Calendar.DAY_OF_WEEK, ((page - 1) * 7) + 6)
            }
            page == -1 -> {
                initial.add(Calendar.DAY_OF_WEEK, -7)
                final.add(Calendar.DAY_OF_WEEK, -1)
            }
            else -> {
                initial.add(Calendar.DAY_OF_WEEK, (page + 1) * 7 - 7)
                final.add(Calendar.DAY_OF_WEEK, (page + 1) * 7 - 1)
            }
        }
        return initial to final
    }

    private fun getMonthlyPeriod(page: Int?): Pair<Calendar, Calendar> {
        val initial = Calendar.getInstance().apply {
            setToStartOfDay()
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.MONTH, 0)
        }
        val final = Calendar.getInstance().apply {
            setToEndOfDay()
            set(Calendar.DAY_OF_MONTH, 31)
            set(Calendar.MONTH, 11)
        }
        page?.let {
            initial.add(Calendar.YEAR, it - if (it > 1) 1 else 0)
            final.add(Calendar.YEAR, it - if (it > 1) 1 else 0)
        }
        return initial to final
    }

    private fun buildMatchObj(start: Date, end: Date, usersId: List<String>?): BasicDBObject {
        val obj = BasicDBObject("loginDate", BasicDBObject("\$gte", start).append("\$lte", end))
        if (!usersId.isNullOrEmpty()) obj.append("userId", BasicDBObject("\$in", usersId))
        return obj
    }

    private fun aggregateLogins(
        matchObj: BasicDBObject,
        projectFields: BasicDBObject,
        groupFields: BasicDBObject,
        sortFields: BasicDBObject
    ): MutableList<Document> {
        val collection = getDb().getCollection("loginRegister")
        val pipeline = listOf(
            BasicDBObject("\$match", matchObj),
            BasicDBObject("\$project", projectFields),
            BasicDBObject("\$group", groupFields),
            BasicDBObject("\$sort", sortFields)
        )
        return collection.aggregate(pipeline).into(ArrayList())
    }

    private fun dailyProjectFields() = BasicDBObject("hour", BasicDBObject("\$dateToString", BasicDBObject("format", "%H").append("date", "\$loginDate")))
        .append("count", BasicDBObject("\$toInt", "1"))
        .append("userId", "\$userId")
        .append("loginDate", "\$loginDate")
        .append("weekDay", BasicDBObject("\$dayOfWeek", "\$loginDate"))
        .append("dateF", BasicDBObject("\$dateToString", BasicDBObject("format", "%d/%m/%Y").append("date", "\$loginDate")))

    private fun dailyGroupFields() = BasicDBObject("_id", "\$hour")
        .append("total", BasicDBObject("\$sum", "\$count"))
        .append("weekDay", BasicDBObject("\$first", "\$weekDay"))
        .append("date", BasicDBObject("\$first", "\$dateF"))

    private fun weeklyProjectFields() = BasicDBObject("day", BasicDBObject("\$dateToString", BasicDBObject("format", "%d").append("date", "\$loginDate")))
        .append("count", BasicDBObject("\$toInt", "1"))
        .append("userId", "\$userId")
        .append("loginDate", "\$loginDate")
        .append("dayString", BasicDBObject("\$dateToString", BasicDBObject("format", "%d").append("date", "\$loginDate")))
        .append("dateToSort", BasicDBObject("\$dateToString", BasicDBObject("format", "%m%d").append("date", "\$loginDate")))

    private fun weeklyGroupFields() = BasicDBObject("_id", "\$day")
        .append("total", BasicDBObject("\$sum", "\$count"))
        .append("dayString", BasicDBObject("\$first", "\$dayString"))
        .append("dateToSort", BasicDBObject("\$first", "\$dateToSort"))

    private fun monthlyProjectFields() = BasicDBObject("month", BasicDBObject("\$dateToString", BasicDBObject("format", "%m").append("date", "\$loginDate")))
        .append("count", BasicDBObject("\$toInt", "1"))
        .append("userId", "\$userId")
        .append("loginDate", "\$loginDate")

    private fun monthlyGroupFields() = BasicDBObject("_id", "\$month")
        .append("total", BasicDBObject("\$sum", "\$count"))

    private fun fillMissingDays(calendarInitial: Calendar, docs: MutableList<Document>) {
        repeat(7) {
            val day = calendarInitial.get(Calendar.DAY_OF_MONTH).toString()
            if (docs.none { it["_id"] == day }) {
                val monthString = (calendarInitial.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
                val dayString = calendarInitial.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
                docs.add(Document().apply {
                    this["_id"] = day
                    this["total"] = 0
                    this["dateToSort"] = monthString + dayString
                })
            }
            calendarInitial.add(Calendar.DAY_OF_MONTH, 1)
        }
        calendarInitial.add(Calendar.DAY_OF_MONTH, -7)
    }

    private fun fillMissingMonths(docs: MutableList<Document>) {
        for (month in 1..12) {
            if (docs.none { it["_id"].toString().toInt() == month }) {
                docs.add(Document().apply {
                    this["_id"] = month.toString().padStart(2, '0')
                    this["total"] = 0
                })
            }
        }
    }

    private fun Calendar.setToStartOfDay() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun Calendar.setToEndOfDay() {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }

}