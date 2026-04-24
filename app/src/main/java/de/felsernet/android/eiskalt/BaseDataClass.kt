package de.felsernet.android.eiskalt

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import java.io.Serializable

interface BaseDataClass {
    var id: String
    var name: String
    var icon: IconInfo?
    var comment: String
}

data class IconInfo(
    @get:Exclude @set:Exclude var type: IconType = IconType.UNKNOWN,
    var path: String = "",
) : Serializable {
    @get:PropertyName("type")
    @set:PropertyName("type")
    var typeString: String
        get() = type.name
        set(value) {
            type = try {
                IconType.valueOf(value)
            } catch (e: Exception) {
                IconType.UNKNOWN
            }
        }

    constructor() : this(IconType.UNKNOWN, "")
}

enum class IconType {
    ASSET,
    R_DRAWABLE,
    UNKNOWN,
}