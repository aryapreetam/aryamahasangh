import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class GenerateKto : DefaultTask() {

  private val apolloGeneratedDir = File(project.rootDir, "composeApp/build/generated/source/apollo/service")
  private val outputDir = File(project.rootDir, "composeApp/build/generated/source/apolloCamelCase")

  init {
    group = "codegen"
    description = "Generates camelCased DTOs from Apollo-generated GraphQL Node classes"
  }

  @TaskAction
  fun generate() {
    if (!apolloGeneratedDir.exists()) {
      println("Apollo generated directory does not exist: $apolloGeneratedDir")
      return
    }

    outputDir.mkdirs()

    apolloGeneratedDir.walkTopDown()
      .filter { it.isFile && it.extension == "kt" && it.readText().contains("data class Node") }
      .forEach { file -> generateDtoForFile(file) }
  }

  private fun generateDtoForFile(file: File) {
    val lines = file.readLines()
    val classNameRegex = Regex("class (\\w+)Collection")
    val propertyRegex = Regex("val (\\w+): ([^,]+)")

    val fileText = file.readText()
    val originalClassName = classNameRegex.find(fileText)?.groupValues?.get(1) ?: return
    val dtoClassName = originalClassName.removeSuffix("s").replaceFirstChar { it.uppercase() }

    val properties = lines
      .dropWhile { !it.contains("data class Node") }
      .drop(1)
      .takeWhile { !it.trim().startsWith(")") }
      .mapNotNull { propertyRegex.find(it)?.groupValues }
      .map { it[1] to it[2] }

    val fileSpec = FileSpec.builder("generated.dto", dtoClassName)
    val typeSpec = TypeSpec.classBuilder(dtoClassName)
      .addModifiers(KModifier.DATA)
      .primaryConstructor(FunSpec.constructorBuilder().apply {
        properties.forEach { (name, type) ->
          val camel = name.toCamelCase()
          addParameter(camel, resolveKotlinType(type))
        }
      }.build())
      .apply {
        properties.forEach { (name, type) ->
          val camel = name.toCamelCase()
          addProperty(
            PropertySpec.builder(camel, resolveKotlinType(type))
              .initializer(camel)
              .build()
          )
        }
      }
      .build()

    fileSpec.addType(typeSpec).build().writeTo(outputDir)
    println("✅ Generated: $dtoClassName.kt")
  }

  private fun String.toCamelCase(): String =
    split("_").joinToString("") { it.lowercase().replaceFirstChar { c -> c.uppercase() } }
      .replaceFirstChar { it.lowercase() }

  private fun resolveKotlinType(typeStr: String): TypeName {
    val trimmed = typeStr.trim()
    val isNullable = trimmed.endsWith("?")
    val rawType = trimmed.removeSuffix("?")

    val typeName = when {
      rawType.startsWith("List<") -> {
        val innerRaw = rawType.removePrefix("List<").removeSuffix(">")
        val innerType = resolveKotlinType(innerRaw)
        LIST.parameterizedBy(innerType)
      }

      rawType == "String" -> STRING
      rawType == "Int" -> INT
      rawType == "Long" -> LONG
      rawType == "Double" -> DOUBLE
      rawType == "Float" -> FLOAT
      rawType == "Boolean" -> BOOLEAN
      rawType == "Any" -> ANY
      else -> ClassName("", rawType)
    }

    return if (isNullable) typeName.copy(nullable = true) else typeName
  }

  companion object {
    private val STRING = ClassName("kotlin", "String")
    private val INT = ClassName("kotlin", "Int")
    private val LONG = ClassName("kotlin", "Long")
    private val DOUBLE = ClassName("kotlin", "Double")
    private val FLOAT = ClassName("kotlin", "Float")
    private val BOOLEAN = ClassName("kotlin", "Boolean")
    private val ANY = ClassName("kotlin", "Any")
    private val LIST = ClassName("kotlin.collections", "List")
  }
}
