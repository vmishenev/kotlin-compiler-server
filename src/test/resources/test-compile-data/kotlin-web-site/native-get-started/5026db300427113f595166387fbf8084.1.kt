fun main() {
    // Read the input value.
    println("Hello, enter your name:")
    val name = readLine()
    // Count the letters in the name.
    name?.replace(" ", "")?.let {
        println("Your name contains ${it.length} letters")
    }
}