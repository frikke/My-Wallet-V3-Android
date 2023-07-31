package com.blockchain.instrumentation

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.RuleSet
import com.pinterest.ktlint.core.RuleSetProvider
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class NoInstrumentationImportRule : Rule("no-instrumentation-import") {
    private val thisPackageName by lazy {
        this::class.java.`package`.name
    }

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType == KtStubElementTypes.IMPORT_DIRECTIVE) {
            val importDirective = node.psi as KtImportDirective
            val path = importDirective.importPath?.pathStr
            if (path != null && path.contains(thisPackageName)) {
                emit(node.startOffset, "Instrumentation code found, remove before merging code", false)
            }
        }
    }
}

class InstrumentationCustomRuleSetProvider : RuleSetProvider {
    override fun get() = RuleSet("instrumentation-ruleset", NoInstrumentationImportRule())
}