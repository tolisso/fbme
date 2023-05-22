package org.fbme.lib.iec61131.converter

import org.fbme.lib.common.Declaration
import org.fbme.lib.iec61131.model.OldStandardXml
import org.fbme.lib.iec61499.IEC61499Factory
import org.fbme.lib.iec61499.declarations.CompositeFBTypeDeclaration
import org.fbme.lib.st.STFactory

class ProjectConverter(
    private val factory: IEC61499Factory,
    private val stFactory: STFactory
) {
    fun getProjectNodes(path: String): List<Declaration> {
        val project = OldStandardXml.serializeProject(path)
        val blockInterfaceService = BlocksInterfaceInfo(project.types.pous)
        val converterArguments = ConverterArguments(factory, stFactory, blockInterfaceService)
        val resDeclarations = getChildNodes(converterArguments, project)

        return resDeclarations + SystemConverter(project, converterArguments).createSystem()
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
        val firstBody = xmlPou.bodyList.getOrNull(0)
            ?: throw Iec61131ConverterException(xmlPou, "pou has no body", null)
        val fbd = firstBody.fbd
            ?: throw Iec61131ConverterException(xmlPou, "non fbd bodies are not supported yet", null)
        val pouInterface = xmlPou.pouInterface
            ?: throw Iec61131ConverterException(xmlPou, "pou has no interface", null)

        val compositeFbtd = converterArguments.factory.createCompositeFBTypeDeclaration(null)
        FbNetworkConverter(fbd, pouInterface, converterArguments, "REQ", "CNF")
            .fillNetwork(compositeFbtd.network)
        FbtdInterfaceConverter(xmlPou, converterArguments).fillInterface(compositeFbtd)
        return compositeFbtd
    }
}