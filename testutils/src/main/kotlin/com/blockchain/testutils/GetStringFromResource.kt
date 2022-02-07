package com.blockchain.testutils

fun Any.getStringFromResource(filePath: String): String =
    object {}.javaClass.getResource("/$filePath").readText()

fun String.asResource(work: (String) -> Unit) {
    work(getStringFromResource(this))
}
