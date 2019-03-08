import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Statement
import java.sql.PreparedStatement
import java.sql.SQLException
import java.util.Date
import java.text.SimpleDateFormat
import java.nio.file.Paths
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxOptions
import org.openqa.selenium.ie.InternetExplorerDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.WebDriverWait
import org.apache.commons.lang3.StringUtils
import ca.szc.configparser.Ini

class MyArgs(parser: ArgParser) {
    val isHideBrowser by parser.flagging(
        "--hide-browser",
        help = "Run browser in headless mode"
    )

    val isDebug by parser.flagging(
        "--debug",
        help = "Run in debugging mode. Not clock-in/out to Kinnosuke."
    )

    val isWithoutClock by parser.flagging(
        "--without-clock",
        help = "Record only DB mode. Record the clock in/out time to DB, but not clock in/out on Kinnosuke."
    )

    val configFilePath by parser.storing(
        "--config",
        help = "Path to the *.ini (default: TimeRecorder.ini in the current directory)"
    ).default("TimeRecorder.ini")

    val sqliteDbPath by parser.storing(
        "--sqlite",
        help= "Path to the sqlite3 DB file (default: TimeRecorder.sqlite3 in the current directory)"
    ).default("TimeRecorder.sqlite3")

    val isCheck by parser.flagging(
        "--check",
        help = "Check whether today has been clocked in/out. If clocked in/out then output 'CLOCKED' otherwise output nothing."
    )

    val recordType by parser.positional(
        "RECORD_TYPE",
        help ="Set IN or OUT as the record type (IN: Clock-in, OUT: Clock-out)"
    )
}

enum class ClockType {
    IN, OUT
}

class TimeRecordDbManager(sqliteFilePath: String, val isDoRecord: Boolean = true) {
    private val conn: Connection = DriverManager.getConnection("jdbc:sqlite:${sqliteFilePath}")

    init {
        conn.autoCommit = false

        val ddlTimeRecord = """
            create table if not exists time_record (
                id          integer     not null primary key autoincrement,
                date_key    text        not null,
                clock_type  text        not null,
                created_at  timestamp   not null default current_timestamp
            )
        """
        val ddlHolidays  = """
            create table if not exists holidays (
                id          integer     not null primary key autoincrement,
                date_key    text        not null,
                yyyymm      text        not null,
                created_at  timestamp   not null default current_timestamp
            )
        """
        val statement: Statement = conn.createStatement()
        statement.executeUpdate(ddlTimeRecord)
        statement.executeUpdate(ddlHolidays)
    }

    fun close() {
        conn.close()
    }

    fun hasInitializedThisMonthHolidays(): Boolean {
        val sql = "select count(1) as has_initialized from holidays where yyyymm = ?"
        val yyyymm = getDateKey().substring(0..5)
        val statement: PreparedStatement = conn.prepareStatement(sql)
        statement.setString(1, yyyymm)
        val rs: ResultSet = statement.executeQuery()
        rs.next()
        return rs.getInt("has_initialized") > 0
    }

    fun addHolidays(holidayDateKeys: List<String>) {
        val sql = "insert into holidays (date_key, yyyymm) values (?, ?)"
        try {
            val statement: PreparedStatement = conn.prepareStatement(sql)
            for (dateKey in holidayDateKeys) {
                statement.clearParameters()
                statement.setString(1, dateKey)
                statement.setString(2, dateKey.substring(0..5))
                statement.executeUpdate()
            }
            conn.commit()
        } catch (e: SQLException) {
            conn.rollback()
            throw e
        }

    }

    fun isRecorded(clockType: ClockType): Boolean {
        val sql = "select count(1) as has_recorded from time_record where date_key = ? and clock_type = ?"
        val dateKey = getDateKey()
        val statement: PreparedStatement = conn.prepareStatement(sql)
        statement.setString(1, dateKey)
        statement.setString(2, clockType.name)
        val rs: ResultSet = statement.executeQuery()
        rs.next()
        return rs.getInt("has_recorded") > 0
    }

    fun record(clockType: ClockType) {
        if (isRecorded(clockType) or !isDoRecord) return

        val sql = "insert into time_record (date_key, clock_type) values (?, ?)"
        val dateKey = getDateKey()
        try {
            val statement: PreparedStatement = conn.prepareStatement(sql)
            statement.setString(1, dateKey)
            statement.setString(2, clockType.name)
            statement.executeUpdate()
            conn.commit()
        } catch (e: SQLException) {
            conn.rollback()
            throw e
        }
    }

    fun isHoliday(): Boolean {
        val sql = "select count(1) as is_holiday from holidays where date_key = ?"
        val dateKey = getDateKey()
        val statement: PreparedStatement = conn.prepareStatement(sql)
        statement.setString(1, dateKey)
        val rs: ResultSet = statement.executeQuery()
        rs.next()
        return rs.getInt("is_holiday") > 0
    }

    private fun getDateKey(targetDate: Date = Date()): String {
        return SimpleDateFormat("yyyyMMdd").format(targetDate)
    }
}

class KinnosukeAutomator(val dbManager: TimeRecordDbManager,
                         val id: String,
                         val password: String,
                         browser: String = "IE",
                         val executablePath: String = "",
                         val topPageUrl: String = "https://www.4628.jp/",
                         val isHideBrowser: Boolean = false,
                         val isDoClock: Boolean = true) {

    private val waitSeconds:Long = 10
    private val timetableUrl = topPageUrl + "?module=timesheet&action=browse"
    private val wait: WebDriverWait

    private val driver: WebDriver = when(browser.toUpperCase()) {
        "FIREFOX" -> {
            fun getFirefoxDriver(): FirefoxDriver {
                if (executablePath == "") {
                    System.setProperty("webdriver.gecko.driver", executablePath)
                }
                return when (isHideBrowser) {
                    true -> {
                        val options = FirefoxOptions()
                        options.addArguments("--headless")
                        FirefoxDriver(options)
                    }
                    false -> FirefoxDriver()
                }
            }
            getFirefoxDriver()
        }
        "CHROME" -> {
            fun getChromeDriver(): ChromeDriver {
                if (executablePath == "") {
                    System.setProperty("webdriver.chrome.driver", executablePath)
                }
                return when (isHideBrowser) {
                    true -> {
                        val options = ChromeOptions()
                        options.addArguments("--headless")
                        ChromeDriver(options)
                    }
                    false -> ChromeDriver()
                }
            }
            getChromeDriver()
        }
        else -> { // Maybe IE
            if (executablePath == "") {
                    System.setProperty("webdriver.ie.driver", executablePath)
            }
            // IE cannot run in headless mode
            InternetExplorerDriver()
        }
    }

    init {
        wait = WebDriverWait(driver, waitSeconds)

        // topPageにアクセスして、ログインボタンが表示されるまで待機
        driver.get(topPageUrl)
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("id_passlogin")))

        // ID, パスワードを入力してログインボタンクリック
        val idBox = driver.findElement(By.id("y_logincd"))
        idBox.sendKeys(id)
        val passwordBox = driver.findElement(By.id("password"))
        passwordBox.sendKeys(password)
        val loginButton = driver.findElement(By.id("id_passlogin"))
        loginButton.click()

        if (!dbManager.hasInitializedThisMonthHolidays()) {
            dbManager.addHolidays(getThisMonthHolidays())
        }
    }

    fun getThisMonthHolidays(): List<String> {
        // Access to the TimeTable URL and wait until the footer is shown.
        driver.get(timetableUrl)
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("footer")))

        val id_prefix = "fix_0_"
        val yyyymm = SimpleDateFormat("yyyyMM").format(Date())
        val dateKeys = ArrayList<String>()
        val xpath = By.xpath("//tr[starts-with(@id, \"${id_prefix}\")]")
        for (tr in driver.findElements(xpath)) {
            // tr with 'bgcolor_white' class is a business day row
            if (tr.getAttribute("class") != "bgcolor_white") {
                val id = tr.getAttribute("id")
                val dateStr =  StringUtils.leftPad(id.replace(id_prefix, ""), 2, "0")
                dateKeys.add(yyyymm + dateStr)
            }
        }
        return dateKeys
    }

    fun clock(clockType: ClockType) {
        val xpath = when(clockType) {
            ClockType.IN -> By.xpath("//*[@id=\"timerecorder_txt\" and (starts-with(text(), \"出社\") or starts-with(text(), \"In\"))]")
            ClockType.OUT -> By.xpath("//*[@id=\"timerecorder_txt\" and (starts-with(text(), \"退社\") or starts-with(text(), \"Out\"))]")
        }

        // Access to the top page and wait until the footer is shown
        driver.get(topPageUrl)
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("footer")))

        // Exit if already clocked
        if (driver.findElements(xpath).isNotEmpty()) {
            dbManager.record(clockType)
            return
        }

        val buttonLables = when(clockType) {
            ClockType.IN -> listOf("出社", "In")
            ClockType.OUT -> listOf("退社", "Out")
        }
        for (button in driver.findElements(By.name("_stampButton"))) {
            if (isDoClock and (button.text in buttonLables)) {
                button.click()
                wait.until(ExpectedConditions.presenceOfElementLocated(By.id("footer")))
                wait.until(ExpectedConditions.presenceOfElementLocated(xpath))
                dbManager.record(clockType)
            }
        }
    }

    fun close() {
        driver.quit()
    }
}

fun main(args: Array<String>) = mainBody {

    // Parse command line arguments
    val argParser = ArgParser(args).parseInto(::MyArgs)

    // Load ini file
    val ini = Ini().read(Paths.get(argParser.configFilePath))
    val id = ini.getValue("Kinnosuke", "ID")
    val password = ini.getValue("Kinnosuke", "PASSWORD")
    val url = ini.getValue("Kinnosuke", "URL")
    val browser = ini.getValue("Selenium", "BROWSER")
    val executablePath = ini.getValue("Selenium", "DRIVER_PATH")

    val mgr = TimeRecordDbManager(argParser.sqliteDbPath, argParser.isDebug)

    val clockType = ClockType.valueOf(argParser.recordType)

    if (argParser.isCheck) {
        if (mgr.isHoliday()) {
            print("HOLIDAY")
        } else if(mgr.isRecorded(clockType)) {
            print("CLOCKED")
        }
    } else if (argParser.isWithoutClock) {
        mgr.record(clockType)
    } else {

        val automator = KinnosukeAutomator(
            mgr,
            id, password,
            browser = browser, executablePath = executablePath,
            topPageUrl = url,
            isHideBrowser = argParser.isHideBrowser,
            isDoClock = argParser.isDebug
        )
        automator.clock(clockType)
        automator.close()
    }
    mgr.close()
}