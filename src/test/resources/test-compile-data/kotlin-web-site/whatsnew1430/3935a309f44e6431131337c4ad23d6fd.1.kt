@JvmInline
value class Negative(val x: Int) {
  init {
      require(x < 0) { }
  }
}