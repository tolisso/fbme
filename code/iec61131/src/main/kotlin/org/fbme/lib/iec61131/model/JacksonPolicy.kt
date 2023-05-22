package org.fbme.lib.iec61131.model

import nl.adaptivity.xmlutil.QName
import nl.adaptivity.xmlutil.allText
import nl.adaptivity.xmlutil.serialization.*
import nl.adaptivity.xmlutil.serialization.structure.SafeParentInfo


object JacksonPolicy : DefaultXmlSerializationPolicy(
    false,
    encodeDefault = XmlSerializationPolicy.XmlEncodeDefault.NEVER,
    unknownChildHandler = UnknownChildHandler { input, inputKind, _, _, _ ->
        emptyList()
    }
) {

    override fun effectiveOutputKind(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        canBeAttribute: Boolean
    ): OutputKind {
        if (tagParent.elementUseAnnotations.filterIsInstance<XmlValue>().isNotEmpty()) {
            return OutputKind.Text
        }
        return super.effectiveOutputKind(serializerParent, tagParent, canBeAttribute)
    }

    // naming policy
    override fun effectiveName(
        serializerParent: SafeParentInfo,
        tagParent: SafeParentInfo,
        outputKind: OutputKind,
        useName: XmlSerializationPolicy.DeclaredNameInfo
    ): QName {
        val qnameWithoutNamespace = useName.annotatedName // name via annotation
            ?: serialUseNameToQName(useName, tagParent.namespace) // variable name in father

        // both variables "body" and "bodyList" parsing into "<body/>"
        val name = qnameWithoutNamespace.localPart.removeSuffix("List")

        return QName("http://www.plcopen.org/xml/tc6_0201", name, "default-prefix")
    }


}
