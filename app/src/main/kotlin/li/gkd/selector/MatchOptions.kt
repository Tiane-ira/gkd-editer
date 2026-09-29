package li.gkd.selector




public data class MatchOptions(
    val fastQuery: Boolean = false,
) {
    public companion object {
        public val default: MatchOptions = MatchOptions()
    }
}
