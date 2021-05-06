fun foo(s: String?) {
    if (s != null) s.length // Compiler automatically casts 's' to 'String'
}