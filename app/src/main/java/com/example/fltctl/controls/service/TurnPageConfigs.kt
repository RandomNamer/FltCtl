package com.example.fltctl.controls.service

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.Keep
import androidx.core.os.bundleOf
import com.example.fltctl.configs.PackageNames
import com.example.fltctl.widgets.view.takeDpAsFloat

private const val TAG = "TurnPageConfig"
val CLICK_PADDING = 50.takeDpAsFloat()
private const val FORCE_USE_DEFAULT_METHOD = true

sealed interface TurnPageMethod {
    fun turnNext(node: AccessibilityNodeInfo): Boolean
    fun turnPrev(node: AccessibilityNodeInfo): Boolean
    fun match(node: AccessibilityNodeInfo): Boolean
}

@Keep
object ViewPagerMethod : TurnPageMethod {
    private val nextAction = AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD
    private val prevAction = AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD
    override fun turnNext(node: AccessibilityNodeInfo): Boolean {
        return node.performAction(nextAction.id)
    }

    override fun turnPrev(node: AccessibilityNodeInfo): Boolean {
        return node.performAction(prevAction.id)
    }

    override fun match(node: AccessibilityNodeInfo): Boolean {
        return node.actionList.contains(nextAction) && node.className.contains("ViewPager")
    }

}
@Keep
object MoveTextMethod: TurnPageMethod {
    private val nextAction = AccessibilityNodeInfo.AccessibilityAction.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
    private val prevAction = AccessibilityNodeInfo.AccessibilityAction.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
    override fun turnNext(node: AccessibilityNodeInfo): Boolean {
        return node.performAction(AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, bundleOf(
            AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT to AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE,
            AccessibilityNodeInfo.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN to true
        ))
    }

    override fun turnPrev(node: AccessibilityNodeInfo): Boolean {
        return node.performAction(AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, bundleOf(
            AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT to AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE,
            AccessibilityNodeInfo.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN to true
        ))
    }

    override fun match(node: AccessibilityNodeInfo): Boolean {
        Log.i(TAG, "inspecting node ${node.className} with actions ${node.actionList}")
        return node.actionList.contains(nextAction) && node.actionList.contains(prevAction)
    }
}

data class TurnPageResult(
    val nextAction: (node: AccessibilityNodeInfo) -> Boolean,
    val prevAction: (node: AccessibilityNodeInfo) -> Boolean,
    val node: AccessibilityNodeInfo
)

fun findPageBy(root: AccessibilityNodeInfo?): TurnPageResult? {
    root ?: return null
    val result = getMethodPriority(root.packageName.toString()).map { method ->
        findNodesBy(root, method::match).map {
            TurnPageResult(method::turnNext, method::turnPrev, it)
        }
    }.flatten()
    Log.i(TAG, "find matches: $result")
    return result.firstOrNull()
}

private fun getMethodPriority(packageName: String): List<TurnPageMethod> {
    if (FORCE_USE_DEFAULT_METHOD) return emptyList()
    return when(packageName) {
        PackageNames.KINDLE -> listOf(ViewPagerMethod)
        PackageNames.DMZJ, PackageNames.DMZJ_ALT -> listOf()
        else -> listOf(ViewPagerMethod)
    }
}

private fun findNodesBy(root: AccessibilityNodeInfo, criteria: (node: AccessibilityNodeInfo) -> Boolean): List<AccessibilityNodeInfo> {
    val deque = ArrayDeque<AccessibilityNodeInfo>()
    deque.add(root)
    var result = mutableListOf<AccessibilityNodeInfo>()
    while (!deque.isEmpty()) {
        val node = deque.removeFirst()
        if (criteria.invoke(node)) {
            result.add(node)
        }
        for (i in 0 until node.childCount) {
            deque.addLast(node.getChild(i))
        }
    }
    return result
}