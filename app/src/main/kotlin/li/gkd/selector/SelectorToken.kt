package li.gkd.selector




public enum class SelectorTokenKind {
    Whitespace,
    Selector,
    Identifier,
    Keyword,
    Integer,
    String,
    Invalid,
    CompareOperator,
    LogicalOperator,
    RelationOperator,
    ArithmeticOperator,
    PolynomialVariable,
    Punctuation,
    Target,
    Wildcard,
}


public enum class SelectorTokenScope {
    Selector,
    Property,
    Relation,
}


/**
 * A tolerant lexical token covering `start` (inclusive) to `end` (exclusive).
 * [scope] provides the coarse syntax context needed by context-sensitive highlighters.
 */
public data class SelectorToken(
    val kind: SelectorTokenKind,
    val scope: SelectorTokenScope,
    val start: Int,
    val end: Int,
)
