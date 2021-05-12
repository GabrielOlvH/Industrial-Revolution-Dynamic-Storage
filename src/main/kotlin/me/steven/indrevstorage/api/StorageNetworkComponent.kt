package me.steven.indrevstorage.api

import kotlin.reflect.KClass
import kotlin.reflect.safeCast

interface StorageNetworkComponent {
    var network: IRDSNetwork?

    fun <T : Any> convert(clazz: KClass<T>): T? = clazz.safeCast(this)
}