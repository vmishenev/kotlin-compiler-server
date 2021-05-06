class ImmutablePropertyBag(map: Map<String, Any>) {
    private val mapCopy = map.toMap()
}