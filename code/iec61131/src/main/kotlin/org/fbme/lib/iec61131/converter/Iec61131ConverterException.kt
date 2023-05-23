package org.fbme.lib.iec61131.converter

import org.fbme.lib.iec61131.model.OldStandardXml

class Iec61131ConverterException(message: String, cause: Throwable?) : RuntimeException(message, cause) {

    constructor(pouName: String, message: String, cause: Throwable?) : this(
        "Exception while processing pou [$pouName]: $message", cause
    )

    constructor(xmlPou: OldStandardXml.Pou, message: String, cause: Throwable?) : this(xmlPou.name, message, cause)

    constructor(pouName: String, message: String) : this(pouName, message, null)

    constructor(message: String): this(message, null)
}