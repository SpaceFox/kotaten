package fr.spacefox.kotaten

import com.beust.jcommander.*
import com.beust.jcommander.validators.PositiveInteger
import com.google.common.collect.EvictingQueue
import java.text.DecimalFormat
import java.text.MessageFormat
import java.util.*
import java.util.ResourceBundle

private const val NS_PER_SECOND = 1_000_000_000L
private const val PRECISION_DEFAULT = 0
private const val PRECISION_MIN = 0
private const val PRECISION_MAX = 0
private const val RESET_TIMER_DEFAULT = 5L
private const val SAMPLE_SIZE_DEFAULT = 5

private fun Double.toBpm(): Double {
    return 60.0 * 1_000_000_000 / this
}

private operator fun ResourceBundle.get(key: String): String {
    return this.getString(key)
}

@Parameters(resourceBundle = "messages")
class Settings {
    @Parameter(names = ["-h", "--help"], descriptionKey = "options.help", help = true)
    var help = false

    @Parameter(
            names = ["-p", "--precision"],
            descriptionKey = "options.precision",
            validateWith = [PrecisionValidator::class])
    var precision = PRECISION_DEFAULT

    @Parameter(
            names = ["-r", "--reset-time"],
            descriptionKey = "options.resettime",
            validateWith = [PositiveInteger::class])
    var resetTime = RESET_TIMER_DEFAULT

    @Parameter(
            names = ["-s", "--sample-size"],
            descriptionKey = "options.samplesize",
            validateWith = [PositiveInteger::class])
    var sampleSize = SAMPLE_SIZE_DEFAULT

    @Parameter(names = ["-v", "--version"], descriptionKey = "options.version")
    var version = false

    class PrecisionValidator: IParameterValidator {
        override fun validate(name: String?, value: String?) {
            val precision = Integer.parseInt(value)
            if (precision < PRECISION_MIN || precision > PRECISION_MAX) {
                throw ParameterException(MessageFormat.format(
                        ResourceBundle.getBundle("messages")["options.precision.outofscope"],
                        PRECISION_MIN,
                        PRECISION_MAX,
                        precision))
            }
        }
    }
}

class Kotaten(settings: Settings) {

    private val input = Scanner(System.`in`)
    private val samples: EvictingQueue<Long> = EvictingQueue.create<Long>(settings.sampleSize)
    private val messages = ResourceBundle.getBundle("messages")
    private val formatter = DecimalFormat()
    private val resetTime = settings.resetTime * NS_PER_SECOND

    init {
        input.useDelimiter("")
        formatter.minimumFractionDigits = settings.precision
        formatter.maximumFractionDigits = settings.precision
    }

    fun run() {
        println(messages["instructions"])
        var key: Char
        var start: Long? = null
        var end: Long
        while (input.hasNext()) {
            key = input.next()[0]
            if (key == 'q')
                break

            end = System.nanoTime()
            if (start == null || (end - start) > resetTime) {
                samples.clear()
                println(messages["typeagain"])
            } else {
                samples.add(end - start)
            }
            start = end

            if (samples.size > 0)
                print(MessageFormat.format(
                        messages["tempo"],
                        formatter.format(samples.average().toBpm())))
        }
        println(messages.getString("bye"))
    }
}

fun main(args: Array<String>) {
    val settings = Settings()
    val jc = JCommander.newBuilder()
            .addObject(settings)
            .build()
    jc.parse(*args)

    if (settings.help) {
        jc.usage()
        return
    }
    if (settings.version) {
        println(ResourceBundle.getBundle("config")["version"])
        return
    }
    Kotaten(settings).run()
}
