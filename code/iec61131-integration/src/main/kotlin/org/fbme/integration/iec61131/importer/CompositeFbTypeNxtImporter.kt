package org.fbme.integration.iec61131.importer

import org.fbme.integration.iec61131.importer.FBNetworkNxtImporter
import org.fbme.lib.iec61499.declarations.CompositeFBTypeDeclaration
import org.fbme.lib.iec61499.parser.CompositeFBTypeConverter
import org.fbme.lib.iec61499.parser.ConverterArguments
import org.fbme.lib.iec61499.parser.FBNetworkConverter

class CompositeFbTypeNxtImporter(arguments: ConverterArguments) : CompositeFBTypeConverter(arguments) {
    override fun createFBNetworkConverter(fbtd: CompositeFBTypeDeclaration): FBNetworkConverter {
        val element = checkNotNull(element)
        return FBNetworkNxtImporter(with(element.getChild("FBNetwork")), fbtd.network)
    }
}