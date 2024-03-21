package com.orangebox.kit.user

import com.orangebox.kit.authkey.UserAuthKey
import com.orangebox.kit.authkey.UserAuthKeyService
import com.orangebox.kit.authkey.UserAuthKeyTypeEnum
import com.orangebox.kit.core.address.AddressInfo
import com.orangebox.kit.core.dao.OperationEnum
import com.orangebox.kit.core.dao.SearchBuilder
import com.orangebox.kit.core.dto.ResponseList
import com.orangebox.kit.core.exception.BusinessException
import com.orangebox.kit.core.utils.BusinessUtils
import com.orangebox.kit.core.utils.SecUtils
import com.orangebox.kit.notification.NotificationBuilder
import com.orangebox.kit.notification.NotificationService
import com.orangebox.kit.notification.TypeSendingNotificationEnum
import com.orangebox.kit.notification.email.data.EmailDataTemplate
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.util.*

@ApplicationScoped
class UserService {

    @Inject
    private lateinit var userAuthKeyService: UserAuthKeyService

    @Inject
    private lateinit var notificationService: NotificationService

    @Inject
    private lateinit var userDAO: UserDAO

    @ConfigProperty(name = "orangekit.user.email.welcome.templateid", defaultValue = "ERROR")
    private lateinit var welcomeEmailTemplateId: String

    @ConfigProperty(name = "orangekit.user.email.forgotpassword.templateid", defaultValue = "ERROR")
    private lateinit var forgotEmailTemplateId: String

    @ConfigProperty(name = "orangekit.core.projecturl", defaultValue = "http://localhost:3000")
    private lateinit var projectUrl: String

    @ConfigProperty(name = "orangekit.core.projectname", defaultValue = "Vcomm")
    private lateinit var projectName: String

    @ConfigProperty(name = "orangekit.core.projectlogo", defaultValue = "https://02m6l.mjt.lu/tplimg/02m6l/b/1zy63/x24wj.png")
    private lateinit var projectLogo: String

    @ConfigProperty(name = "orangekit.user.validatephone", defaultValue = "false")
    private lateinit var validatePhone: String

    @ConfigProperty(name = "orangekit.user.validatephone.message.pt", defaultValue = "false")
    private lateinit var validatePhoneMessage: String

    @ConfigProperty(name = "orangekit.user.email.confirmation.templateid", defaultValue = "ERROR")
    private lateinit var confirmationEmailTemplateId: String

    @ConfigProperty(name = "orangekit.user.email.resetPasswordTemplateId.templateid", defaultValue = "ERROR")
    private lateinit var resetPasswordTemplateId: String

    @ConfigProperty(name = "orangekit.user.email.forgotPasswordTemplateId.templateid", defaultValue = "ERROR")
    private lateinit var forgotPasswordTemplateId: String

    init {
        projectUrl = System.getenv("orangekit.core.projecturl") ?: "http://localhost:3000"
        projectLogo = System.getenv("orangekit.core.projectlogo")
                ?: "https://orangebox.technology/assets/img/logo_lado.png"
        projectName = System.getenv("orangekit.core.projectname") ?: "OrangeBox"
    }

    fun retrieveByEmail(email: String): User? {
        return userDAO.retrieve(userDAO.createBuilder()
                .appendParamQuery("email", email)
                .build())
    }

    fun retrieveByPhone(phoneNumber: Long): User? {
        return userDAO.retrieve(userDAO.createBuilder()
                .appendParamQuery("phoneNumber", phoneNumber)
                .build())
    }

    fun saveByPhone(user: User): User {
        var user = user
        if (user.phoneNumber == null) {
            throw BusinessException("missing_phone")
        }
        val userBase = retrieveByPhone(user.phoneNumber!!)
        return if (userBase == null) {
            user = createNewUser(user)
            user
        } else {
            userBase
        }
    }

    fun save(user: User) {
        if (user.id == null) {
            createNewUser(user)
        } else {
            updateFromClient(user)
        }
    }

    fun createNewUser(user: User): User {
        if (user.name == null) {
            throw BusinessException("name_required")
        }
        if (user.email == null) {
            throw BusinessException("email_required")
        }
        if (user.password == null && user.idFacebook == null && user.idGoogle == null && user.idApple == null && confirmationEmailTemplateId == "ERROR") {
            throw BusinessException("password_required")
        }
        var userDB: User? = null
        var fgPhoneError = false
        if (user.email != null && user.email != "") {
            userDB = retrieveByEmail(user.email!!)
        }
        if (userDB == null && user.phoneNumber != null && user.phoneNumber != 0L) {
            userDB = retrieveByPhone(user.phoneNumber!!)
            fgPhoneError = true
        }
        if (userDB != null) {
            if (user.idFacebook != null && user.idFacebook != "" && (userDB.idFacebook == null || userDB.idFacebook == "")) {
                userDB.idFacebook = user.idFacebook
                userDAO.update(userDB)
                return userDB
            }
            if (user.idGoogle != null && user.idGoogle != "" && (userDB.idGoogle == null || userDB.idGoogle == "")) {
                userDB.idGoogle = user.idGoogle
                userDAO.update(userDB)
                return userDB
            }
            if (user.idApple != null && user.idApple != "" && (userDB.idApple == null || userDB.idApple == "")) {
                userDB.idApple = user.idApple
                userDAO.update(userDB)
                return userDB
            }
            throw BusinessException("email_exists")
        }
        user.creationDate = Date()
        user.id = null
        if (user.password != null) {
            user.salt = SecUtils.salt
            user.password = SecUtils.generateHash(user.salt, user.password!!)
        }
        if (user.status == null) {
            user.status = "ACTIVE"
        }
        if (user.info != null && user.info!!.containsKey("idUserAnonymous") && user.type == "anonymous") {
            user.type = "user"
            user.id = user.info!!["idUserAnonymous"] as String?
            BusinessUtils(userDAO).basicSave(user)
        } else {
            userDAO.insert(user)
        }
        if (user.phone != null && validatePhone.toBoolean()) {
            confirmUserSMS(user.id!!)
        }
        sendWelcomeEmail(user)
        createToken(user)
        return user
    }

    fun saveAnonymous(user: User): User {
        if (user.id == null) {
            user.creationDate = Date()
            user.type = "anonymous"
            createToken(user)
            userDAO.insert(user)
            if (user.tokenFirebase?.isNotEmpty() == true) {
                user.info = HashMap()
                user.info!!["tokenFirebase"] = user.tokenFirebase!!
                BusinessUtils(userDAO).basicSave(user)
            }
        }
        return user
    }

    fun sendWelcomeEmail(user: User) {
        if (welcomeEmailTemplateId != "ERROR") {
            notificationService.sendNotification(
                    NotificationBuilder()
                            .setTo(user)
                            .setTypeSending(TypeSendingNotificationEnum.EMAIL)
                            .setFgAlertOnly(true)
                            .setEmailDataTemplate(object : EmailDataTemplate {
                                override val data: Map<String?, Any?>
                                    get() {
                                        val params: MutableMap<String?, Any?> = HashMap()
                                        params["user_name"] = user.name
                                        return params
                                    }
                                override val templateId: Int
                                    get() = welcomeEmailTemplateId.toInt()
                            })
                            .build()
            )
        }
    }

    protected fun validateUser(user: User) {
        if (user.email != null) {
            val userDB = userDAO.retrieve(userDAO.createBuilder()
                    .appendParamQuery("email", user.email!!)
                    .build())
            if (userDB != null && userDB.id != user.id) {
                throw BusinessException("email_exists")
            }
        }
    }

    fun retrieveByIdFacebook(idFacebook: String): User? {
        return userDAO.retrieveByIdFacebook(idFacebook)
    }

    fun loginFB(user: User): User? {
        val userDB = userDAO.retrieveByIdFacebook(user.idFacebook!!)
        return loginSocial(user, userDB)
    }

    fun loginGoogle(user: User): User? {
        val userDB = userDAO.retrieveByIdGoogle(user.idGoogle!!)
        return loginSocial(user, userDB)
    }

    fun loginApple(user: User): User? {
        val userDB = userDAO.retrieveByIdApple(user.idApple!!)
        return loginSocial(user, userDB)
    }

    protected fun loginSocial(userLogin: User, userDB: User?): User {
        val user: User
        if (userDB == null) {
            user = createNewUser(userLogin)
        } else {
            user = userDB
            if (userDB.status != null && userDB.status == "BLOCKED") {
                throw BusinessException("user_blocked")
            }
            createToken(userDB)
        }
        return user
    }

    fun retrieve(id: String): User? {
        return userDAO.retrieve(id)
    }

    fun checkEmailExists(email: String): Boolean {
        val user = retrieveByEmail(email)
        return if (user != null) {
            !(user.type != null && user.type == "anonymous")
        } else {
            false
        }
    }


    fun autoLogin(user: User): User? {
        val userDB = userDAO.retrieve(user.id!!)
        if (userDB == null || user.password == null && user.idFacebook == null && user.idGoogle == null && user.idApple == null) {
            throw BusinessException("user_not_found")
        }
        if (user.password != null && (userDB.password == null || user.password != userDB.password)) {
            throw BusinessException("invalid_user_password")
        }
        if (user.idFacebook != null && (userDB.idFacebook == null || user.idFacebook != userDB.idFacebook)) {
            throw BusinessException("invalid_user_password")
        }
        if (user.idGoogle != null && (userDB.idGoogle == null || user.idGoogle != userDB.idGoogle)) {
            throw BusinessException("invalid_user_password")
        }
        if (user.idApple != null && (userDB.idApple == null || user.idApple != userDB.idApple)) {
            throw BusinessException("invalid_user_password")
        }
        if (userDB.status != null && userDB.status == "BLOCKED") {
            throw BusinessException("user_blocked")
        }
        createToken(userDB)
        return userDB
    }

    fun login(user: User): User? {
        return login(user, user.password!!)
    }

    protected fun login(user: User, password: String): User? {
        val userDB = retrieveByEmail(user.email!!) ?: throw BusinessException("invalid_user_password")
        if (user.type?.isNotEmpty() == true && user.type != userDB.type) {
            throw BusinessException("invalid_user_Type")
        }
        if (userDB.status != null && userDB.status == "BLOCKED") {
            throw BusinessException("user_blocked")
        }
        val passHash: String = SecUtils.generateHash(userDB.salt, password)
        if (userDB.password != passHash) {
            throw BusinessException("invalid_user_password")
        } else {
            createToken(userDB)
            var listTokens = ArrayList<UserToken>()
            for (i in userDB.userTokens!!) {
                var index = userDB.userTokens!!.indexOf(i)
                if (userDB.userTokens!![index].tokenExpirationDate?.after(Date()) == true) {
                    listTokens.add(i)
                }
            }
            userDB.userTokens = listTokens
            userDAO.update(userDB)
        }
        return userDB
    }

    fun logout(idUser: String) {
        val user = retrieve(idUser)
        if (user != null) {
            user.token = null
            user.tokenExpirationDate = null
            update(user)
        }
    }

    fun updateFromClient(user: User) {
        if (user.id == null) {
            throw BusinessException("user_id_required")
        }
        val userBase: User = userDAO.retrieve(user) ?: throw BusinessException("user_not_found")
        validateUser(user)
        if (user.phone != null && userBase.phone != user.phone) {
            if (validatePhone.toBoolean()) {
                confirmUserSMS(user.id!!)
            }
        }
        BusinessUtils(userDAO).basicSave(user)
    }

    fun update(user: User) {
        userDAO.update(user)
    }

    fun updatePassword(user: User): String? {
        var userBase: User? = null
        if (user.oldPassword != null) {
            userBase = login(user, user.oldPassword!!)
        } else {
            userBase = userDAO.retrieve(user.id!!)
        }
        if (userBase != null) {
            userBase.salt = SecUtils.salt
            userBase.password = SecUtils.generateHash(userBase.salt, user.password!!)
            update(userBase)
        }
        return userBase!!.password
    }

    fun listUserIn(userIds: List<String>): List<User>? {
        return userDAO.search(userDAO.createBuilder()
                .appendParamQuery("id", userIds, OperationEnum.IN)
                .build())
    }

    fun generateCard(idUser: String): UserCard? {
        val user = userDAO.retrieve(idUser)
        return user?.let { generateCard(it) }
    }

    fun generateCard(user: User): UserCard {
        val card = UserCard()
        generateCard(user, card)
        return card
    }

    protected fun generateCard(user: User, card: UserCard) {
        card.id = user.id
        card.name = user.name
        card.email = user.email
        if (user.phoneNumber != null) {
            card.phone = user.phoneNumber.toString()
        }
        card.idObj = user.idObj
        card.nameObj = user.nameObj
        card.creationDate = user.creationDate
        card.status = user.status
        card.type = user.type
    }

    fun updateStartInfo(userStartInfo: UserStartInfo) {
        val user = userDAO.retrieve(User(userStartInfo.idUser))
        if (user != null) {
            user.lastLogin = Date()
            if (userStartInfo.tokenFirebase != null) {
                user.tokenFirebase = userStartInfo.tokenFirebase
            }
            if (userStartInfo.language != null) {
                user.language = userStartInfo.language
            }
            if (userStartInfo.latitude != null) {
                if (user.lastAddress == null) {
                    user.lastAddress = AddressInfo()
                }
                user.lastAddress!!.latitude = userStartInfo.latitude
                user.lastAddress!!.longitude = userStartInfo.longitude
            }
            userDAO.update(user)
        }
    }

    fun updatePhoneUser(user: User): User? {
        val userBase = userDAO.retrieve(User(user.id))
        if (userBase != null) {
            userBase.phoneNumber = user.phoneNumber
            userBase.phone = user.phone
            userDAO.update(userBase)
        }
        return userBase
    }

    fun confirmUserSMS(idUser: String) {
        val user = retrieve(idUser)
        val key = userAuthKeyService.createKey(idUser, UserAuthKeyTypeEnum.SMS)
        val message = validatePhoneMessage + key.key
        notificationService.sendNotification(
                NotificationBuilder()
                        .setTo(user)
                        .setTypeSending(TypeSendingNotificationEnum.SMS)
                        .setMessage(message)
                        .setFgAlertOnly(true)
                        .build()
        )
    }

    fun confirmUserEmail(idUser: String) {
        if (confirmationEmailTemplateId != "ERROR") {
            val user = retrieve(idUser)
            if (user != null) {
                val key = userAuthKeyService.createKey(idUser, UserAuthKeyTypeEnum.EMAIL)
                val language = if (user.language == null) "" else "_" + user.language!!.uppercase(Locale.getDefault())
                val link = "$projectUrl/pages/confirm_user?l=$language&k=${key.key!!}&u=${user.id!!}&t=${key.type}"
                sendNotification(user, null, confirmationEmailTemplateId.toInt(), link, null)
            }
        }
    }

    fun validateKey(key: UserAuthKey): Boolean {
        val validate = userAuthKeyService.validateKey(key)
        if (validate) {
            val user = userDAO.retrieve(User(key.idUser))
            if (user != null) {
                user.userConfirmed = true
                if (key.type == UserAuthKeyTypeEnum.EMAIL) {
                    user.emailConfirmed = true
                } else {
                    user.phoneConfirmed = true
                }
                userDAO.update(user)
            }
        }
        return validate
    }

    fun forgotPassword(email: String) {
        // val logo = configurationService.loadByCode("PROJECT_LOGO_URL")?.value
        val user = retrieveByEmail(email) ?: throw BusinessException("user_not_found")
        val key: UserAuthKey = userAuthKeyService.createKey(user.id!!, UserAuthKeyTypeEnum.EMAIL)
        if (user.language == null) {
            user.language = "pt"
            update(user)
        }
        val link = "$projectUrl/reset-password?l=${user.language}&k=${key.key}&u=${user.id}&t=${key.type}"
        if (forgotEmailTemplateId == "ERROR") {
            throw IllegalArgumentException("orangekit.user.email.forgotpassword.templateid must be provided in .env")
        }
        notificationService.sendNotification(
                NotificationBuilder()
                        .setTo(user)
                        .setTypeSending(TypeSendingNotificationEnum.EMAIL)
                        .setFgAlertOnly(true)
                        .setEmailDataTemplate(object : EmailDataTemplate {
                            override val data: Map<String?, Any?>
                                get() {
                                    val params: MutableMap<String?, Any?> = HashMap()
                                    params["user_name"] = user.name
                                    params["confirmation_link"] = link
                                    params["project_name"] = projectName
                                    params["project_logo"] = projectLogo
                                    return params
                                }
                            override val templateId: Int
                                get() = forgotEmailTemplateId.toInt()
                        })
                        .build()
        )
    }

    protected fun sendNotification(user: User, title: String?, emailTemplateId: Int, link: String?, msg: String?) {
        notificationService.sendNotification(
                NotificationBuilder()
                        .setTo(user)
                        .setTypeSending(TypeSendingNotificationEnum.EMAIL)
                        .setTitle(title)
                        .setFgAlertOnly(true)
                        .setEmailDataTemplate(object : EmailDataTemplate {
                            override val templateId: Int
                                get() = emailTemplateId
                            override val data: Map<String?, Any?>?
                                get() {
                                    val params: MutableMap<String?, Any?> = HashMap()
                                    params["user_name"] = user.name
                                    params["confirmation_link"] = link
                                    params["msg"] = msg
                                    return params
                                }
                        })
                        .build()
        )
    }

    fun searchByName(name: String): List<UserCard>? {
        val listUsers = userDAO.search(
                userDAO.createBuilder()
                        .appendParamQuery("name", name, OperationEnum.LIKE)
                        .setMaxResults(10)
                        .build()
        )
        if (listUsers != null) {
            val list: List<UserCard>
            list = ArrayList()
            for (user in listUsers) {
                list.add(generateCard(user))
            }
            return list
        }
        return null
    }

    fun listAll(): List<UserCard>? {
        val listUsers = userDAO.listAll()
        if (listUsers != null) {
            var list: MutableList<UserCard>
            list = ArrayList()
            for (user in listUsers) {
                list.add(generateCard(user))
            }
            return list
        }
        return null
    }

    fun cancelUser(idUser: String) {
        val user = retrieve(idUser)
        if (user != null) {
            user.status = "BLOCKED"
            update(user)
        }
    }

    @Throws(Exception::class)
    fun checkToken(token: String): Boolean {
        val user = retrieveByToken(token) ?: return false
        val userToken = user.userTokens!!.stream()
                .filter { p: UserToken -> p.token == token }
                .findFirst()
                .orElse(null)
        return userToken != null && !user.tokenExpirationDate!!.before(Date())
    }

    fun retrieveByToken(token: String): User? {
        return userDAO.retrieve(
                userDAO.createBuilder()
                        .appendParamQuery("userTokens.token", token)
                        .build()
        )
    }

    protected fun createToken(userDB: User) {
        val expCal: Calendar = Calendar.getInstance()
        expCal.add(Calendar.HOUR, 12)
        if (userDB.userTokens == null) {
            userDB.userTokens = ArrayList()
        }
        val userToken = UserToken()
        userToken.token = UUID.randomUUID().toString()
        userToken.tokenExpirationDate = expCal.getTime()
        userDB.userTokens!!.add(userToken)
        userDB.token = userToken.token
        userDB.tokenExpirationDate = userToken.tokenExpirationDate
        userDAO.update(userDB)
    }


    fun customersByRadius(
            latitude: Double,
            longitude: Double,
            distanceKM: Int
    ): List<User>? {
        return userDAO.search(
                userDAO.createBuilder()
                        .appendParamQuery(
                                "lastAddress", arrayOf(latitude, longitude, distanceKM.toDouble()),
                                OperationEnum.GEO
                        )
                        .build()
        )
    }

    fun listByFieldInfo(
            field: String,
            value: String
    ): List<User>? {
        return userDAO.listByFieldInfo(field, value)
    }

    fun changeStatus(idUser: String) {
        val user = retrieve(idUser)
        if (user != null) {
            if (user.status == "ACTIVE") {
                user.status = "BLOCKED"
            } else {
                user.status = "ACTIVE"
            }
            userDAO.update(user)
        }
    }

    fun saveByAdmin(user: User) {
        val sendEmail = user.id == null
        save(user)
        if (sendEmail) {
            sendWelcomeEmail(user)
        }
    }

    fun search(search: UserSearch): List<UserCard>? {
        val searchBuilder = userDAO.createBuilder()
        searchBuilder.appendParamQuery("status", "ACTIVE")
        if (search.queryString != null) {
            searchBuilder.appendParamQuery("name", search.queryString!!)
        }
        if (search.code != null) {
            searchBuilder.appendParamQuery("code", search.code!!)
        }
        searchBuilder.setFirst(TOTAL_PAGE * (search.page!! - 1))
        searchBuilder.setMaxResults(TOTAL_PAGE)

        //ordena
        val list: MutableList<UserCard>
        val listUsers = userDAO.search(searchBuilder.build())
        if (listUsers != null) {
            list = ArrayList()
            for (user in listUsers) {
                list.add(generateCard(user))
            }
            return list
        }
        return null
    }

    fun listActives(): List<UserCard>? {
        val listUsers: List<User>?
        val searchBuilder = userDAO.createBuilder()
        searchBuilder.appendParamQuery("status", "ACTIVE")
        listUsers = userDAO.search(searchBuilder.build())
        val list: MutableList<UserCard>?
        if (listUsers != null) {
            list = ArrayList()
            for (user in listUsers) {
                list.add(generateCard(user))
            }
            return list
        }
        return null
    }

    fun searchAdmin(userSearch: UserSearch): ResponseList<UserCard> {
        return if (userSearch.page == null) {
            throw BusinessException("missing_page")
        } else {
            val sb = userDAO.createBuilder()
            if (userSearch.status != null) {
                sb.appendParamQuery("status", userSearch.status!!)
            }
            if (userSearch.type != null) {
                sb.appendParamQuery("type", userSearch.type!!)
            }
            if (userSearch.code != null) {
                sb.appendParamQuery("code", userSearch.code!!)
            }
            if (userSearch.typeIn != null) {
                sb.appendParamQuery("type", userSearch.typeIn!!, OperationEnum.IN)
            }
            if (userSearch.queryString != null && userSearch.queryString!!.isNotEmpty()) {
                sb.appendParamQuery(
                        "name|nameObj|addressInfo.city",
                        userSearch.queryString!!,
                        OperationEnum.OR_FIELDS_LIKE
                )
            }
            if (userSearch.creationDate != null) {
                sb.appendParamQuery("creationDate", userSearch.creationDate!!, OperationEnum.GT)
            }
            if (userSearch.type != null) {
                sb.appendParamQuery("type", "anonymous", OperationEnum.NOT)
            }
            if (userSearch.pageItensNumber != null && userSearch.pageItensNumber!! > 0) {
                sb.setFirst(userSearch.pageItensNumber!! * (userSearch.page!! - 1))
                sb.setMaxResults(userSearch.pageItensNumber)
            } else {
                sb.setFirst(10 * (userSearch.page!! - 1))
                sb.setMaxResults(10)
            }
            sb.appendSort("creationDate", -1)
            val listUsers = userDAO.search(sb.build())
            val totalAmount = totalAmount(sb)
            val pageQuantity = if (userSearch.pageItensNumber != null && userSearch.pageItensNumber!! > 0) {
                pageQuantity(userSearch.pageItensNumber!!, totalAmount)
            } else {
                pageQuantity(10, totalAmount)
            }
            val list: MutableList<UserCard> = ArrayList()
            if (!listUsers.isNullOrEmpty()) {
                for (user in listUsers) {
                    list.add(generateCard(user))
                }
            }
            val result = ResponseList<UserCard>()
            result.list = list
            result.totalAmount = totalAmount
            result.pageQuantity = pageQuantity
            result
        }
    }

    protected fun totalAmount(sb: SearchBuilder): Long {
        return userDAO.count(sb.build())
    }

    protected fun pageQuantity(numberOfItensByPage: Int, totalAmount: Long): Long {
        val pageQuantity = if (totalAmount % numberOfItensByPage != 0L) {
            totalAmount / numberOfItensByPage + 1
        } else {
            totalAmount / numberOfItensByPage
        }
        return pageQuantity
    }

    fun userSearchById(id: String): User? {
        val userSelect = userDAO.retrieve(id)
        return if (userSelect != null) {
            val user = User()
            user.id = userSelect.id
            user.name = userSelect.name
            user.email = userSelect.email
            if (userSelect.cpf != null) {
                user.cpf = userSelect.cpf
            }
            if (userSelect.document != null) {
                user.document = userSelect.document
            }
            if (userSelect.birthDate != null) {
                user.birthDate = userSelect.birthDate
            }
            if (userSelect.phone != null) {
                user.phone = userSelect.phone
            }
            if (userSelect.phoneNumber != null) {
                user.phoneNumber = userSelect.phoneNumber
            }
            if (userSelect.lastAddress != null) {
                user.lastAddress = userSelect.lastAddress
            }
            if (userSelect.info != null) {
                user.info = userSelect.info
            }
            user
        } else {
            null
        }
    }

    fun setUserImage(search: UserSearch) {
        val user = retrieve(search.idUser!!)
        if (user != null) {
            for (i in user.gallery!!.indices) {
                if (search.idImg == user.gallery!![i].id) {
                    user.urlImage = user.gallery!![i].urlFile
                    userDAO.update(user)
                }
            }
        }
    }

    fun removeGallery(userSearch: UserSearch) {
        val userList = userDAO.listAll()
        if (userList != null) {
            for (i in userList.indices) {
                if (userSearch.idUser == userList[i].id) {
                    for (j in userList[i].gallery!!.indices) {
                        if (userSearch.idImg == userList[i].gallery!![j].id) {
                            userList[i].gallery?.removeAt(j)
                            userDAO.update(userList[i])
                        }
                    }
                }
            }
        }
    }

    fun getUserById(id: String): User {
        val user = retrieve(id) ?: throw BusinessException("usuário não encontrado")
        val getUser = User()
        getUser.name = user.name
        getUser.lastAddress = user.lastAddress
        getUser.info = user.info
        getUser.urlImage = user.urlImage
        getUser.gallery = user.gallery
        getUser.gender = user.gender
        return getUser
    }

    fun setStatus(id: String, status: String) {
        val user = userDAO.retrieve(id) ?: throw BusinessException("user_not_found")
        user.status = status
        userDAO.update(user)
    }

    fun newUserSendEmailResetPassword(user: User): User {
        if (user.name == null) {
            throw BusinessException("name_required")
        }
        if (user.email == null) {
            throw BusinessException("email_required")
        }
        if (user.password == null && user.idFacebook == null && user.idGoogle == null && user.idApple == null && confirmationEmailTemplateId == "ERROR") {
            throw BusinessException("password_required")
        }
        var userDB: User? = null
        var fgPhoneError = false
        if (user.email != null && user.email != "") {
            userDB = retrieveByEmail(user.email!!)
        }
        if (userDB == null && user.phoneNumber != null && user.phoneNumber != 0L) {
            userDB = retrieveByPhone(user.phoneNumber!!)
            fgPhoneError = true
        }
        if (userDB != null) {

            if (user.idFacebook != null && user.idFacebook != "" && (userDB.idFacebook == null || userDB.idFacebook == "")) {
                userDB.idFacebook = user.idFacebook
                userDAO.update(userDB)
                return userDB
            }
            if (user.idGoogle != null && user.idGoogle != "" && (userDB.idGoogle == null || userDB.idGoogle == "")) {
                userDB.idGoogle = user.idGoogle
                userDAO.update(userDB)
                return userDB
            }
            if (user.idApple != null && user.idApple != "" && (userDB.idApple == null || userDB.idApple == "")) {
                userDB.idApple = user.idApple
                userDAO.update(userDB)
                return userDB
            }
            if(userDB.idGoogle != null){
                throw BusinessException("email_exists_google_login")
            }
            throw BusinessException("email_exists")
        }
        user.creationDate = Date()
        user.id = null
        if (user.password != null) {
            user.salt = SecUtils.salt
            user.password = SecUtils.generateHash(user.salt, user.password!!)
        }
        if (user.status == null) {
            user.status = "ACTIVE"
        }
        if (user.info != null && user.info!!.containsKey("idUserAnonymous") && user.type == "anonymous") {
            user.type = "user"
            user.id = user.info!!["idUserAnonymous"] as String?
            BusinessUtils(userDAO).basicSave(user)
        } else {
            userDAO.insert(user)
        }
        if (user.phone != null && validatePhone.toBoolean()) {
            confirmUserSMS(user.id!!)
        }
        if(user.idGoogle == null && user.idFacebook == null && user.idApple == null){
            confirmEmailLinkResetPassword(user.email!!)
        }
        createToken(user)
        return user
    }

    fun confirmEmailLinkResetPassword(email: String) {
        // val logo = configurationService.loadByCode("PROJECT_LOGO_URL")?.value
        val user = retrieveByEmail(email) ?: throw BusinessException("user_not_found")
        val key: UserAuthKey = userAuthKeyService.createKey(user.id!!, UserAuthKeyTypeEnum.EMAIL)
        if (user.language == null) {
            user.language = "pt"
            update(user)
        }
        val link = "$projectUrl/pages/reset-password?l=${user.language}&k=${key.key}&u=${user.id}&t=${key.type}"
        if (resetPasswordTemplateId == "ERROR") {
            throw IllegalArgumentException("orangekit.user.email.resetPasswordTemplateId.templateid must be provided in .env")
        }
        notificationService.sendNotification(
            NotificationBuilder()
                .setTo(user)
                .setTypeSending(TypeSendingNotificationEnum.EMAIL)
                .setFgAlertOnly(true)
                .setEmailDataTemplate(object : EmailDataTemplate {
                    override val data: Map<String?, Any?>
                        get() {
                            val params: MutableMap<String?, Any?> = HashMap()
                            params["user_name"] = user.name
                            params["confirmation_link"] = link
                            params["project_name"] = projectName
                            params["project_logo"] = projectLogo
                            return params
                        }
                    override val templateId: Int
                        get() = resetPasswordTemplateId.toInt()
                })
                .build()
        )
    }

    fun forgotPasswordVerifySocialMedia(email: String){
        val user = retrieveByEmail(email) ?: throw BusinessException("user_not_found")
        if(user.idGoogle != null || user.idApple != null || user.idFacebook != null){
            throw BusinessException("user_login_with_social_media")
        }

        val key: UserAuthKey = userAuthKeyService.createKey(user.id!!, UserAuthKeyTypeEnum.EMAIL)
        if (user.language == null) {
            user.language = "pt"
            update(user)
        }
        val link = "$projectUrl/pages/reset-password?l=${user.language}&k=${key.key}&u=${user.id}&t=${key.type}"
        if (forgotPasswordTemplateId == "ERROR") {
            throw IllegalArgumentException("orangekit.user.email.resetPasswordTemplateId.templateid must be provided in .env")
        }
        notificationService.sendNotification(
            NotificationBuilder()
                .setTo(user)
                .setTypeSending(TypeSendingNotificationEnum.EMAIL)
                .setFgAlertOnly(true)
                .setEmailDataTemplate(object : EmailDataTemplate {
                    override val data: Map<String?, Any?>
                        get() {
                            val params: MutableMap<String?, Any?> = HashMap()
                            params["user_name"] = user.name
                            params["confirmation_link"] = link
                            params["project_name"] = projectName
                            params["project_logo"] = projectLogo
                            return params
                        }
                    override val templateId: Int
                        get() = forgotPasswordTemplateId.toInt()
                })
                .build()
        )
    }

    fun validateKeyAngular(key: UserAuthKey): User? {
        val validate: Boolean = userAuthKeyService.validateKey(key)
        if (validate) {
            val user = userDAO.retrieve(User(key.idUser))!!
            user.userConfirmed = true
            if (key.type!! == UserAuthKeyTypeEnum.EMAIL) {
                user.emailConfirmed = true
            } else {
                user.phoneConfirmed = true
            }
            generateSession(user)
            userDAO.update(user)
            return user
        }
        throw BusinessException("invalid_key")
    }

    fun generateSession(user: User){
        user.token = UUID.randomUUID().toString()
        val expCal = Calendar.getInstance()
        expCal.add(Calendar.HOUR, 12)
        user.tokenExpirationDate = expCal.time
        userDAO.update(user)

    }

    fun updatePasswordForgot(user: User) {
        val userBase = userDAO.retrieve(user.id!!)
        if (userBase != null) {
            val nullPasword = userBase.password == null
            userBase.salt = SecUtils.salt
            userBase.password = SecUtils.generateHash(userBase.salt, user.password!!)
            userDAO.update(userBase)
        }
    }

    fun searchUser(userSearch: UserSearch):List<User> {
        val searchBuilder = userDAO.createBuilder()

        if(userSearch.queryString != null){
            searchBuilder.appendParamQuery("name|lastName|email|cpf", userSearch.queryString!!, OperationEnum.OR_FIELDS_LIKE)
        }
        if(userSearch.name != null){
            searchBuilder.appendParamQuery("name", userSearch.name!!, OperationEnum.LIKE)
        }
        if(userSearch.email != null){
            searchBuilder.appendParamQuery("email", userSearch.email!!, OperationEnum.LIKE)
        }
        if(userSearch.cpf != null){
            searchBuilder.appendParamQuery("cpf", userSearch.cpf!!)
        }
        if(userSearch.document != null){
            searchBuilder.appendParamQuery("document", userSearch.document!!)
        }
        if(userSearch.phoneNumber != null){
            searchBuilder.appendParamQuery("phone", userSearch.phoneNumber!!)
        }
        if(userSearch.type != null){
            searchBuilder.appendParamQuery("type", userSearch.type!!)
        }
        if(userSearch.admin != null){
            searchBuilder.appendParamQuery("admin ", userSearch.admin!!)
        }
        if(userSearch.status != null){
            searchBuilder.appendParamQuery("status", userSearch.status!!)
        }

        val userList = userDAO.search(searchBuilder.build())

        val returnList = ArrayList<User>()

        userList?.forEach {
            val user = User()
            user.id = it.id
            user.name = it.name
            user.email = it.email
            if (it.cpf != null) {
                user.cpf = it.cpf
            }
            if (it.document != null) {
                user.document = it.document
            }
            if (it.birthDate != null) {
                user.birthDate = it.birthDate
            }
            if (it.phone != null) {
                user.phone = it.phone
            }
            if (it.phoneNumber != null) {
                user.phoneNumber = it.phoneNumber
            }
            if (it.lastAddress != null) {
                user.lastAddress = it.lastAddress
            }
            if (it.info != null) {
                user.info = it.info
            }

            returnList.add(user)
        }

        return returnList
    }


    companion object {
        private const val TOTAL_PAGE = 10
    }
}