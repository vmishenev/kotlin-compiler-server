fun String.countDistinctCharacters() = toLowerCase().toList().distinct().count()

fun main() {
    // Read the input value.
    println("Hello, enter your name:")
    val name = readLine()
    // Count the letters in the name.
    name?.replace(" ", "")?.let {
        println("Your name contains ${it.length} letters")
        // Print the number of unique letters.
        println("Your name contains ${it.countDistinctCharacters()} unique letters")
    } ?: error("Error while reading input from the terminal: the value can't be null.")
}