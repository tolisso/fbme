package org.fbme.lib.iec61131.converter

import org.fbme.lib.common.Declaration
import org.fbme.lib.iec61131.model.OldStandardXml
import org.fbme.lib.iec61499.IEC61499Factory
import org.fbme.lib.iec61499.declarations.CompositeFBTypeDeclaration
import org.fbme.lib.iec61499.parser.STConverter
import org.fbme.lib.st.STFactory
import org.fbme.lib.st.expressions.Literal

class ProjectConverter(
    private val factory: IEC61499Factory,
    private val stFactory: STFactory
) {
    fun getProjectNodes(path: String): List<Declaration> {
        val project = OldStandardXml.serializeProject(path)
        val blockInterfaceService = BlocksInterfaceInfo(project.types.pous)
        val converterArguments = ConverterArguments(factory, stFactory, blockInterfaceService)
        val resDeclarations = getChildNodes(converterArguments, project)

        return resDeclarations + SystemConverter(project, converterArguments).createSystems()
    }

    private fun getChildNodes(
        converterArguments: ConverterArguments,
        project: OldStandardXml.Project
    ): List<Declaration> {
        return project.types.pous.pouList.map { convertToCompositeFBType(it, converterArguments) }
    }

    private fun convertToCompositeFBType(
        xmlPou: OldStandardXml.Pou,
        converterArguments: ConverterArguments
    ): CompositeFBTypeDeclaration {
        val fbdInfo = getFbdInfo(xmlPou)
        val compositeFbtd = converterArguments.factory.createCompositeFBTypeDeclaration(null)
        FbNetworkConverter(fbdInfo, converterArguments, "REQ", "CNF")
            .fillNetwork(compositeFbtd.network)
        FbInterfaceConverter(xmlPou, converterArguments).fillInterface(compositeFbtd)
        return compositeFbtd
    }
}