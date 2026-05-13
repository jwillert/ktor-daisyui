package dev.jwillert.kopetal

class RegistryKey<T : Any>(val id: String)

object KopetalRegistry {
    private val slots = mutableMapOf<RegistryKey<*>, Any>()

    @Suppress("UNCHECKED_CAST")
    operator fun <T : Any> get(key: RegistryKey<T>): T? = slots[key] as? T

    operator fun <T : Any> set(key: RegistryKey<T>, value: T) {
        slots[key] = value
    }

    fun <T : Any> remove(key: RegistryKey<T>) {
        slots.remove(key)
    }
}
