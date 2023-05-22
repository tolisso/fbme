package org.fbme.lib.iec61131.converter

import org.fbme.lib.iec61131.model.OldStandardXml

class Iec61131ConverterException(message: String, cause: Throwable?) : RuntimeException(message, cause) {

    constructor(xmlPou: OldStandardXml.Pou, message: String, cause: Throwable?) : this(
        "Exception while processing pou [${xmlPou.name}]: $message", cause
    )
}