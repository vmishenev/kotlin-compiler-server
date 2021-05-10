var counter = 0 // the initializer assigns the backing field directly
    set(value) {
        if (value >= 0) field = value
    }