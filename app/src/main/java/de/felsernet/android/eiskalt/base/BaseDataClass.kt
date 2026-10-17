package de.felsernet.android.eiskalt.base

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import de.felsernet.android.eiskalt.log.AppLog
import java.io.Serializable

interface BaseDataClass {
    var id: String
    var name: String
    var icon: IconInfo?
    var comment: String

    // We need "equals", because this interface is used in DiffUtil class,
    // so let the linter know we have the function.
    // As long as the implementing classes are data classes, we do not need actually need to
    // implement the function ourselves, the kotlin default implementation for data classes
    // will be sufficient ;-)
    override fun equals(other: Any?): Boolean
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
                AppLog.w("icon", "Unknown icon type \"$value\" in DB")
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