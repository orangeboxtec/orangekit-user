package com.orangebox.kit.user.loginregister

import com.mongodb.BasicDBObject
import com.orangebox.kit.core.dao.AbstractDAO
import jakarta.enterprise.context.ApplicationScoped
import org.bson.Document
import java.text.SimpleDateFormat
import java.util.*

@ApplicationScoped
class LoginRegisterDAO: AbstractDAO<LoginRegister>(LoginRegister::class.java) {

    private val dayOfWeek = listOf("", "Domingo", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado")

    private val monthStringShort = listOf("", "Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago","Set", "Out", "Nov", "Dez")

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

        list.first()["weekDay"] = dayOfWeek[list.first()["weekDay"].toString().toInt()]

        val map = HashMap<String, Any>()
        map["labels"] = list.map { p-> p["_id"].toString().toInt() - 3}
        map["series"] = list.map { p-> p["total"] }
        map["weekDay"] = list.first()["weekDay"].toString()
        map["date"] = list.first()["date"].toString()

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

        val sdf = SimpleDateFormat("dd/MM/yyyy")

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

}