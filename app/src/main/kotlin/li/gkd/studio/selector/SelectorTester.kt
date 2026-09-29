package li.gkd.studio.selector

import li.gkd.selector.Selector
import li.gkd.selector.SelectorCompileResult
import li.gkd.studio.data.model.SnapshotNode

sealed class SelectorTestResult {
    data class Success(val matchingNodes: List<SnapshotNode>) : SelectorTestResult()
    data class Error(val message: String) : SelectorTestResult()
}

object SelectorTester {
    fun test(selectorStr: String, allNodes: List<SnapshotNode>): SelectorTestResult {
        if (selectorStr.isBlank()) {
            return SelectorTestResult.Error("选择器不能为空")
        }
        val compileResult = Selector.compile(selectorStr)
        if (compileResult !is SelectorCompileResult.Success) {
            val err = (compileResult as? SelectorCompileResult.Failure)?.error?.message ?: "选择器语法错误"
            return SelectorTestResult.Error(err)
        }
        val selector = compileResult.value

        val matched = mutableListOf<SnapshotNode>()
        for (node in allNodes) {
            val res = selector.match(node, SnapshotNodeAdapter)
            if (res != null) {
                matched.add(res)
            }
        }
        val distinctMatched = matched.distinctBy { it.id }
        return SelectorTestResult.Success(distinctMatched)
    }
}
