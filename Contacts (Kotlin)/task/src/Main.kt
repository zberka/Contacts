package contacts

import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess

/*
NOTE:   add to build.gradle
        implementation "com.squareup.moshi:moshi:1.12.0" // use the latest version
        implementation "com.squareup.moshi:moshi-kotlin:1.12.0" // for Kotlin support
 */

enum class ACTION_MENU {
    add,
    list,
    search,
    count,
    save,
    load,
    exit
}


enum class ACTION_SEARCH {
    back,
    again,
}

enum class ACTION_RECORD {
    edit,
    delete,
    menu,
}

enum class ACTION_LIST {
    back,
}


var allContacts = Contacts()
//var DBFile = "phonebook.json"
var DBFile = ""

fun viewMainMenu() {
    print("\n[menu] Enter action (add, list, search, count, exit):")
    val actionString = readln()
    val action: ACTION_MENU? = enumValues<ACTION_MENU>().find { it.name == actionString }

    when(action) {
        ACTION_MENU.exit -> exitProcess(0)
        ACTION_MENU.add -> {
            print("Enter the type (person, organization):")
            val typeString = readln()
            val type: TYPE? = enumValues<TYPE>().find { it.name == typeString }

            when (type) {
                TYPE.person -> {
                    val newPerson = Person.enterPerson()
                    allContacts.add(newPerson)
                }
                TYPE.organization -> {
                    val newOrganization = Organization.enterOrganization()
                    allContacts.add(newOrganization)


                }
                else -> {}
            }
            saveToFile(DBFile)

        }
        ACTION_MENU.list ->  viewList()
        ACTION_MENU.search -> viewSearch()
        ACTION_MENU.save -> {
            saveToFile(DBFile)
        }
        ACTION_MENU.load -> {
            loadFromFile(DBFile)
        }
        ACTION_MENU.count -> println("The Phone Book has ${allContacts.count()} records.")
        null -> println("Incorrect Input")
    }
}

fun viewList() {

    allContacts.printList()
    println("\n[list] Enter action ([number], back):");

    val actionString = readln()
    val action: ACTION_LIST? = enumValues<ACTION_LIST>().find { it.name == actionString }

    when {
        action == ACTION_LIST.back -> return
        actionString.toInt() in  1 .. allContacts.count() -> {

            val recordIndex = actionString.toInt() - 1
            allContacts.printDetail(recordIndex)
            viewRecord(recordIndex)
            return
        }

    }

}

fun viewSearch() {

    while (true ) {
        print("Enter search query:")
        val searchText = readln()

        var countSearch = allContacts.search(searchText)

        print("\n[search] Enter action ([number], back, again):")
        val actionString = readln()
        val action: ACTION_SEARCH? = enumValues<ACTION_SEARCH>().find { it.name == actionString }

        when {
            action == ACTION_SEARCH.back -> return
            action == ACTION_SEARCH.again -> continue
            actionString.toInt() in  1 .. countSearch -> {

                var foundIndex = allContacts.getFoundIndex(actionString.toInt()-1)
                allContacts.printDetail(foundIndex)
                viewRecord(foundIndex)
                return
            }

        }
    }

}

fun viewRecord(index : Int) {

    while (true ) {
        print("\n[record] Enter action (edit, delete, menu):")
        val actionString = readln()
        val action: ACTION_RECORD? = enumValues<ACTION_RECORD>().find { it.name == actionString }

        when(action) {
            ACTION_RECORD.edit -> {
                allContacts.edit(index)
                allContacts.printDetail(index)
                saveToFile(DBFile)
            }
            ACTION_RECORD.delete -> {
                allContacts.remove(index)
                saveToFile(DBFile)
            }
            ACTION_RECORD.menu -> return
            null -> println("Incorrect Input")
        }

    }
}


//------------------- JSON ---------------------------

class LocalDateTimeAdapter {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @ToJson
    fun toJson(value: LocalDateTime): String {
        return value.format(formatter)
    }

    @FromJson
    fun fromJson(value: String): LocalDateTime {
        return LocalDateTime.parse(value, formatter)
    }
}



//Create a Polymorphic Json Adapter
//You'll need a custom adapter that can serialize and deserialize objects of the abstract Contact class
// into the correct subclasses.
class PolymorphicContactAdapter {
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .add(LocalDateTimeAdapter())
        .build()

    private val personAdapter: JsonAdapter<Person> = moshi.adapter(Person::class.java)
    private val organizationAdapter: JsonAdapter<Organization> = moshi.adapter(Organization::class.java)

    @ToJson
    fun toJson(writer: JsonWriter, value: Contact?) {
        when (value) {
            is Person -> personAdapter.toJson(writer, value)
            is Organization -> organizationAdapter.toJson(writer, value)
            else -> throw IllegalArgumentException("Unknown type: $value")
        }
    }

    @FromJson
    fun fromJson(reader: JsonReader): Contact? {
        val jsonObject = reader.readJsonValue() as Map<*, *>
        return when {
            "name" in jsonObject -> personAdapter.fromJsonValue(jsonObject)
            "organizationName" in jsonObject -> organizationAdapter.fromJsonValue(jsonObject)
            else -> throw IllegalArgumentException("Unknown type in JSON: $jsonObject")
        }
    }
}

// Vytvoření instance Moshi
val moshi = Moshi.Builder()
    .add(LocalDateTimeAdapter())
    .add(PolymorphicContactAdapter())
    .add(KotlinJsonAdapterFactory())
    .build()



private fun loadFromFile(datafile : String) {
    var file = File(datafile)
    if (file.exists()) {
        val jsonText = file.readText()
        println("open $datafile")
        allContacts.fromJSON(jsonText)
    }
}

private fun saveToFile(datafile : String) {
    if (datafile.isNotEmpty()) {
        var file = File(datafile)
        val json = allContacts.toJSON()
        file.writeText(json)
        println("save $datafile")
    }
}



enum class TYPE {
    person,
    organization,
}

fun testRegex() {
    //val pattern = Regex("""(\+)?(\(\w{1,}\)|\w{1,})(\s|-)?(\(\w{2,}\)|\w{2,})?(\s|-)?(\w{2,})?(\s|-)?(\w{2,})?""")
    //val pattern = Regex("""(\+)?(\(\w+\)|\w+)([\s-]\w{2,})*""")
    //val pattern = Regex("""(\+)?(\(\w+\)|\w+)([\s-](\(\w{2,}\)|\w{2,}))*""")
//    val pattern = Regex("""^\+?((\(\w{1,}\))|\w+)(([\s-](\(\w{2,}\))|[\s-]\w{2,}){0,1}([\s-]\w{2,})*)$""")
    val pattern = Regex("""^\+?(\(\w{1,3}\)|\w{1,3})([\s-]\w{2,})*$""")

    val testNumbers = listOf(
        "+0 (123) 456-789-ABcd",
        "(123) 234 345-456",
        "123-456-78",
        "+0(123)456-789-9999",
        "123+456 78912",
        "(123)-456-(78912)",
        "+(123) (123)"
    )

    for (number in testNumbers) {
        if (pattern.matches(number)) {
            println("$number matches")
        } else {
            println("$number does not match")
        }
    }
}

val pattern = Regex("""^\+?((\(\w{1,}\))|\w+)(([\s-](\(\w{2,}\))|[\s-]\w{2,}){0,1}([\s-]\w{2,})*)$""")
val pattern2 = Regex("""^\+\(\d*\)\s\(\d*\)$""")
val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

//-----------------------------------------------------
// NEW DEFINITION
//-----------------------------------------------------
abstract class Contact(
    open var _number: String,
    open var timeCreated: LocalDateTime,
    open var timeLastEdit: LocalDateTime
) {
    var number: String
        get() = _number.ifEmpty { "[no number]" }
        set(value) {
            // validation
            if (pattern.matches(value)) _number = value
            else {
                _number = ""
                println("Wrong number format!")
            }
            // Exception "+(123) (123)"
            if (pattern2.matches(value)) {
                _number = ""
                println("Wrong number format!")
            }
        }

    abstract fun title() : String
    abstract fun searchInput() : String
}

data class Person(
    var name: String,
    var surname: String,
    var _birthDate: String?,  // nullable, as it can be "[no data]"
    var _gender: String?,     // nullable, as it can be "[no data]"
    override var _number: String,
    override var timeCreated: LocalDateTime = LocalDateTime.now(),
    override var timeLastEdit: LocalDateTime = LocalDateTime.now()
) : Contact(_number, timeCreated, timeLastEdit) {

    // Secondary Empty constructor with no parameters
    constructor() : this(
        name = "",
        surname = "",
        _birthDate = null,
        _gender = null,
        _number = "",
        timeCreated = LocalDateTime.now(),
        timeLastEdit = LocalDateTime.now()
    )


    var birthDate: String?
        get() = if (_birthDate == null) "[no data]" else _birthDate!!
        set(value) {
            // validation
            if (value?.isEmpty() != true) _birthDate = value
            else {
                _birthDate = null
                println("Bad birth date!")
            }
        }

    var gender: String
        get() = if (_gender == null) "[no data]" else _gender!!
        set(value) {
            // validation
            if (value == "M" || value == "F") _gender = value
            else {
                _gender = null
                println("Bad gender!")
            }
        }

    companion object {
        fun enterPerson(): Person {
            val p = Person()
            print("Enter the name:")
            val name = readln()
            p.name = name
            print("Enter the surname:")
            val surname = readln()
            p.surname = surname
            print("Enter the birth date:")
            val birthDate = readln()
            p.birthDate = birthDate
            print("Enter the gender (M, F):")
            val gender = readln()
            p.gender = gender
            print("Enter the number:")
            val number = readln()
            p.number = number
            //---
            return p
        }
    }

    override fun title(): String  = "$name $surname"
    override fun searchInput(): String = "$name $surname $birthDate $number"

    /*
    // Example
    Name: John
    Surname: Smith
    Birth date: [no data]
    Gender: [no data]
    Number: (123) 234 345-456
    Time created: 2018-01-01T00:00
    Time last edit: 2018-01-01T00:01
     */
    override fun toString(): String =
        """Name: $name
Surname: $surname
Birth date: $birthDate
Gender: $gender
Number: $number
Time created: ${timeCreated.format(formatter)}
Time last edit: ${timeLastEdit.format(formatter)}""".trimIndent()

}

//val emptyPerson = Person()

data class Organization(
    var organizationName: String,
    var address: String,
    override var _number: String,
    override var timeCreated: LocalDateTime = LocalDateTime.now(),
    override var timeLastEdit: LocalDateTime = LocalDateTime.now()
) : Contact(_number, timeCreated, timeLastEdit) {

    // Secondary Empty constructor with no parameters
    constructor() : this(
        organizationName = "",
        address = "",
        _number = "",
        timeCreated = LocalDateTime.now(),
        timeLastEdit = LocalDateTime.now()
    )

    companion object {
        fun enterOrganization(): Organization {
            val o = Organization()
            print("Enter the organization name:")
            val name = readln()
            o.organizationName = name
            print("Enter the address:")
            val address = readln()
            o.address = address
            print("Enter the number:")
            val number = readln()
            o.number = number
            //---
            return o
        }
    }

    override fun title(): String  = "$organizationName"
    override fun searchInput(): String = "$organizationName $address $number"

    /*
    // Example
    Organization name: Pizza shop
    Address: Wall St. 1
    Number: +0 (123) 456-789-9999
    Time created: 2018-01-01T00:00
    Time last edit: 2018-01-01T00:00
     */
    override fun toString(): String =
        """Organization name: $organizationName
Address: $address
Number: $number
Time created: ${timeCreated.format(formatter)}
Time last edit: ${timeLastEdit.format(formatter)}""".trimIndent()
}

//val emptyOrganization = Organization()


class Contacts {
    private var contacts: MutableList<Contact> = mutableListOf()

    fun count(): Int = contacts.size
    fun isEmpty(): Boolean = contacts.size == 0
    fun add(contact: Contact) {
        contacts.add(contact)
        println("The record added.")
    }

    fun toJSON() : String {
        val listType = Types.newParameterizedType(List::class.java, Contact::class.java)
        val adapter = moshi.adapter<List<Contact>>(listType)
        val json = adapter.toJson(contacts)
        return json
    }

    fun fromJSON(json : String) {
        val listType = Types.newParameterizedType(List::class.java, Contact::class.java)
        val adapter = moshi.adapter<List<Contact>>(listType)
        val deserializedList = adapter.fromJson(json)
//        println("\nDeserialized List:")
//        deserializedList?.forEach {
//            when (it) {
//                is Person -> println("Person: ${it.name} ${it.surname}")
//                is Organization -> println("Organization: ${it.organizationName}")
//            }
//        }
        //add list to list
        if (deserializedList != null) {
            contacts.clear()
            contacts.addAll(deserializedList)
        }
    }

    fun printList() {
        for (i in contacts.indices) {
            println("${i + 1}. ${contacts[i].title()}") //polymorphism solution
            /*
            if (contacts[i] is Person) {
                val person = contacts[i] as Person
                println("${i + 1}. ${person.name} ${person.surname}")
            }
            if (contacts[i] is Organization) {
                val organization = contacts[i] as Organization
                println("${i + 1}. ${organization.organizationName}")
            }
             */
        }
    }

    private var searchIndex : Array<Int> = emptyArray()

    fun printDetail(index : Int) {
        println(contacts[index])
    }

    fun getFoundIndex(index : Int)  :Int{
        return searchIndex[index]
    }

    fun search(text : String) : Int{
        searchIndex = emptyArray()
        for (i in contacts.indices) {
            if (contacts[i].searchInput().contains(text, true)) {
                searchIndex += i
                println("${searchIndex.size}. ${contacts[i].title()}") //polymorphism solution
            }
        }
        return searchIndex.size
    }

    fun info() {
        printList()
        print("Enter index to show info:")
        val index = readln().toInt()
        println("${contacts[index - 1]}")
    }

    fun remove(index: Int) {
        contacts.removeAt(index)
        println("The record removed!")
    }

    fun edit(index: Int) {

        if (contacts[index] is Person) {
            val person = contacts[index] as Person
            println("Select a field (name, surname, birth, gender, number):")
            val field = readln()
            println("Enter $field:")
            val value = readln()
            when (field) {
                "name" -> person.name = value;
                "surname" -> person.surname = value;
                "birth" -> person.birthDate = value;
                "gender" -> person.gender = value;
                "number" -> person.number = value;
            }
        }
        if (contacts[index] is Organization) {
            val organization = contacts[index] as Organization
            println("Select a field (address, number):")
            val field = readln()
            println("Enter $field:")
            val value = readln()
            when (field) {
                "address" -> organization.address = value;
                "number" -> organization.number = value;
            }
        }

        contacts[index].timeLastEdit = LocalDateTime.now()
        println("The record updated!")
    }

}


fun main(args: Array<String>) {

    //testRegex()

    println("---------------------------- BEGIN MAIN -------------------------------------")


    allContacts = Contacts()

    // Print DB Info
    if (args.size > 0)  DBFile = args[0]

    loadFromFile(DBFile)


    do {
        viewMainMenu()
    } while (true)
}