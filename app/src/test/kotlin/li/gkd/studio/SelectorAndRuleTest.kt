package li.gkd.studio

import li.gkd.studio.data.model.SnapshotAttr
import li.gkd.studio.data.model.SnapshotNode
import li.gkd.studio.rule.OutputScope
import li.gkd.studio.rule.RuleCategory
import li.gkd.studio.rule.RuleGenerator
import li.gkd.studio.rule.RuleOptions
import li.gkd.studio.selector.SelectorGenerator
import li.gkd.studio.selector.SelectorTestResult
import li.gkd.studio.selector.SelectorTester
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectorAndRuleTest {

    private fun createSampleTree(): List<SnapshotNode> {
        val root = SnapshotNode(
            id = 0,
            pid = -1,
            attr = SnapshotAttr(
                name = "android.widget.FrameLayout",
                left = 0, top = 0, right = 1080, bottom = 2400
            )
        )

        val linearLayout = SnapshotNode(
            id = 1,
            pid = 0,
            idQf = true,
            attr = SnapshotAttr(
                name = "android.widget.LinearLayout",
                id = "com.taobao.taobao:id/container",
                vid = "container",
                left = 0, top = 100, right = 1080, bottom = 2300
            )
        )

        val skipBtn = SnapshotNode(
            id = 2,
            pid = 1,
            idQf = true,
            textQf = true,
            attr = SnapshotAttr(
                name = "android.widget.TextView",
                id = "com.taobao.taobao:id/btn_skip",
                vid = "btn_skip",
                text = "跳过 5s",
                clickable = true,
                left = 800, top = 120, right = 1000, bottom = 220
            )
        )

        val titleTv = SnapshotNode(
            id = 3,
            pid = 1,
            textQf = true,
            attr = SnapshotAttr(
                name = "android.widget.TextView",
                text = "广告标题",
                left = 100, top = 500, right = 900, bottom = 600
            )
        )

        val closeBtn = SnapshotNode(
            id = 4,
            pid = 1,
            attr = SnapshotAttr(
                name = "android.widget.ImageView",
                desc = "关闭",
                clickable = true,
                left = 950, top = 120, right = 1050, bottom = 220
            )
        )

        root.children = listOf(linearLayout)
        linearLayout.parent = root
        linearLayout.children = listOf(skipBtn, titleTv, closeBtn)
        skipBtn.parent = linearLayout
        titleTv.parent = linearLayout
        closeBtn.parent = linearLayout

        return listOf(root, linearLayout, skipBtn, titleTv, closeBtn)
    }

    @Test
    fun testSelectorGeneratorProducesCandidates() {
        val tree = createSampleTree()
        val skipNode = tree[2]

        val candidates = SelectorGenerator.generateCandidates(skipNode, tree)
        assertTrue("Should generate at least one candidate", candidates.isNotEmpty())

        // Test GKD standard splash skip pattern candidate
        val gkdPatternCandidate = candidates.find { it.selector.contains("[text*=\"跳过\"][text.length<=10]") }
        assertNotNull("Should generate GKD standard splash skip candidate", gkdPatternCandidate)
        assertEquals("GKD skip candidate should match the skip node", 1, gkdPatternCandidate?.matchCount)

        val textCandidate = candidates.find { it.selector.contains("text=\"跳过 5s\"") }
        assertNotNull("Should generate text candidate", textCandidate)
        assertEquals("Text candidate should match exactly 1 node", 1, textCandidate?.matchCount)

        val vidCandidate = candidates.find { it.selector.contains("vid=\"btn_skip\"") }
        assertNotNull("Should generate vid candidate", vidCandidate)
        assertEquals("Vid candidate should match exactly 1 node", 1, vidCandidate?.matchCount)
    }

    @Test
    fun testSelectorTesterMatchesCorrectNodes() {
        val tree = createSampleTree()

        // Test 1: Match by text
        val result1 = SelectorTester.test("[text*='跳过']", tree)
        assertTrue(result1 is SelectorTestResult.Success)
        val matches1 = (result1 as SelectorTestResult.Success).matchingNodes
        assertEquals(1, matches1.size)
        assertEquals(2, matches1[0].id)

        // Test 2: Match by vid and clickable
        val result2 = SelectorTester.test("[vid = 'btn_skip'][clickable = true]", tree)
        assertTrue(result2 is SelectorTestResult.Success)
        val matches2 = (result2 as SelectorTestResult.Success).matchingNodes
        assertEquals(1, matches2.size)
        assertEquals(2, matches2[0].id)

        // Test 3: Match multiple TextViews
        val result3 = SelectorTester.test("TextView", tree)
        assertTrue(result3 is SelectorTestResult.Success)
        val matches3 = (result3 as SelectorTestResult.Success).matchingNodes
        assertEquals(2, matches3.size) // skipBtn and titleTv

        // Test 4: Match with child combinator
        val result4 = SelectorTester.test("[vid = 'container'] > [text *= '跳过']", tree)
        assertTrue(result4 is SelectorTestResult.Success)
        val matches4 = (result4 as SelectorTestResult.Success).matchingNodes
        assertEquals(1, matches4.size)
        assertEquals(2, matches4[0].id)

        // Test 5: Invalid selector syntax handling
        val resultInvalid = SelectorTester.test("[invalid syntax ===]", tree)
        assertTrue(resultInvalid is SelectorTestResult.Error)
        val errMsg = (resultInvalid as SelectorTestResult.Error).message
        assertNotNull(errMsg)
    }

    @Test
    fun testActivityValidation() {
        // Android Views and Layouts must be rejected
        assertFalse(RuleGenerator.isPotentialActivityName("android.widget.FrameLayout"))
        assertFalse(RuleGenerator.isPotentialActivityName("android.widget.LinearLayout"))
        assertFalse(RuleGenerator.isPotentialActivityName("android.widget.PopupWindow"))
        assertFalse(RuleGenerator.isPotentialActivityName("android.view.View"))
        assertFalse(RuleGenerator.isPotentialActivityName("com.android.systemui.statusbar.phone.NotificationShadeWindowView"))
        assertFalse(RuleGenerator.isPotentialActivityName("android.widget.PopupWindow\$PopupDecorView"))
        assertFalse(RuleGenerator.isPotentialActivityName("null"))
        assertFalse(RuleGenerator.isPotentialActivityName(""))

        // Real Activities should be accepted
        assertTrue(RuleGenerator.isPotentialActivityName("com.taobao.taobao.MainActivity"))
        assertTrue(RuleGenerator.isPotentialActivityName("com.tencent.mm.ui.LauncherUI"))
        assertTrue(RuleGenerator.isPotentialActivityName(".MainActivity"))

        // formatActivityId test
        assertEquals(".MainActivity", RuleGenerator.formatActivityId("com.taobao.taobao", "com.taobao.taobao.MainActivity"))
        assertEquals(".ui.LauncherUI", RuleGenerator.formatActivityId("com.tencent.mm", "com.tencent.mm.ui.LauncherUI"))
        assertNull(RuleGenerator.formatActivityId("com.taobao.taobao", "android.widget.FrameLayout"))
    }

    @Test
    fun testRuleGeneratorSplashAdMatchesGkdSubscriptionStandard() {
        val options = RuleOptions(
            appId = "com.taobao.taobao",
            appName = "淘宝",
            activityId = "com.taobao.taobao.MainActivity",
            selector = "[text*=\"跳过\"][text.length<=10]",
            category = RuleCategory.SPLASH,
            groupKey = 0,
            groupName = "开屏广告",
            includeActivity = false,
            outputScope = OutputScope.APP
        )

        val json5 = RuleGenerator.generateJson5(options)
        assertTrue(json5.contains("id: 'com.taobao.taobao'"))
        assertTrue(json5.contains("name: '淘宝'"))
        assertTrue(json5.contains("key: 0"))
        assertTrue(json5.contains("name: '开屏广告'"))
        assertTrue(json5.contains("matchTime: 10000"))
        assertTrue(json5.contains("actionMaximum: 1"))
        assertTrue(json5.contains("resetMatch: 'app'"))
        assertTrue(json5.contains("fastQuery: true"))
        assertTrue(json5.contains("matches: '[text*=\"跳过\"][text.length<=10]'"))
        // Splash ad should not lock activityId
        assertFalse(json5.contains("activityIds"))
    }

    @Test
    fun testRuleGeneratorOutputScopes() {
        val options = RuleOptions(
            appId = "com.quark.browser",
            appName = "夸克",
            activityId = "com.quark.browser.BrowserActivity",
            selector = "[vid=\"close\"]",
            category = RuleCategory.UPDATE,
            groupKey = 2,
            groupName = "更新提示",
            includeActivity = true,
            outputScope = OutputScope.RULE
        )

        // Rule Scope
        val ruleJson = RuleGenerator.generateJson5(options.copy(outputScope = OutputScope.RULE))
        assertTrue(ruleJson.contains("matchTime: 10000"))
        assertTrue(ruleJson.contains("actionMaximum: 1"))
        assertTrue(ruleJson.contains("resetMatch: 'app'"))
        assertTrue(ruleJson.contains("activityIds: '.BrowserActivity'"))
        assertTrue(ruleJson.contains("matches: '[vid=\"close\"]'"))

        // Group Scope
        val groupJson = RuleGenerator.generateJson5(options.copy(outputScope = OutputScope.GROUP))
        assertTrue(groupJson.contains("key: 2"))
        assertTrue(groupJson.contains("name: '更新提示'"))
        assertTrue(groupJson.contains("rules: ["))

        // Subscription Scope
        val subJson = RuleGenerator.generateJson5(options.copy(outputScope = OutputScope.SUBSCRIPTION))
        assertTrue(subJson.contains("id: 10001"))
        assertTrue(subJson.contains("name: 'GKD 本地规则订阅'"))
        assertTrue(subJson.contains("apps: ["))
    }
}
