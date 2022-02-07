import java.io.File
import java.io.FileInputStream
import java.util.Properties

class BuildProperties constructor(
    private val propertiesFile: File,
    private val defaultPropertiesFile: File
) {
    private val properties by lazy {
        initProperties(propertiesFile)
    }

    private val defaultProperties by lazy {
        initProperties(defaultPropertiesFile)
    }

    private fun initProperties(propertiesFile: File) =
        Properties().apply {
            if (propertiesFile.exists()) {
                load(FileInputStream(propertiesFile))
            }
        }

    val secrets = HashMap<String, Any>().apply {
        defaultProperties.forEach {
            this[it.key.toString()] = it.value
        }
        properties.forEach {
            this[it.key.toString()] = it.value
        }
    }
}