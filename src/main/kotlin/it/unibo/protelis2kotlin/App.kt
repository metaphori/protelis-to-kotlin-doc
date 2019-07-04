/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package it.unibo.protelis2kotlin
import java.io.File
import kotlin.text.RegexOption.MULTILINE
import kotlin.text.RegexOption.DOT_MATCHES_ALL
import java.io.File.separator as SEP
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.IllegalStateException

var context = Context(setOf())

val protelisFileExt = "pt"

object Log {
    var debug = true

    fun log(msg: String) {
        if (debug) {
            println(msg)
        }
    }

    fun printInfo(msg: String) {
        println(msg)
    }

    fun logException(e: Exception) {
        val errorStr = StringWriter()
        e.printStackTrace(PrintWriter(errorStr))
        log(errorStr.toString())
    }
}

/**
 * Data class containing information that should be collected during parsing.
 */
data class Context(var protelisTypes: Set<String>)

/**
 * Utility function to extend contextual info with a Protelis type
 */
fun registerProtelisType(pt: String) {
    context = context.copy(context.protelisTypes + pt)
}

/**
 * Interface for a "piece of documentation"
 */
interface DocPiece {
    companion object {
        val docParamRegex = """@param\s+(\w+)\s*([^\n]*)""".toRegex()
        val docReturnRegex = """@return\s+([^\n]*)""".toRegex()
        val docOtherDirectiveRegex = """@(\w+)\s+([^\n]*)""".toRegex()
    }

    fun extendWith(txt: String): DocPiece
}

/**
 * Data class for a piece of documentation text (like this very comment)
 */
data class DocText(val text: String) : DocPiece {
    override fun extendWith(txt: String): DocPiece {
        return DocText(text + txt)
    }
}

/**
 * Data class for a piece of documentation describing a function parameter
 */
data class DocParam(
    val paramName: String,
    val paramType: String,
    val paramDescription: String
) : DocPiece {
    override fun extendWith(txt: String): DocPiece {
        return DocParam(paramName, paramType, paramDescription + txt)
    }
}

/**
 * Data class for a piece of documentation describing a function's return value/type
 */
data class DocReturn(
    val returnType: String,
    val returnDescription: String
) : DocPiece {
    override fun extendWith(txt: String): DocPiece {
        return DocReturn(returnType, returnDescription + txt)
    }
}

/**
 * Data class for a generic documentation directive `@<directive> description`
 */
data class DocDirective(
    val directive: String,
    val description: String
) : DocPiece {
    override fun extendWith(txt: String): DocPiece {
        return DocDirective(directive, description + txt)
    }
}

/**
 * Data class describing a Protelis function parameter (name and type)
 */
data class ProtelisFunArg(val name: String, val type: String)

/**
 * Data class describing a Protelis function: name, parameters, return type, visibility, and type parameters (generics)
 */
data class ProtelisFun(
    val name: String,
    val params: List<ProtelisFunArg> = listOf(),
    val returnType: String = "",
    val public: Boolean = false,
    val genericTypes: Set<String> = setOf()
)

/**
 * Data class containing the various documentation pieces for a Protelis function
 */
data class ProtelisFunDoc(val docPieces: List<DocPiece>)

/**
 * Data class pairing a Protelis function with its documentation
 */
data class ProtelisItem(val function: ProtelisFun, val docs: ProtelisFunDoc)

/**
 * Parses a type and returns both the parsed type and the remaining text
 * @param line The text line to be parsed
 */
fun parseTypeAndRest(line: String): Pair<String, String> {
    // Works by finding the first comma which is not contained within parentheses
    var stillType = true
    var k = 0
    var parentheses = ""
    var type = line.takeWhile { c ->
        k++
        val cond = (c != ',' || stillType) && !(c == ',' && k>0 && parentheses.isEmpty())
        if (stillType && (c == '(' || c == '[')) parentheses += c
        if (stillType && (c == ')' || c == ']')) {
            parentheses = parentheses.dropLast(1)
            if (parentheses.isEmpty()) stillType = false
        }
        cond
    }
    return Pair(type, line.substring(k).trim())
}

/**
 * Parses the documentation of a Protelis function
 * @param doc The documentation string to be parsed
 * @return [ProtelisFunDoc]
 */
fun parseDoc(doc: String): ProtelisFunDoc {
    var txt = ""
    val pieces: MutableList<DocPiece> = mutableListOf()
    doc.lines().map { """\s*\*\s*""".trimMargin().toRegex().replace(it, "").trim() }.forEach { l ->
        if (!l.startsWith("@")) {
            val partialtxt = l
            if (pieces.isEmpty()) txt += if (txt.isEmpty()) partialtxt else "\n $partialtxt"
            else {
                val last = pieces.last()
                pieces.remove(last)
                pieces.add(last.extendWith(" " + partialtxt))
            }
        } else {
            DocPiece.docParamRegex.matchEntire(l)?.let { matchRes ->
                val gs = matchRes.groupValues
                val (type, desc) = parseTypeAndRest(gs[2])
                pieces.add(DocParam(gs[1], type, desc))
                return@forEach
            }

            DocPiece.docReturnRegex.matchEntire(l)?.let { matchRes ->
                val gs = matchRes.groupValues
                val (type, desc) = parseTypeAndRest(gs[1])
                pieces.add(DocReturn(type, desc))
                return@forEach
            }

            DocPiece.docOtherDirectiveRegex.matchEntire(l)?.let { matchRes ->
                val directive = matchRes.groupValues[1]
                val desc = matchRes.groupValues[2]
                pieces.add(DocDirective(directive, desc))
                return@forEach
            }
        }
    }
    if (!txt.isEmpty()) pieces.add(0, DocText(txt))

    return ProtelisFunDoc(pieces)
}

/**
 * Parses a Protelis function definition
 * @param fline The string of a Protelis function definition to be parsed
 * @return [ProtelisFun]
 */
fun parseProtelisFunction(fline: String): ProtelisFun {
    return ProtelisFun(
            name = """def\s+(\w+)""".toRegex().find(fline)?.groupValues?.get(1) ?: throw IllegalStateException("Cannot parse function name in: $fline"),
            params = """\(([^\)]*)\)""".toRegex().find(fline)?.groupValues?.get(1)?.split(",")
                    ?.filter { !it.isEmpty() }
                    ?.map {
                        // if (!"""\w""".toRegex().matches(it)) throw IllegalStateException("Bad argument name: $it")
                        ProtelisFunArg(it.trim(), "")
                    }?.toList() ?: throw IllegalStateException("Cannot parse arglist in: $fline"),
            public = """(public\s+def)""".toRegex().find(fline) != null)
}

/**
 * Parses Protelis source code into a list of [ProtelisItem]s
 * @param content The string of Protelis source code to be parsed
 */
fun parseFile(content: String): List<ProtelisItem> {
    val pitems = mutableListOf<ProtelisItem>()

    """^\s*(/\*\*(.*?)\*/)\n*([^\n]*)"""
            .toRegex(setOf(MULTILINE, DOT_MATCHES_ALL))
            .findAll(content)
            .forEach { matchRes ->
                val groups = matchRes.groupValues
                val doc = groups[2]
                val funLine = groups[3]
//                println("-----------------\nDoc: $doc")
//                parseDoc(doc).docPieces.forEach { p ->
//                    println("Doc piece: $p")
//                }
//                println("Function line: $funLine\n${parseProtelisFunction(funLine)}")
                var parsedDoc: ProtelisFunDoc
                var parsedFun: ProtelisFun

                try {
                    parsedDoc = parseDoc(doc)
                } catch (e: Exception) {
                    Log.log("Failed to parse doc\n\"\"\"${matchRes.value}\"\"\" [$doc]")
                    Log.logException(e)
                    return@forEach
                }

                // Easy check to control if we actually have a function
                if (!funLine.contains("def")) return@forEach

                try {
                    parsedFun = parseProtelisFunction(funLine)
                } catch (e: Exception) {
                    Log.log("Failed to parse function\n\"\"\"${matchRes.value}\"\"\" [$funLine]")
                    Log.logException(e)
                    return@forEach
                }

                pitems.add(ProtelisItem(parsedFun, parsedDoc))
            }
    return pitems
}

/**
 * Generates (Dokka ) Kotlin documentation from a [ProtelisFunDoc]
 * @param docs The [ProtelisFunDoc] object encapsulating the docs for a Protelis function
 */
fun generateKotlinDoc(docs: ProtelisFunDoc): String {
    val docPieces = docs.docPieces
    return "/**\n" +
            docPieces.map { p ->
                if (p is DocText) {
                    p.text.lines().map { "  * $it" }.joinToString("\n")
                } else if (p is DocParam) {
                    "  * @param ${p.paramName} ${p.paramDescription}"
                } else if (p is DocReturn) {
                    "  * @return ${p.returnDescription}"
                } else if (p is DocDirective) {
                    "  * @${p.directive} ${p.description}"
                } else ""
            }.joinToString("\n") + "\n  */"
}

/**
 * Generates a Kotlin type from a Protelis type
 */
fun generateKotlinType(protelisType: String): String = when (protelisType) {
    "" -> "Unit"
    "bool" -> "Boolean"
    "num" -> "Number"
    else ->
        """\(([^\)]*)\)\s*->\s*(.*)""".toRegex().matchEntire(protelisType)?.let { matchRes ->
            val args = matchRes.groupValues[1].split(",").map { generateKotlinType(it.trim()) }
            val ret = generateKotlinType(matchRes.groupValues[2])
            """(${args.joinToString(",")}) -> $ret"""
        } ?: """\[.*\]""".toRegex().matchEntire(protelisType)?.let { _ ->
            registerProtelisType("Tuple")
            "Tuple" // "List<${generateKotlinType()}>"
        } ?: if (protelisType.length == 1 && protelisType.any { it.isUpperCase() })
            protelisType
        else if ("""[A-Z]'""".toRegex().matches(protelisType))
            "${protelisType[0].inc()}"
        else if ("""\w+""".toRegex().matches(protelisType)) {
            registerProtelisType(protelisType)
            protelisType
        } else "Any"
}

/**
 * Symbols used frely in Protelis but that are not valid in Kotlin (e.g., as they are reserved words) are sanitized
 */
fun sanitizeNameForKotlin(name: String): String = when (name) {
    "null" -> "`null`"
    else -> name
}

/**
 * Generates a Kotlin function from a Protelis function descriptor
 */
fun generateKotlinFun(fn: ProtelisFun): String {
    var genTypesStr = fn.genericTypes.joinToString(",")
    if (!genTypesStr.isEmpty()) genTypesStr = " <$genTypesStr>"

    return "fun$genTypesStr ${sanitizeNameForKotlin(fn.name)}(" +
            fn.params.map { "${sanitizeNameForKotlin(it.name)}: ${generateKotlinType(it.type)}" }.joinToString(", ") +
            "): ${generateKotlinType(fn.returnType)} = TODO()"
}

/**
 * Generates a Kotlin item (doc + fun signature) from a Protelis item (doc + fun)
 */
fun generateKotlinItem(pitem: ProtelisItem): String {
    val doc = pitem.docs
    var fn = pitem.function
    return generateKotlinDoc(doc) + "\n" + generateKotlinFun(fn)
}

/**
 * Generates a string from a list of Protelis items (function and docs pairs)
 */
fun generateKotlin(protelisItems: List<ProtelisItem>): String {
    // Retrieve type info from docs
    val pitems = protelisItems.map { pitem ->
        val doc = pitem.docs
        var fn = pitem.function
        pitem.copy(function = fn.copy(
                returnType = doc.docPieces.filter { it is DocReturn }.map { (it as DocReturn).returnType }.firstOrNull() ?: "",
                params = fn.params.map { param ->
                    param.copy(type = doc.docPieces.filter { it is DocParam && it.paramName == param.name }
                            .map { (it as DocParam).paramType }.firstOrNull() ?: "Any") },
                genericTypes = doc.docPieces.map {
                    if (!(it is DocParam)) "" else it.paramType
                }.flatMap { """([A-Z]'?)""".toRegex().findAll(it).map {
                    if (it.value.length == 2 && it.value[1] == '\'') "${it.value[0].inc()}"
                    else it.value
                }.toList() }.toSet()
        ))
    }

    return pitems.map { generateKotlinItem(it) }.joinToString("\n\n")
}

/**
 * Main function: reads all Protelis files under a base directory, parses them, and generates corresponding Kotlin files in a destination directory.
 *
 * This is to be called with two arguments:
 * 1) The base directory from which recursively looking for Protelis files
 * 2) The destination directory that will contain the output Kotlin files
 */
fun main(args: Array<String>) {
    if (args.size < 2) {
        println("USAGE: program <dir> <destDir> <debug>")
        return
    }

    val header = "[Protelis2Kotlin]"

    val dir = args[0]
    val destDir = args[1]
    Log.debug = if (args.size == 3) args[2] == "1" else false

    Log.log("$header Base directory: $dir\n$header Destination directory: $destDir")

    var k = 0

    File(dir).walkTopDown().forEach { file ->
        if (!file.isFile || file.extension != protelisFileExt) return@forEach

        val fileText: String = file.readText()

        Log.log("Processing " + file.absolutePath)

        val pkg = """module (.+)""".toRegex().find(fileText)?.groupValues?.component2() ?: ""
        if (pkg.isEmpty()) {
            Log.log("\tCannot parse Protelis package. Skipping.")
            return@forEach
        }

        val pkgParts = pkg.split(':')
        Log.log("\tPackage: " + pkg)

        // RESET CONTEXT
        context = Context(setOf())

        var protelisItems: List<ProtelisItem>

        try {
            protelisItems = parseFile(fileText)
        } catch (e: Exception) {
            Log.log("Failed to parse $file")
            Log.logException(e)
            return@forEach
        }

        Log.log("\tFound " + protelisItems.size + " Protelis items.")

        val pkgCode = "package ${pkgParts.joinToString(".")}\n\n"
        val kotlinCode = generateKotlin(protelisItems)

        Log.log("\tContext: " + context)

        val importCode = context.protelisTypes.map { when (it) {
            "ExecutionContext", "ExecutionEnvironment" -> "org.protelis.vm.$it"
            "Tuple" -> "org.protelis.lang.datatype.$it"
            else -> ""
        } }.filterNot { it.isEmpty() }.map { "import " + it }.joinToString("\n") + "\n\n"

        val kotlinFullCode = pkgCode + importCode + kotlinCode

        val outPath = "$destDir$SEP${pkgParts.joinToString(SEP)}$SEP${file.name.replace(".pt",".kt")}"

        Log.log("\tWriting " + outPath)

        File(outPath).let {
            it.parentFile.mkdirs()
            it.createNewFile()
            it
        }.writeText(kotlinFullCode).let { k++ }
    }

    Log.log("$header Converted $k .pt files to Kotlin")
}
