package org.fbme.lib.iec61131.converter

import org.fbme.lib.iec61131.model.OldStandardXml

class FbdInfo(
    val name: String,
    val xmlFbd: OldStandardXml.FBD,
    val xmlInterface: OldStandardXml.Interface,
)