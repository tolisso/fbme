package org.fbme.lib.iec61131.service

import org.fbme.lib.iec61131.converter.ConverterArguments
import org.fbme.lib.iec61131.converter.ConverterBase
import org.fbme.lib.iec61131.model.OldStandardXml

class InterfaceService(
    private val xmlInterface: OldStandardXml.Interface,
    converterArguments: ConverterArguments
) : ConverterBase(converterArguments) {

    fun getInputVariables(): Set<String> {
        return getVarNames(xmlInterface.inputVars)
    }

    fun getInOutVariables(): Set<String> {
        return getVarNames(xmlInterface.inOutVars)
    }

    fun getOutputVariables(): Set<String> {
        return getVarNames(xmlInterface.outputVars)
    }

    private fun getVarNames(varListList: List<OldStandardXml.VariableList>): Set<String> {
        return HashSet(varListList.map { varList -> varList.variableList.map { it.name } }.flatten())
    }
}