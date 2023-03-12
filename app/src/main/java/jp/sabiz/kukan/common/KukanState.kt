package jp.sabiz.kukan.common

enum class KukanState {
    OFF,
    ON;

    fun toOn(): KukanState {
        return ON
    }

    fun toOff(): KukanState {
        return OFF
    }

    fun toggle(): KukanState {
        return when(this){
            ON -> OFF
            OFF -> ON
        }
    }
}